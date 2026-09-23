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

    