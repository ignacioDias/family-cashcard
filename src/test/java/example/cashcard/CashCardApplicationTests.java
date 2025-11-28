package example.cashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashCardApplicationTests {

	@LocalServerPort
	int port;

	@Autowired
	RestClient.Builder builder;

	private RestClient client(String user, String pass) {
		String token = Base64.getEncoder()
				.encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
		return builder
				.baseUrl("http://localhost:" + port)
				.defaultHeader("Authorization", "Basic " + token)
				.build();
	}

	@Test
	void shouldReturnACashCardWhenDataIsSaved() {
		RestClient client = RestClient.builder()
				.baseUrl("http://localhost:" + port)
				.defaultHeaders(headers -> headers.setBasicAuth("sarah1", "abc123"))
				.build();

		ResponseEntity<String> response = client.get()
				.uri("/cashcards/99")
				.retrieve()
				.toEntity(String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext doc = JsonPath.parse(response.getBody());
		assertThat(doc.read("$.id", Integer.class)).isEqualTo(99);
		assertThat(doc.read("$.amount", Double.class)).isEqualTo(123.45);
	}

	@TestConfiguration
	static class RestClientTestConfig {
		@Bean
		RestClient.Builder builder() {
			return RestClient.builder();
		}
	}
}
