package example.cashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONArray;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashCardApplicationTests {

	@LocalServerPort
	int port;

    RestClient client;

	@Autowired
	RestClient.Builder builder;

	@TestConfiguration
	static class RestClientTestConfig {
		@Bean
		RestClient.Builder builder() {
			return RestClient.builder();
		}
	}

    @BeforeEach
    void setUp() {
        client = RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultHeaders(headers -> headers.setBasicAuth("sarah1", "abc123"))
            .build();
    }
	@Test
	void shouldReturnACashCardWhenDataIsSaved() {

		ResponseEntity<String> response = client.get()
				.uri("/cashcards/99")
				.retrieve()
				.toEntity(String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext doc = JsonPath.parse(response.getBody());
		assertThat(doc.read("$.id", Integer.class)).isEqualTo(99);
		assertThat(doc.read("$.amount", Double.class)).isEqualTo(123.45);
	}

	@Test
	void shouldNotReturnACashCardWithAnUnknownId() {

		ResponseEntity<String> response = client.get()
			.uri("/cashcards/70")
			.exchange((request, clientResponse) -> {
				return ResponseEntity.status(clientResponse.getStatusCode())
					.headers(clientResponse.getHeaders())
					.body(clientResponse.bodyTo(String.class));
			});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isBlank();
	}

    @Test
    @DirtiesContext
    void shouldCreateANewCashCard() {
        CashCard newCashCard = new CashCard(null, 250.00, null);

		ResponseEntity<Void> response = client.post()
			.uri("/cashcards")
            .body(newCashCard)
            .retrieve()
            .toEntity(Void.class);


        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        URI locationOfNewCashCard = response.getHeaders().getLocation();
        ResponseEntity<String> getResponse =client.get()
                .uri(locationOfNewCashCard) 
                .retrieve()
                .toEntity(String.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnAllCashCardsWhenListIsRequested() {

        ResponseEntity<String> response = client.get()
                .uri("/cashcards")
                .retrieve()
                .toEntity(String.class);
                
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        int cashCardCount = documentContext.read("$.length()");
        assertThat(cashCardCount).isEqualTo(3);

        JSONArray ids = documentContext.read("$..id");
        assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

        JSONArray amounts = documentContext.read("$..amount");
        assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.00, 150.00);
    }
    @Test
    void shouldReturnAPageOfCashCards() {
        ResponseEntity<String> response = client.get()
            .uri("/cashcards?page=0&size=1")
            .retrieve()
            .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(1);
    }
    @Test
    void shouldReturnASortedPageOfCashCards() {
        ResponseEntity<String> response = client.get()
            .uri("/cashcards?page=0&size=1&sort=amount,desc")
            .retrieve()
            .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray read = documentContext.read("$[*]");
        assertThat(read.size()).isEqualTo(1);

        double amount = documentContext.read("$[0].amount");
        assertThat(amount).isEqualTo(150.00);
    }

    @Test
    void shouldReturnASortedPageOfCashCardsWithNoParametersAndUseDefaultValues() {
        ResponseEntity<String> response = client.get()
                .uri("/cashcards")
                .retrieve()
                .toEntity(String.class);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(3);

        JSONArray amounts = documentContext.read("$..amount");
        assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
    }

    @Test
    void shouldNotReturnACashCardWhenUsingBadCredentials() {
        RestClient badClient = RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultHeaders(headers -> headers.setBasicAuth("wrongUser", "abc123"))
            .build();
        ResponseEntity<String> response = badClient.get()
            .uri("/cashcards/99")
			.exchange((request, clientResponse) -> {
				return ResponseEntity.status(clientResponse.getStatusCode())
					.headers(clientResponse.getHeaders())
					.body(clientResponse.bodyTo(String.class));
			});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        RestClient badClient2 = RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultHeaders(headers -> headers.setBasicAuth("sarah1", "badPass"))
            .build();
        response = badClient2.get()
                .uri("/cashcards/99")
                .exchange((request, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                        .headers(clientResponse.getHeaders())
                        .body(clientResponse.bodyTo(String.class));
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectUsersWhoAreNotCardOwners() {
        RestClient clientWithNoCards = RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .defaultHeaders(headers -> headers.setBasicAuth("hank-owns-no-cards", "qrs456"))
            .build();
        ResponseEntity<String> response = clientWithNoCards.get()
                .uri("/cashcards/")
                .exchange((request, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                        .headers(clientResponse.getHeaders())
                        .body(clientResponse.bodyTo(String.class));
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldNotAllowAccessToCashCardsTheyDoNotOwn() {
        ResponseEntity<String> response = client.get()
            .uri("/cashcards/102")
            .exchange((request, clientResponse) -> {
				return ResponseEntity.status(clientResponse.getStatusCode())
					.headers(clientResponse.getHeaders())
					.body(clientResponse.bodyTo(String.class));
			});
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DirtiesContext
    void shouldUpdateAnExistingCashCard() {
        CashCard cashCardUpdate = new CashCard(null, 19.99, null);
        HttpEntity<CashCard> request = new HttpEntity<>(cashCardUpdate);
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/99", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> getResponse = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .getForEntity("/cashcards/99", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
        Number id = documentContext.read("$.id");
        Double amount = documentContext.read("$.amount");
        assertThat(id).isEqualTo(99);
        assertThat(amount).isEqualTo(19.99);
    }

    @Test
    void shouldNotUpdateACashCardThatDoesNotExist() {
        CashCard unknownCard = new CashCard(null, 19.99, null);
        HttpEntity<CashCard> request = new HttpEntity<>(unknownCard);
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/99999", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotUpdateACashCardThatIsOwnedBySomeoneElse() {
        CashCard kumarsCard = new CashCard(null, 333.33, null);
        HttpEntity<CashCard> request = new HttpEntity<>(kumarsCard);
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/102", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

}
