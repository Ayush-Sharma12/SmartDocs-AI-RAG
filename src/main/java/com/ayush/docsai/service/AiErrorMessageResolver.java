package com.ayush.docsai.service;

public final class AiErrorMessageResolver {

    private AiErrorMessageResolver() {
    }

    public static String resolve(Throwable exception, String fallbackMessage) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return fallbackMessage;
        }

        String lower = message.toLowerCase();
        if (lower.contains("insufficient_quota")) {
            return "OpenAI API quota is exhausted. Update billing or use a funded API key.";
        }
        if (lower.contains("api key")) {
            return "OpenAI API key is missing or invalid. Check OPENAI_API_KEY in .env.";
        }
        if (lower.contains("429")) {
            return "OpenAI rate limit or quota was hit. Please retry shortly or check billing.";
        }

        return fallbackMessage + ": " + message;
    }
}
