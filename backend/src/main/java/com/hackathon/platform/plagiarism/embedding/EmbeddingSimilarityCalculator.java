package com.hackathon.platform.plagiarism.embedding;

import com.hackathon.platform.plagiarism.embedding.FunctionEmbeddingStore.StoredEmbedding;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Centering + cosine similarity + symmetric greedy best-match, operating on raw embeddings already
 * fetched into memory. Deliberately stateless and DB-free.
 *
 * non-fine-tuned CodeBERT embeddings are anisotropic.
 * mean-pooled vectors for almost any two code snippets land at 0.98-0.99 cosine similarity
 * regardless of actual similarity, because the model's embedding space occupies a narrow cone
 * rather than being spread out. Subtracting the mean embedding vector of the whole comparison
 * corpus (see meanVector) before computing cosine similarity is a standard, well-documented
 * mitigation for this and restored real separation in testing.
 *
 * The mean vector MUST be computed across every function embedding in the same batch of 
 * submissions being compared (a whole level's run), not per-pair.
 */
@Component
public class EmbeddingSimilarityCalculator {}