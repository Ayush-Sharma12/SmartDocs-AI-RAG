package com.ayush.docsai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.shell.command.annotation.Command;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;


@Component
public class SpringAssistantCommand {

    @Value("classpath:/prompts/smartdocs-prompt.st")
    private Resource sbPromptTemplate;

    private final Optional<ChatModel> chatModel;
    private final Optional<VectorStore> vectorStore;

    public SpringAssistantCommand(Optional<ChatModel> chatModel, Optional<VectorStore> vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    @Command(command = "q", description = "Ask a question about Spring Boot documentation")
    public String question(String message) {
        if (chatModel.isEmpty()) {
            return "Error: ChatModel not configured. Please set the OPENAI_API_KEY environment variable.";
        }
        if (vectorStore.isEmpty()) {
            return "Error: VectorStore not available. Make sure the database or vector store is configured.";
        }
        
        try {
            String templateContent = new String(sbPromptTemplate.getContentAsByteArray(), StandardCharsets.UTF_8);
            PromptTemplate promptTemplate = new PromptTemplate(templateContent);
            Map<String, Object> promptParameters = new HashMap<>();
            promptParameters.put("input", message);
            promptParameters.put("documents", String.join("\n", findSimilarDocuments(message)));

            Prompt prompt = promptTemplate.create(promptParameters);
            
            return chatModel.get().call(prompt)
                    .getResult()
                    .getOutput()
                    .getText();
        } catch (IOException e) {
            return "Error reading prompt template: " + e.getMessage();
        }
    }

    private List<String> findSimilarDocuments(String message) {
        SearchRequest request = SearchRequest.builder()
                .query(message)
                .topK(3)
                .build();
        List<Document> similarDocuments = vectorStore.get().similaritySearch(request);
        return similarDocuments.stream().map(Document::getText).toList();
    }

}
