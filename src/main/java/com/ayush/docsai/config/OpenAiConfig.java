package com.ayush.docsai.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "OPENAI_API_KEY")
    public OpenAiApi openAiApi(@Value("${OPENAI_API_KEY:}") String apiKey) {
        return OpenAiApi.builder()
                .apiKey(apiKey)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    @ConditionalOnProperty(name = "OPENAI_API_KEY")
    public ChatModel chatModel(OpenAiApi openAiApi,
                               @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String model) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    @ConditionalOnProperty(name = "OPENAI_API_KEY")
    public EmbeddingModel embeddingModel(OpenAiApi openAiApi,
                                         @Value("${spring.ai.openai.embedding.options.model:text-embedding-3-small}") String model) {
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(model)
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.NONE, options);
    }
}
