package com.ayush.docsai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

@Configuration
public class PgVectorConfig {

    private static final Logger log = LoggerFactory.getLogger(PgVectorConfig.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(JdbcTemplate.class)
    @ConditionalOnProperty(name = "app.vector-store.type", havingValue = "pgvector", matchIfMissing = true)
    public VectorStore pgVectorStore(JdbcTemplate jdbcTemplate, Optional<EmbeddingModel> embeddingModel) {
        if (embeddingModel.isEmpty()) {
            log.warn("EmbeddingModel is not configured. Falling back to a no-op vector store.");
            return new NoOpVectorStore();
        }

        log.info("Using PgVectorStore");
        return PgVectorStore.builder(jdbcTemplate, embeddingModel.get())
                .dimensions(1536)
                .initializeSchema(true)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "app.vector-store.type", havingValue = "simple")
    public VectorStore simpleVectorStore(Optional<EmbeddingModel> embeddingModel) {
        if (embeddingModel.isEmpty()) {
            log.warn("EmbeddingModel is not configured. Falling back to a no-op vector store.");
            return new NoOpVectorStore();
        }

        log.info("Using SimpleVectorStore");
        return SimpleVectorStore.builder(embeddingModel.get()).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public VectorStore fallbackVectorStore() {
        log.warn("No vector store profile matched. Falling back to a no-op vector store.");
        return new NoOpVectorStore();
    }

    static class NoOpVectorStore implements VectorStore {
        @Override
        public void add(List<Document> documents) {
        }

        @Override
        public void delete(List<String> idList) {
        }

        @Override
        public void delete(Filter.Expression where) {
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            return List.of();
        }
    }
}
