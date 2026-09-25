"""

Function-level semantic embeddings using a pretrained code model
(microsoft/codebert-base).

This model embeds lightly cleaned source text (comments stripped), per function,
using real source spans produced by normalize.normalize_tree.

"""

import re
import threading
from dataclasses import dataclass

import torch
from transformers import AutoModel, AutoTokenizer

MODEL_NAME = "microsoft/codebert-base"
MAX_TOKENS = 512

_lock = threading.Lock()
_tokenizer = None
_model = None

def _ensure_loaded() -> None:
    global _tokenizer, _model
    if _model is not None:
        return
    with _lock:
        if _model is not None:
            return
        _tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
        _model = AutoModel.from_pretrained(MODEL_NAME)
        _model.eval()

@dataclass
class EmbeddingResult:
    qualified_name: str
    vector: list[float]
    truncated: bool

#this is a best-effort cleanup pass before embedding, not a second normalization stage.
_BLOCK_COMMENT_RE = re.compile(r"/\*.*?\*/", re.DOTALL)
_LINE_COMMENT_RE = re.compile(r"(//|#)[^\n]*")

def _strip_comments(source: str) -> str:
    source = _BLOCK_COMMENT_RE.sub(" ", source)
    source = _LINE_COMMENT_RE.sub("", source)
    return source

def embed_functions(source: str, spans: list[dict]) -> list[EmbeddingResult]:
    """spans: [{"qualified_name": str, "start_byte": int, "end_byte": int}]"""

    _ensure_loaded()

    source_bytes = source.encode("utf-8", errors="replace")
    results: list[EmbeddingResult] = []

    for span in spans:
        raw = source_bytes[span["start_byte"] : span["end_byte"]].decode("utf-8", errors="replace")
        cleaned = _strip_comments(raw).strip()
        if not cleaned:
            continue
        
        encoded =  _tokenizer(
            cleaned,
            return_tensors="pt",
            truncation=True,
            max_length=MAX_TOKENS,
            padding=False,
        )
        truncated = encoded["input_ids"].shape[1] >= MAX_TOKENS

        with torch.no_grad():
            output = _model(**encoded)

        # Mean-pool token embeddings
        mask = encoded["attention_mask"].unsqueeze(-1).float()
        summed = (output.last_hidden_state * mask).sum(dim=1)
        counts = mask.sum(dim=1).clamp(min=1e-9)
        pooled = (summed / counts).squeeze(0)

        results.append(
            EmbeddingResult(
                qualified_name=span["qualified_name"],
                vector=pooled.tolist(),
                truncated=truncated,

            )
        )

    return results

def embedding_dimension() -> int:
    _ensure_loaded()
    return _model.config.hidden_size