package com.hackathon.platform.plagiarism.ast;

import java.util.List;

/**
 * Deserialized response from the AST parsing microservice's /parse.
 */
public record AstParseResponse(
    String status, String language, List<AstToken> tokens, List<AstFunctionSpan> functions, String detail
) {
    public boolean isOk() {
        return "ok".equals(status);
        
    }
}