package com.familycashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashCardApplicationTests {

	@Autowired
	TestRestTemplate restTemplate;

	private HttpEntity<?> auth(String username, String password) {
		String token = username + ":" + password;
		String base64 = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Basic " + base64);

		return new HttpEntity<>(headers);
	}

	@Test
	void shouldReturnACashCardWhenDataIsSaved() {
		ResponseEntity<String> response = restTemplate.exchange(
				"/cashcards/99",
				HttpMethod.GET,
				auth("sarah1", "abc123"),
				String.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isEqualTo(99);

		Double amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(123.45);
	}

	@Test
	void shouldNotReturnACashCardWithAnUnknownId() {
		ResponseEntity<String> response = restTemplate.exchange(
				"/cashcards/1000",
				HttpMethod.GET,
				auth("sarah1", "abc123"),
				String.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	@DirtiesContext
	void shouldCreateANewCashCard() {
		CashCard newCashCard = new CashCard(null, 250.00, null);

		ResponseEntity<Void> createResponse = restTemplate.exchange(
				"/cashcards",
				HttpMethod.POST,
				new HttpEntity<>(newCashCard, auth("sarah1", "abc123").getHeaders()),
				Void.class
		);

		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI location = createResponse.getHeaders().getLocation();

		ResponseEntity<String> getResponse = restTemplate.exchange(
				location,
				HttpMethod.GET,
				auth("sarah1", "abc123"),
				String.class
		);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext ctx = JsonPath.parse(getResponse.getBody());
		assertThat(ctx.read("$.amount", Double.class)).isEqualTo(250.00);
	}

	@Test
	void shouldReturnAllCashCardsWhenListIsRequested() {
		ResponseEntity<String> response = restTemplate.exchange(
				"/cashcards",
				HttpMethod.GET,
				auth("sarah1", "abc123"),
				String.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext doc = JsonPath.parse(response.getBody());
		assertThat(doc.read("$.length()", Integer.class)).isEqualTo(3);

		JSONArray ids = doc.read("$..id");
		assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);
	}

	@Test
	void shouldReturnAPageOfCashCards() {
		ResponseEntity<String> response = restTemplate.exchange(
				"/cashcards?page=0&size=1",
				HttpMethod.GET,
				auth("sarah1", "abc123"),
				String.class
		);

		DocumentContext doc = JsonPath.parse(response.getBody());
		JSONArray page = doc.read("$[*]");
		assertThat(page.size()).isEqualTo(1);
	}

	@Test
	void shouldReturnASortedPageOfCashCards() {
		ResponseEntity<String> response = restTemplate.exchange(
				"/cashcards?page=0&size=1&sort=amount,desc",
				HttpMethod.GET,
				auth("sarah1", "abc123"),
				String.class
		);

		DocumentContext doc = JsonPath.parse(response.getBody());
		assertThat(doc.read("$[0].amount", Double.class)).isEqualTo(150.00);
	}

	@Test
	void shouldReturnASortedPageOfCashCardsWithNoParametersAndUseDefaultValues() {
		ResponseEntity<String> response = restTemplate.exchange(
				"/cashcards",
				HttpMethod.GET,
				auth("sarah1", "abc123"),
				String.class
		);

		DocumentContext doc = JsonPath.parse(response.getBody());
		JSONArray amounts = doc.read("$..amount");

		assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
	}

	@Test
	void shouldNotReturnACashCardWhenUsingBadCredentials() {
		ResponseEntity<String> response = restTemplate.exchange(
				"/cashcards/99",
				HttpMethod.GET,
				auth("BAD-USER", "abc123"),
				String.class
		);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldRejectUsersWhoAreNotCardOwners() {
		ResponseEntity<String> response = restTemplate.exchange(
				"/cashcards/99",
				HttpMethod.GET,
				auth("hank-owns-no-cards", "qrs456"),
				String.class
		);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldNotAllowAccessToCashCardsTheyDoNotOwn() {
		ResponseEntity<String> response = restTemplate.exchange(
				"/cashcards/102",
				HttpMethod.GET,
				auth("sarah1", "abc123"),
				String.class
		);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	void shouldUpdateAnExistingCashCard() {
		CashCard update = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> req = new HttpEntity<>(update, auth("sarah1", "abc123").getHeaders());

		ResponseEntity<Void> response = restTemplate.exchange(
				"/cashcards/99",
				HttpMethod.PUT,
				req,
				Void.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate.exchange(
				"/cashcards/99",
				HttpMethod.GET,
				auth("sarah1", "abc123"),
				String.class
		);

		DocumentContext doc = JsonPath.parse(getResponse.getBody());
		assertThat(doc.read("$.amount", Double.class)).isEqualTo(19.99);
	}

	@Test
	void shouldNotUpdateACashCardThatDoesNotExist() {
		CashCard unknown = new CashCard(null, 19.99, null);
		HttpEntity<CashCard> req = new HttpEntity<>(unknown, auth("sarah1", "abc123").getHeaders());

		ResponseEntity<Void> response = restTemplate.exchange(
				"/cashcards/99999",
				HttpMethod.PUT,
				req,
				Void.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotUpdateACashCardThatIsOwnedBySomeoneElse() {
		CashCard update = new CashCard(null, 333.33, null);
		HttpEntity<CashCard> req = new HttpEntity<>(update, auth("sarah1", "abc123").getHeaders());

		ResponseEntity<Void> response = restTemplate.exchange(
				"/cashcards/102",
				HttpMethod.PUT,
				req,
				Void.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	void shouldDeleteAnExistingCashCard() {
		ResponseEntity<Void> response = restTemplate.exchange(
				"/cashcards/99",
				HttpMethod.DELETE,
				auth("sarah1", "abc123"),
				Void.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate.exchange(
				"/cashcards/99",
				HttpMethod.GET,
				auth("sarah1", "abc123"),
				String.class
		);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotDeleteACashCardThatDoesNotExist() {
		ResponseEntity<Void> response = restTemplate.exchange(
				"/cashcards/99999",
				HttpMethod.DELETE,
				auth("sarah1", "abc123"),
				Void.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotAllowDeletionOfCashCardsTheyDoNotOwn() {
		ResponseEntity<Void> deleteResponse = restTemplate.exchange(
				"/cashcards/102",
				HttpMethod.DELETE,
				auth("sarah1", "abc123"),
				Void.class
		);

		assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		ResponseEntity<String> getResponse = restTemplate.exchange(
				"/cashcards/102",
				HttpMethod.GET,
				auth("kumar2", "xyz789"),
				String.class
		);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}
