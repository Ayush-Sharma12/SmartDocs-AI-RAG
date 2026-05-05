package com.ayush.docsai.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DotenvLoader {

    private DotenvLoader() {
    }

    public static void loadIntoSystemProperties() {
        Path dotenvPath = Path.of(".env");
        if (!Files.exists(dotenvPath)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(dotenvPath);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int separatorIndex = line.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }

                String key = line.substring(0, separatorIndex).trim();
                String value = line.substring(separatorIndex + 1).trim();

                if (key.isEmpty() || alreadyDefined(key)) {
                    continue;
                }

                System.setProperty(key, stripWrappingQuotes(value));
            }
        } catch (IOException ignored) {
            // Fall back to normal Spring configuration if the local .env file cannot be read.
        }
    }

    private static boolean alreadyDefined(String key) {
        return System.getenv(key) != null || System.getProperty(key) != null;
    }

    private static String stripWrappingQuotes(String value) {
        if (value.length() >= 2) {
            boolean doubleQuoted = value.startsWith("\"") && value.endsWith("\"");
            boolean singleQuoted = value.startsWith("'") && value.endsWith("'");
            if (doubleQuoted || singleQuoted) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
