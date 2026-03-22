package docsai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.ayush.docsai.SmartDocsApp;

@SpringBootTest(classes = SmartDocsApp.class)
@Import(TestConfig.class)
class SmartDocsApplicationTests {

	@Test
	void contextLoads() {
		
	}

}
