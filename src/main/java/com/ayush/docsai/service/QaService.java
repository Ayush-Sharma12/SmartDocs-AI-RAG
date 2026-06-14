package com.ayush.docsai.service;

import com.ayush.docsai.dto.AnswerResponse;
import com.ayush.docsai.dto.QuestionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QaService {

    private static final Logger logger = LoggerFactory.getLogger(QaService.class);
    private static final int TOP_K = 5;

    private final Optional<ChatModel> chatModel;
    private final VectorStore vectorStore;
    private final DocumentService documentService;
    private final Resource promptTemplate;

    public QaService(
            Optional<ChatModel> chatModel,
            VectorStore vectorStore,
            DocumentService documentService,
            @Value("classpath:/prompts/smartdocs-prompt.st") Resource promptTemplate) {

        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.documentService = documentService;
        this.promptTemplate = promptTemplate;
    }

    public AnswerResponse answerQuestion(Long userId, QuestionRequest request) {

        logger.info(
                "Answering question for userId={}, documentId={}",
                userId,
                request.getDocumentId()
        );

        ChatModel model = chatModel.orElseThrow(() -> {
            logger.error("Chat model is not configured");
            return new IllegalArgumentException(
                    "Chat model is not configured. Set OPENAI_API_KEY first."
            );
        });

        validateDocumentOwnership(userId, request.getDocumentId());

        List<Document> documents = findSimilarDocuments(
                userId,
                request.getQuestion(),
                request.getDocumentId()
        );

        if (documents.isEmpty()) {
            logger.debug(
                    "No indexed content found for userId={} question={}",
                    userId,
                    request.getQuestion()
            );
            throw new IllegalArgumentException(
                    "No indexed content found for this question"
            );
        }

        Prompt prompt = buildPrompt(request.getQuestion(), documents);

        var result = model.call(prompt).getResult();

        String answer = result == null || result.getOutput() == null
                ? ""
                : result.getOutput().getText();

        logger.debug(
                "Model produced answer length={} for userId={}",
                answer.length(),
                userId
        );

        return AnswerResponse.builder()
                .question(request.getQuestion())
                .answer(answer)
                .documentId(request.getDocumentId())
                .build();
    }

    private void validateDocumentOwnership(Long userId, Long documentId) {
        if (documentId != null) {
            documentService.getOwnedDocument(userId, documentId);
        }
    }

    private Prompt buildPrompt(String question, List<Document> documents) {

        try {
            String templateContent = new String(
                    promptTemplate.getContentAsByteArray(),
                    StandardCharsets.UTF_8
            );

            String documentText = documents.stream()
                    .map(Document::getText)
                    .reduce("", (a, b) -> a + "\n\n" + b)
                    .trim();

            PromptTemplate template = new PromptTemplate(templateContent);

            return template.create(Map.of(
                    "input", question,
                    "documents", documentText
            ));

        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Unable to read prompt template",
                    exception
            );
        }
    }

    private List<Document> findSimilarDocuments(
            Long userId,
            String question,
            Long documentId) {

        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();

        SearchRequest.Builder searchRequest = SearchRequest.builder()
                .query(question == null ? "" : question)
                .topK(TOP_K);

        if (documentId != null) {
            searchRequest.filterExpression(
                    filterBuilder.and(
                            filterBuilder.eq("userId", userId),
                            filterBuilder.eq("documentId", documentId)
                    ).build()
            );
        } else {
            searchRequest.filterExpression(
                    filterBuilder.eq("userId", userId).build()
            );
        }

        return vectorStore.similaritySearch(searchRequest.build());
    }
}
