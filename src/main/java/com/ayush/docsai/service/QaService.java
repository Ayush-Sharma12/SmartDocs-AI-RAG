package com.ayush.docsai.service;

import com.ayush.docsai.dto.AnswerResponse;
import com.ayush.docsai.dto.QuestionRequest;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class QaService {

    private static final Logger logger = LoggerFactory.getLogger(QaService.class);

    private final Optional<ChatModel> chatModel;
    private final VectorStore vectorStore;
    private final DocumentService documentService;
    private final Resource promptTemplate;

    public QaService(Optional<ChatModel> chatModel,
                     VectorStore vectorStore,
                     DocumentService documentService,
                     @Value("classpath:/prompts/smartdocs-prompt.st") Resource promptTemplate) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.documentService = documentService;
        this.promptTemplate = promptTemplate;
    }

    @SuppressWarnings("null")
    public AnswerResponse answerQuestion(Long userId, QuestionRequest request) {
        logger.info("Answering question for userId={}, documentId={}", userId, request.getDocumentId());

        if (chatModel.isEmpty()) {
            logger.error("Chat model is not configured");
            throw new IllegalArgumentException("Chat model is not configured. Set OPENAI_API_KEY first.");
        }

        if (request.getDocumentId() != null) {
            documentService.getOwnedDocument(userId, request.getDocumentId());
        }

        List<Document> matches = findSimilarDocuments(userId, request.getQuestion(), request.getDocumentId());
        if (matches.isEmpty()) {
            logger.debug("No indexed content found for userId={} question={}", userId, request.getQuestion());
            throw new IllegalArgumentException("No indexed content found for this question");
        }

        try {
            String templateStr = new String(promptTemplate.getContentAsByteArray(), StandardCharsets.UTF_8);
            PromptTemplate promptTpl = new PromptTemplate(templateStr);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("input", request.getQuestion());
            parameters.put("documents", matches.stream().map(Document::getText).collect(Collectors.joining("\n\n")).trim());

            Prompt prompt = promptTpl.create(parameters);
                var result = chatModel.get().call(prompt).getResult();
                String answer = result == null || result.getOutput() == null ? "" : result.getOutput().getText();
                logger.debug("Model produced answer length={} for userId={}", answer.length(), userId);

                return AnswerResponse.builder()
                    .question(request.getQuestion())
                    .answer(answer)
                    .documentId(request.getDocumentId())
                    .build();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read prompt template", exception);
        }
    }

    private List<Document> findSimilarDocuments(Long userId, String question, Long documentId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        SearchRequest.Builder requestBuilder = SearchRequest.builder()
                .query(question != null ? question : "")
                .topK(5);

        if (documentId != null) {
            requestBuilder.filterExpression(builder.and(
                    builder.eq("userId", userId),
                    builder.eq("documentId", documentId)
            ).build());
        } else {
            requestBuilder.filterExpression(builder.eq("userId", userId).build());
        }

        return vectorStore.similaritySearch(requestBuilder.build());
    }
}
