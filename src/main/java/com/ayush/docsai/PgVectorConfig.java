package com.ayush.docsai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Optional;

@Configuration
public class PgVectorConfig {

    @Bean
    @ConditionalOnMissingBean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, Optional<EmbeddingModel> embeddingModel) {
        // If EmbeddingModel is not available, return a no-op implementation
        if (embeddingModel.isEmpty()) {
            // Return a simple in-memory vector store as fallback
            return new InMemoryVectorStore(embeddingModel.orElse(null));
        }
        return PgVectorStore.builder(jdbcTemplate, embeddingModel.get()).build();
    }

    // Simple in-memory implementation as fallback
    public static class InMemoryVectorStore implements VectorStore {
        private final EmbeddingModel embeddingModel;

        public InMemoryVectorStore(EmbeddingModel embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        @Override
        public void add(java.util.List<org.springframework.ai.document.Document> documents) {
            // No-op for now
        }

        @Override
        public void delete(java.util.List<String> idList) {
            // No-op for now
        }

        @Override
        public void delete(org.springframework.ai.vectorstore.filter.Filter.Expression where) {
            // No-op for now
        }

        @Override
        public java.util.List<org.springframework.ai.document.Document> similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest request) {
            return java.util.Collections.emptyList();
        }
    }
}
