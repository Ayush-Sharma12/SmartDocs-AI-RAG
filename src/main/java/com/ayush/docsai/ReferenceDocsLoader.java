package com.ayush.docsai;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class ReferenceDocsLoader {

    private static final Logger log = LoggerFactory.getLogger(ReferenceDocsLoader.class);

    @Value("classpath:/docs/technical-reference.pdf")
    private Resource pdfResource;

    @Value("${app.seed-reference-docs:false}")
    private boolean seedReferenceDocs;

    private final Optional<JdbcClient> jdbcClient;
    private final VectorStore vectorStore;

    public ReferenceDocsLoader(Optional<JdbcClient> jdbcClient, VectorStore vectorStore) {
        this.jdbcClient = jdbcClient;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void init() {
        if (!seedReferenceDocs) {
            log.info("Reference document seeding is disabled");
            return;
        }

        if (jdbcClient.isPresent()) {
            try {
                Integer count = jdbcClient.get().sql("select count(*) from vector_store")
                        .query(Integer.class)
                        .single();

                log.info("Current count of the Vector Store: {}", count);
                if (count == 0) {
                    log.info("Loading Spring Boot Reference PDF into Vector Store");
                    loadPdfToVectorStore();
                }
            } catch (Exception e) {
                log.warn("Database not available, using in-memory vector store: {}", e.getMessage());
                log.info("Loading Spring Boot Reference PDF into in-memory Vector Store");
                loadPdfToVectorStore();
            }
        } else {
            log.info("No database configured, using in-memory vector store");
            loadPdfToVectorStore();
        }
    }

    private void loadPdfToVectorStore() {
        try {
            var config = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder().withNumberOfBottomTextLinesToDelete(0)
                            .withNumberOfTopPagesToSkipBeforeDelete(0)
                            .build())
                    .withPagesPerDocument(1)
                    .build();

            var pdfReader = new PagePdfDocumentReader(pdfResource, config);
            var textSplitter = new TokenTextSplitter();
            List<Document> splitDocuments = textSplitter.apply(pdfReader.get())
                    .stream()
                    .map(document -> {
                        document.getMetadata().put("userId", -1L);
                        document.getMetadata().put("documentId", "reference-doc");
                        document.getMetadata().put("source", "technical-reference.pdf");
                        return document;
                    })
                    .toList();
            vectorStore.accept(splitDocuments);

            log.info("Application is ready");
        } catch (Exception e) {
            log.error("Failed to load PDF into vector store: {}", e.getMessage());
        }
    }
}
