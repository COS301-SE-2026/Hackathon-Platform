from typing import Optional

from fastapi import FastAPI
from pydantic import BaseModel
from tree_sitter_languages import get_parser

from normalize import LANGUAGE_CONFIG, normalize_tree

app = FastAPI(title="ast-normalization-service")

_parser_cache: dict[str, object] = {}

def _extension_of(file_name: str) -> str:
    if "." not in file_name:
        return ""
    return file_name.rsplit(".", 1)[-1].lower()

def _get_parser(ts_name: str):
    if ts_name not in _parser_cache:
        _parser_cache[ts_name] = get_parser(ts_name)
    return _parser_cache[ts_name]

class ParseRequest(BaseModel):
    file_name: str
    content: str

class TokenResponse(BaseModel):
    text: str
    start: int
    end: int

class FunctionSpanResponse(BaseModel):
    qualified_name: str
    node_type: str
    start_byte: int
    end_byte: int
    start_line: int
    end_line: int

class ParseResponse(BaseModel):
    status: str # "ok" , "unsupported_language" , "parse_error"
    language: Optional[str] = None
    tokens: list[TokenResponse] = []
    functions: list[FunctionSpanResponse] = []
    detail: Optional[str] = None

@app.get("/health")
def health() -> dict:
    return {"status": "ok", "supported_extensions": sorted(LANGUAGE_CONFIG.keys())}

@app.post("/parse", response_model=ParseResponse)
def parse(req: ParseRequest) -> ParseResponse:
    ext = _extension_of(req.file_name)
    lang_config = LANGUAGE_CONFIG.get(ext)

    if lang_config is None:
        return ParseResponse(
            status="unsupported_language",
            language=ext or None,
            detail=f"no tree-sitter grammar configured for extension '.{ext}'",
        )

    if not req.content.strip():
        return ParseResponse(
            status="parse_error",
            language=lang_config.ts_name,
            detail="empty file content",
        )

    try:
        parser = _get_parser(lang_config.ts_name)
        source_bytes = req.content.encode("utf-8", errors="replace")
        tree = parser.parse(source_bytes)
    except Exception as e: # grammar load / parser crash
        return ParseResponse(
            status="parse_error",
            language=lang_config.ts_name,
            detail=f"parser raised: {e}",
        )

    if tree.root_node.has_error and _error_ratio(tree.root_node) > 0.15:

        return ParseResponse(
            status="parse_error",
            language=lang_config.ts_name,
            detail="parse tree exceeds error-node threshold",
        )

    tokens, functions = normalize_tree(tree.root_node, lang_config)

    return ParseResponse(
        status="ok",
        language=lang_config.ts_name,
        tokens=[TokenResponse(text=t.text, start=t.start, end=t.end) for t in tokens],
        functions=[
            FunctionSpanResponse(
                qualified_name=f.qualified_name,
                node_type=f.node_type,
                start_byte=f.start_byte,
                end_byte=f.end_byte,
                start_line=f.start_line,
                end_line=f.end_line,
            )
            for f in functions
        ],
    )

def _error_ratio(root) -> float:
    total = 0
    errors = 0

    def walk(node):
        nonlocal total, errors
        total += 1
        if node.type == "ERROR":
            errors += 1
        for child in node.children:
            walk(child)

    walk(root)
    return errors/ total if total else 0.0

#Embedding semantic similarity stuff

class SpanRequest(BaseModel):
    qualified_name: str
    start_byte: int
    end_byte: int

class EmbedRequest(BaseModel):
    file_name: str
    content: str
    spans: list[SpanRequest]

class EmbeddingResponse(BaseModel):
    qualified_name: str
    vector: list[float]
    truncated: bool

class EmbedResponse(BaseModel):
    status: str # "ok" , "error"
    model: Optional[str] = None
    dimension: Optional[int] = None
    embeddings: list[EmbeddingResponse] = []
    detail: Optional[str] = None

@app.post("/embed" , response_model=EmbedResponse)
def embed(req: EmbedRequest) -> EmbedResponse:

    try:
        import embeddings
    except Exception as e:
        return EmbedResponse(status="error", detail=f"embedding backend unavailable: {e}")

    if not req.spans:
        return EmbedResponse(status="ok", model=embeddings.MODEL_NAME, embeddings=[])

    try:
        results = embeddings.embed_functions(
            req.content, [s.model_dump() for s in req.spans]
        )

        dimension = embeddings.embedding_dimension()
    except Exception as e:
        return EmbedResponse(status="error", detail=f"embedding failed: {e}")

    return EmbedResponse(
        status="ok",
        model=embeddings.MODEL_NAME,
        dimension=dimension,
        embeddings=[
            EmbeddingResponse(qualified_name=r.qualified_name, vector=r.vector, truncated=r.truncated)
            for r in results

        ],
    )
