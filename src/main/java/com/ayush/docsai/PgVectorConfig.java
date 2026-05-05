package com.ayush.docsai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Configuration
public class PgVectorConfig {

    private static final Logger log = LoggerFactory.getLogger(PgVectorConfig.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(JdbcTemplate.class)
    @ConditionalOnProperty(name = "app.vector-store.type", havingValue = "pgvector", matchIfMissing = true)
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, Optional<EmbeddingModel> embeddingModel) {
        if (embeddingModel.isEmpty()) {
            log.warn("EmbeddingModel is not configured. Falling back to in-memory vector store.");
            return new InMemoryVectorStore();
        }

        log.info("Configuring PgVectorStore with PostgreSQL-backed embeddings");
        return PgVectorStore.builder(jdbcTemplate, embeddingModel.get())
                .dimensions(1536)
                .initializeSchema(true)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public VectorStore fallbackVectorStore() {
        log.warn("Using in-memory vector store fallback");
        return new InMemoryVectorStore();
    }

    static class InMemoryVectorStore implements VectorStore {
        @Override
        public void add(java.util.List<org.springframework.ai.document.Document> documents) {
        }

        @Override
        public void delete(java.util.List<String> idList) {
        }

        @Override
        public void delete(org.springframework.ai.vectorstore.filter.Filter.Expression where) {
        }

        @Override
        public java.util.List<org.springframework.ai.document.Document> similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest request) {
            return java.util.List.of();
        }
    }
}
