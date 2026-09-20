"""
Tree sitter config and the AST normalized token stream traversal
used by the plagiarism strutural signal.

Every langauge block below is intentionally small and explicit rather than a gigantic rule,
because tree-sitter grammars are NOT consistent with each other on field names.
"""

from dataclasses import dataclass, field
from typing import Optional

from tree_sitter import Node

@dataclass(frozen=True)
class LangConfig:
    ts_name: str
    function_decl_types: set[str]
    class_decl_types: set[str]
    call_types: set[str]
    call_function_field: str = "function"
    decl_name_field: str = "name"

#Only langs with with a working bundled tree-sitter grammer belong here.
#Anything else falls back to the regex lexer.

LANGUAGE_CONFIG: dict[str, LangConfig] = {
    "java": LangConfig(
        ts_name="java",
        function_decl_types={"method_declaration", "constructor_declaration"},
        class_decl_types={"class_declaration", "interface_declaration", "enum_declaration", "record_declaration"},
        call_types={"method_invocation", "object_creation_expression"},
        call_function_field="name",
    ),
    "py": LangConfig(
        ts_name="python",
        function_decl_types={"function_definition"},
        class_decl_types={"class_definition"},
        call_types={"call"},
        call_function_field="function",
    ),
    "c": LangConfig(
        ts_name="c",
        function_decl_types={"function_definition"},
        class_decl_types={"struct_specifier"},
        call_types={"call_expression"},
        call_function_field="function",
    ),
    "h": LangConfig(
        ts_name="c",
        function_decl_types={"function_definition"},
        class_decl_types={"struct_specifier"},
        call_types={"call_expression"},
        call_function_field="function",
    ),
    "cpp": LangConfig(
        ts_name="cpp",
        function_decl_types={"function_definition"},
        class_decl_types={"class_specifier", "struct_specifier"},
        call_types={"call_expression"},
        call_function_field="function",
    ),
    "hpp": LangConfig(
        ts_name="cpp",
        function_decl_types={"function_definition"},
        class_decl_types={"class_specifier", "struct_specifier"},
        call_types={"call_expression"},
        call_function_field="function",
    ),
    "cs": LangConfig(
        ts_name="c_sharp",
        function_decl_types={"method_declaration", "constructor_declaration", "local_function_statement"},
        class_decl_types={"class_declaration", "struct_declaration", "interface_declaration"},
        call_types={"invocation_expression", "object_creation_expression"},
        call_function_field="function",
    ),
    "js": LangConfig(
        ts_name="javascript",
        function_decl_types={"function_declaration", "method_definition", "function"},
        class_decl_types={"class_declaration"},
        call_types={"call_expression"},
        call_function_field="function",
    ),
    "jsx": LangConfig(
        ts_name="javascript",
        function_decl_types={"function_declaration", "method_definition", "function"},
        class_decl_types={"class_declaration"},
        call_types={"call_expression"},
        call_function_field="function",
    ),
    "ts": LangConfig(
        ts_name="typescript",
        function_decl_types={"function_declaration", "method_definition", "function"},
        class_decl_types={"class_declaration", "interface_declaration"},
        call_types={"call_expression"},
        call_function_field="function",
    ),
    "tsx": LangConfig(
        ts_name="tsx",
        function_decl_types={"function_declaration", "method_definition", "function"},
        class_decl_types={"class_declaration", "interface_declaration"},
        call_types={"call_expression"},
        call_function_field="function",
    ),
    "go": LangConfig(
        ts_name="go",
        function_decl_types={"function_declaration", "method_declaration"},
        class_decl_types={"type_declaration"},
        call_types={"call_expression"},
        call_function_field="function",
    ),
    "rs": LangConfig(
        ts_name="rust",
        function_decl_types={"function_item"},
        class_decl_types={"struct_item", "impl_item", "enum_item"},
        call_types={"call_expression"},
        call_function_field="function",
    ),
    "kt": LangConfig(
        ts_name="kotlin",
        function_decl_types={"function_declaration"},
        class_decl_types={"class_declaration"},
        call_types={"call_expression"},
        call_function_field="function",
    ),
    


}
