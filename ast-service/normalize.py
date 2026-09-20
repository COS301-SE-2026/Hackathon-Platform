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

# Node types whose text should collapse to a generic identifier placeholder
# rather than being kept literally

IDENTIFIER_NODE_TYPES = {
    "identifier",
    "type_identifier",
    "field_identifier",
    "property_identifier",
    "shorthand_property_identifier",
    "shorthand_property_identifier_pattern",
}

def _is_comment(node_type: str) -> bool:
    return "comment" in node_type

def _literal_placeholder(node_type: str) -> Optional[str]:
    """Best effort mapping of a leaf literal node type to a placeholder.
    Subtring match rather than exhaustive per-grammar table."""
    t = node.type.lower()
    if "string" in t or "char_literal" in t or t == "char":
        return "STR"
    if "true" == t or "false" == t or "boolean" in t:
        return "BOOL"
    if "null" in t or t == "none":
        return "NULL"
    if any(k in t for k in ("integer", "float", "number", "decimal", "hex", "octal", "int_literal")):
        return "NUM"
    return None

def classify_identifier(node: Node, lang: LangConfig) -> str:
    """FID (funtion/method name), TID (type/class name), or VID (everything
    else: locals, params, fields, call arguments...)."""
    if node.type == "type_identifier":
        return "TID"
    
    parent = node.parent
    if parent is None:
        return "VID"

    field_name = parent.field_name_for_child(_child_index(parent, node))

    if parent.type in lang.class_decl_types and field_name == "name":
        return "TID"
    
    if parent.type in lang.function_decl_types and field_name == "name":
        return "FID"

    if parent.type in lang.call_types and field_name == lang.call_function_field:
        return "FID"

    # a calls callee is sometimes wrapped one level down
    # walk up one more level for that common shape.
    grandparent = parent.parent
    if (
        grandparent is not None
        and grandparent.type in lang.call_types
        and field_name in ("attribute", "property", "field")
    ):

        return "FID"
    
    return "VID"

def _child_index(parent: Node, child: Node) -> int:
    for i in range(parent.child_count):
        if parent.child(i).id == child.id:
            return i
    return -1

@dataclass
class FunctionSpan:
    qualified_name: str
    node_type: str
    start_type: str
    end_byte: int
    start_line: int
    end_line: int

@dataclass
class Token:
    """One normalized token plus its original source byte offsets."""

    text: str
    start: int
    end: int

