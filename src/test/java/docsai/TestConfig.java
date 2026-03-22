package docsai;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.vectorstore.VectorStore;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {

    @Bean
    public VectorStore vectorStore() {
        return mock(VectorStore.class);
    }
}
