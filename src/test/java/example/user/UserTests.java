package example.user;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UserTests {
    
    @LocalServerPort
    int port;

    @Autowired
    RestClient.Builder builder;

    @TestConfiguration
    static class RestClientTestConfig {
        @Bean
        RestClient.Builder builder() {
            return RestClient.builder();
        }
    }

    private RestClient client(String username, String password) {
        return builder
                .baseUrl("http://localhost:" + port)
                .defaultHeaders(headers -> headers.setBasicAuth(username, password))
                .build();
    }

    private RestClient unauthenticatedClient() {
        return builder
                .baseUrl("http://localhost:" + port)
                .build();
    }

    // ========== REGISTRATION TESTS ==========

    @Test
    void shouldRegisterNewUser() {
        RestClient client = unauthenticatedClient();
        
        UserRegistrationRequest request = new UserRegistrationRequest("newuser", "password123");
        
        ResponseEntity<Void> response = client.post()
                .uri("/users/register")
                .body(request)
                .retrieve()
                .toEntity(Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().getPath()).isEqualTo("/users/newuser");
    }

    @Test
    void shouldNotRegisterUserWithDuplicateUsername() {
        RestClient client = unauthenticatedClient();
        
        // Register first user
        UserRegistrationRequest request = new UserRegistrationRequest("duplicateuser", "password123");
        client.post()
                .uri("/users/register")
                .body(request)
                .retrieve()
                .toEntity(Void.class);

        // Try to register again with same username
        ResponseEntity<Void> response = client.post()
                .uri("/users/register")
                .body(request)
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldNotRegisterUserWithBlankUsername() {
        RestClient client = unauthenticatedClient();
        
        UserRegistrationRequest request = new UserRegistrationRequest("", "password123");
        
        ResponseEntity<Void> response = client.post()
                .uri("/users/register")
                .body(request)
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldNotRegisterUserWithShortUsername() {
        RestClient client = unauthenticatedClient();
        
        UserRegistrationRequest request = new UserRegistrationRequest("ab", "password123");
        
        ResponseEntity<Void> response = client.post()
                .uri("/users/register")
                .body(request)
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldNotRegisterUserWithInvalidUsernameCharacters() {
        RestClient client = unauthenticatedClient();
        
        UserRegistrationRequest request = new UserRegistrationRequest("user@name!", "password123");
        
        ResponseEntity<Void> response = client.post()
                .uri("/users/register")
                .body(request)
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldNotRegisterUserWithShortPassword() {
        RestClient client = unauthenticatedClient();
        
        UserRegistrationRequest request = new UserRegistrationRequest("validuser", "short");
        
        ResponseEntity<Void> response = client.post()
                .uri("/users/register")
                .body(request)
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ========== AUTHENTICATION TESTS ==========

    @Test
    void shouldAuthenticateRegisteredUser() {
        // Register user
        RestClient unauthClient = unauthenticatedClient();
        UserRegistrationRequest request = new UserRegistrationRequest("authuser", "password123");
        unauthClient.post()
                .uri("/users/register")
                .body(request)
                .retrieve()
                .toEntity(Void.class);

        // Try to authenticate
        RestClient client = client("authuser", "password123");
        
        ResponseEntity<String> response = client.get()
                .uri("/users/authuser")
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .body(clientResponse.bodyTo(String.class));
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldNotAuthenticateWithWrongPassword() {
        // Register user
        RestClient unauthClient = unauthenticatedClient();
        UserRegistrationRequest request = new UserRegistrationRequest("testuser", "correctpass");
        unauthClient.post()
                .uri("/users/register")
                .body(request)
                .retrieve()
                .toEntity(Void.class);

        // Try with wrong password
        RestClient client = client("testuser", "wrongpass");
        
        ResponseEntity<String> response = client.get()
                .uri("/users/testuser")
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ========== GET USER TESTS ==========

    @Test
    void shouldGetOwnUserProfile() {
        // Register and login
        RestClient unauthClient = unauthenticatedClient();
        UserRegistrationRequest request = new UserRegistrationRequest("getuser", "password123");
        unauthClient.post()
                .uri("/users/register")
                .body(request)
                .retrieve()
                .toEntity(Void.class);

        RestClient client = client("getuser", "password123");
        
        ResponseEntity<String> response = client.get()
                .uri("/users/getuser")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("getuser");
    }

    @Test
    void shouldNotGetOtherUserProfile() {
        // Register two users
        RestClient unauthClient = unauthenticatedClient();
        
        unauthClient.post()
                .uri("/users/register")
                .body(new UserRegistrationRequest("user1", "password123"))
                .retrieve()
                .toEntity(Void.class);
        
        unauthClient.post()
                .uri("/users/register")
                .body(new UserRegistrationRequest("user2", "password123"))
                .retrieve()
                .toEntity(Void.class);

        // User1 tries to get User2's profile
        RestClient client = client("user1", "password123");
        
        ResponseEntity<String> response = client.get()
                .uri("/users/user2")
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldNotGetUserProfileWithoutAuthentication() {
        // Register user
        RestClient unauthClient = unauthenticatedClient();
        unauthClient.post()
                .uri("/users/register")
                .body(new UserRegistrationRequest("someuser", "password123"))
                .retrieve()
                .toEntity(Void.class);

        // Try without authentication
        ResponseEntity<String> response = unauthClient.get()
                .uri("/users/someuser")
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ========== CHANGE PASSWORD TESTS ==========

    @Test
    void shouldChangePasswordSuccessfully() {
        // Register user
        RestClient unauthClient = unauthenticatedClient();
        unauthClient.post()
                .uri("/users/register")
                .body(new UserRegistrationRequest("changeuser", "oldpassword"))
                .retrieve()
                .toEntity(Void.class);

        // Change password
        RestClient client = client("changeuser", "oldpassword");
        ChangePasswordRequest changeRequest = new ChangePasswordRequest("oldpassword", "NewPass123");
        
        ResponseEntity<Void> response = client.put()
                .uri("/users/changeuser/change-password")
                .body(changeRequest)
                .retrieve()
                .toEntity(Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify new password works
        RestClient newClient = client("changeuser", "NewPass123");
        ResponseEntity<String> verifyResponse = newClient.get()
                .uri("/users/changeuser")
                .retrieve()
                .toEntity(String.class);

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldNotChangePasswordWithWrongCurrentPassword() {
        // Register user
        RestClient unauthClient = unauthenticatedClient();
        unauthClient.post()
                .uri("/users/register")
                .body(new UserRegistrationRequest("wrongpass", "correctpass"))
                .retrieve()
                .toEntity(Void.class);

        // Try to change with wrong current password
        RestClient client = client("wrongpass", "correctpass");
        ChangePasswordRequest changeRequest = new ChangePasswordRequest("wrongcurrent", "NewPass123");
        
        ResponseEntity<Void> response = client.put()
                .uri("/users/wrongpass/change-password")
                .body(changeRequest)
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldNotChangeOtherUserPassword() {
        // Register two users
        RestClient unauthClient = unauthenticatedClient();
        unauthClient.post()
                .uri("/users/register")
                .body(new UserRegistrationRequest("user1", "password123"))
                .retrieve()
                .toEntity(Void.class);
        
        unauthClient.post()
                .uri("/users/register")
                .body(new UserRegistrationRequest("user2", "password456"))
                .retrieve()
                .toEntity(Void.class);

        // User1 tries to change User2's password
        RestClient client = client("user1", "password123");
        ChangePasswordRequest changeRequest = new ChangePasswordRequest("password456", "NewPass123");
        
        ResponseEntity<Void> response = client.put()
                .uri("/users/user2/change-password")
                .body(changeRequest)
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldNotChangePasswordWithShortNewPassword() {
        // Register user
        RestClient unauthClient = unauthenticatedClient();
        unauthClient.post()
                .uri("/users/register")
                .body(new UserRegistrationRequest("shortpw", "validpass123"))
                .retrieve()
                .toEntity(Void.class);

        // Try with short new password
        RestClient client = client("shortpw", "validpass123");
        ChangePasswordRequest changeRequest = new ChangePasswordRequest("validpass123", "short");
        
        ResponseEntity<Void> response = client.put()
                .uri("/users/shortpw/change-password")
                .body(changeRequest)
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldNotChangePasswordWithoutRequiredComplexity() {
        // Register user
        RestClient unauthClient = unauthenticatedClient();
        unauthClient.post()
                .uri("/users/register")
                .body(new UserRegistrationRequest("complex", "ValidPass123"))
                .retrieve()
                .toEntity(Void.class);

        // Try with password lacking complexity
        RestClient client = client("complex", "ValidPass123");
        ChangePasswordRequest changeRequest = new ChangePasswordRequest("ValidPass123", "alllowercase");
        
        ResponseEntity<Void> response = client.put()
                .uri("/users/complex/change-password")
                .body(changeRequest)
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ========== DELETE ACCOUNT TESTS ==========

    @Test
    void shouldDeleteOwnAccount() {
        // Register user
        RestClient unauthClient = unauthenticatedClient();
        unauthClient.post()
                .uri("/users/register")
                .body(new UserRegistrationRequest("deleteuser", "password123"))
                .retrieve()
                .toEntity(Void.class);

        // Delete account
        RestClient client = client("deleteuser", "password123");
        
        ResponseEntity<Void> response = client.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/users/deleteuser")
                .body("password123")
                .retrieve()
                .toEntity(Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify user can't login anymore
        ResponseEntity<String> verifyResponse = client.get()
                .uri("/users/deleteuser")
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldNotDeleteAccountWithWrongPassword() {
        // Register user
        RestClient unauthClient = unauthenticatedClient();
        unauthClient.post()
                .uri("/users/register")
                .body(new UserRegistrationRequest("nodelete", "correctpass"))
                .retrieve()
                .toEntity(Void.class);

        // Try to delete with wrong password
        RestClient client = client("nodelete", "correctpass");
        
        ResponseEntity<Void> response = client.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/users/nodelete")
                .body("wrongpass")
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldNotDeleteOtherUserAccount() {
        // Register two users
        RestClient unauthClient = unauthenticatedClient();
        unauthClient.post()
                .uri("/users/register")
                .body(new UserRegistrationRequest("deluser1", "password123"))
                .retrieve()
                .toEntity(Void.class);
        
        unauthClient.post()
                .uri("/users/register")
                .body(new UserRegistrationRequest("deluser2", "password456"))
                .retrieve()
                .toEntity(Void.class);

        // User1 tries to delete User2's account
        RestClient client = client("deluser1", "password123");
        
        ResponseEntity<Void> response = client.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/users/deluser2")
                .body("password456")
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldNotDeleteNonExistentUser() {
        // Register user
        RestClient unauthClient = unauthenticatedClient();
        unauthClient.post()
                .uri("/users/register")
                .body(new UserRegistrationRequest("realuser", "password123"))
                .retrieve()
                .toEntity(Void.class);

        // Try to delete non-existent user
        RestClient client = client("realuser", "password123");
        
        ResponseEntity<Void> response = client.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/users/fakeuser")
                .body("password123")
                .exchange((req, clientResponse) -> {
                    return ResponseEntity.status(clientResponse.getStatusCode())
                            .build();
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
