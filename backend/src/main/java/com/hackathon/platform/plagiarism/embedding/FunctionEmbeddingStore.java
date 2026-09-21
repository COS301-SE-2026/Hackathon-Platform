package com.hackathon.platform.plagiarism.embedding;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Stores per-function CodeBERT embeddings in the submission_function_embedding pgvector
 * table.
 */
@Repository
@RequiredArgsConstructor
public class FunctionEmbeddingStore {

    private final JdbcTemplate jdbc;

    public record Row(String fileName, String qualifiedName, float[] vector, boolean truncated) {}

    public record StoredEmbedding(Long submissionId, String qualifiedName, float[] vector) {}

    /** Replaces all stored embeddings for a submission with the given set. */
    public void replaceEmbeddings(Long submissionId, List<Row> rows) {
        jdbc.update("DELETE FROM submission_function_embedding WHERE submission_id = ?", submissionId);

        if(rows.isEmpty()) {
            return;
        }

        jdbc.batchUpdate(
            "INSERT INTO submission_function_embedding "
                + "(submission_id, file_name, qualified_name, embedding, truncated) "
                + "VALUES (?, ?, ?, ?::vector, ?)",
            rows,
            rows,size(),
            (ps, row) -> {
                ps.setLong(1, submissionId);
                ps.setString(2, row.fileName());
                ps.setString(3, row.qualifiedName());
                ps.setString(4, toVectorLiteral(row.vector()));
                ps.setBoolean(5, row.truncated());

            }
        );
    }

    /** Reads back every stored function embedding for one submission. */
    public List<StoredEmbedding> findBySubmissionId(Long submissionId) {
        return jdbc.query(
            "SELECT submission_id, qualified_name, embedding::text AS embedding_text "
                + "FROM submission_function_embedding WHERE submission_id = ?",
            
            (rs, rowNum) ->
                new StoredEmbedding(
                    rs.getLong("submission_id"),
                    rs.getString("qualified_name"),
                    parseVectorLiteral(rs.getString("embedding_text"))
                ),
            submissionId
        );
    }

    

}