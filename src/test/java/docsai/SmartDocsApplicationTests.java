package docsai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.ayush.docsai.SmartDocsApp;

@SpringBootTest(
		classes = SmartDocsApp.class,
		properties = {
				"spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
				"spring.datasource.driver-class-name=org.h2.Driver",
				"spring.datasource.username=sa",
				"spring.datasource.password=",
				"spring.jpa.hibernate.ddl-auto=create-drop",
				"spring.sql.init.mode=never",
				"app.seed-reference-docs=false"
		}
)
@Import(TestConfig.class)
class SmartDocsApplicationTests {

	@Test
	void contextLoads() {
		
	}

}
