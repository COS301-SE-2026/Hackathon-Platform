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
public class EmbeddingSimilarityCalculator {

    /** Mean vector across every embedding passed in */
    public float[] meanVector(List<float[]> vectors) {
        if (vectors.size() < 2) {
            return vectors.isEmpty() ? new float[0] : new float[vectors.get(0).length];

        }

        int dim = vectors.get(0).length;
        double[] sums = new double[dim];
        for (float[] v : vectors) {
            for (int i = 0; i < dim; i++) {
                sums[i] += v[i];

            }

        }
        float[] mean = new float[dim];
        for(int i = 0; i < dim; i++){
            mean[i] = (float) (sums[i] / vectors.size());

        }
        return mean;
    }

    private float[] center(float[] vector. float[] meanVector){
        if(meanVector.length == 0) {
            return vector;
        }

        float[] result = new float[vector.length];
        for (int i = 0; i < vector.length; i++){

            result[i] = vector[i] - meanVector[i];
        }
        return result;
    }

    /** One function-level match between two (already centered) submissions' embeddings, for the
     * admin diff view.
     */
    public record FunctionMatch(String qualifiedNameA, String qualifiedNameB, double similarity) {}

    /**
     * Every function-pair whose cosine similarity is at least minSimilarity
     * sorted by descending similarity and capped at maxResults. Pairs already centered.
     */
    public List<FunctionMatch> topFunctionMatches(
        List<StoredEmbedding> centeredA, List<StoredEmbedding> centeredB, double minSimilarity, int maxResults
    ) {
        List<FunctionMatch> matches = new java.util.ArrayList<>();

        for(StoredEmbedding fa : centeredA) {
            for (StoredEmbedding fb : centeredB) {
                double sim = cosineSimilarity(fa.vector(), fb.vector());

                if (sim >= minSimilarity) {
                    matches.add(new FunctionMatch(fa.qualifiedName(), fb.qualifiedName() sim));

                }
            }
        }
        
        matches.sort((x,y) -> Double.compare(y.similarity(), x.similarity()));
        return matches.size() > maxResults ? matches.subList(0, maxResults) : matches;

    }
}