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
    t = node_type.lower()
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
    start_byte: int
    end_byte: int
    start_line: int
    end_line: int

@dataclass
class Token:
    """One normalized token plus its original source byte offsets."""

    text: str
    start: int
    end: int

def normalize_tree(root: Node, lang: LangConfig) -> tuple[list[Token], list[FunctionSpan]]:
    """Pre-order traversal producing:
    -a flattened normalized token stream
    -the list of function/method spans found (byte + line
    offsets into the original source, for COdeBERT-stage chunking,
    which are not normalized)"""

    tokens: list[Token] = []
    functions: list[FunctionSpan] = []
    class_name_stack: list[str] = []

    def visit(node: Node) -> None:
        if _is_comment(node.type):
            return

        if node.type in lang.class_decl_types:
            class_name_stack.append(_decl_name(node, lang) or "?")
            _emit_children_with_wrapper(node)
            class_name_stack.pop()
            return
        
        if node.type in lang.function_decl_types:
            name = _decl_name(node, lang) or "?"
            qualified = ".".join(class_name_stack + [name]) if class_name_stack else name
            functions.append(
                FunctionSpan(
                    qualified_name=qualified,
                    node_type=node.type,
                    start_byte=node.start_byte,
                    end_byte=node.end_byte,
                    start_line=node.start_point[0] + 1,
                    end_line=node.end_point[0] + 1,

                )
            )
            _emit_children_with_wrapper(node)
            return
        
        if node.child_count == 0:
            _emit_leaf(node)
            return
        
        _emit_children_with_wrapper(node)

    def _emit_children_with_wrapper(node: Node) -> None:
        tokens.append(Token(f"({node.type}", node.start_byte, node.start_byte))
        for child in node.children:
            visit(child)
        tokens.append(Token(")", node.end_byte, node.end_byte))

    def _emit_leaf(node: Node) -> None:
        start, end = node.start_byte, node.end_byte
        if node.type in IDENTIFIER_NODE_TYPES:
            tokens.append(Token(classify_identifier(node, lang), start, end))
            return
        literal = _literal_placeholder(node.type)
        if literal is not None:
            tokens.append(Token(literal, start, end))
            return
        
        tokens.append(Token(node.type, start, end))

    def _decl_name(node: Node, lang: LangConfig) -> Optional[str]:
        name_node = node.child_by_field_name(lang.decl_name_field)
        if name_node is None:
            return None
        return name_node.text.decode("utf-8", errors="replace")
    
    visit(root)
    return tokens, functions



