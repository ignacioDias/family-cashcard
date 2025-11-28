package example.user;

import java.net.URI;
import java.security.Principal;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;


@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @PostMapping("/register")
    private ResponseEntity<Void> register(@Valid @RequestBody UserRegistrationRequest request, UriComponentsBuilder ucb) {
        if(userRepository.findById(request.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        var password = passwordEncoder.encode(request.getPassword());
        var registeredUser = new User(request.getUsername(), password);
        userRepository.save(registeredUser);
        URI locationOfNewCashCard = ucb
                .path("/users/{username}")
                .buildAndExpand(registeredUser.getUsername())
                .toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }
    @GetMapping("/{username}")
    private ResponseEntity<String> findByUsername(@PathVariable String username, Principal principal) {
        if(principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if(!principal.getName().equals(username))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        var userFromDB = userRepository.findById(username).orElse(null);
        if(userFromDB == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userFromDB.getUsername());
    }
   @PutMapping("/{username}/change-password")
    private ResponseEntity<Void> changePassword(@PathVariable String username, @Valid @RequestBody ChangePasswordRequest request, Principal principal) { 
            var currentPassword = request.getCurrentPassword();
            var newPassword = request.getNewPassword();
            if(!principal.getName().equals(username))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            var userFromDB = userRepository.findById(username)
                    .orElse(null);
            if(userFromDB == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            if(!passwordEncoder.matches(currentPassword, userFromDB.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            var hashedNewPassword = passwordEncoder.encode(newPassword);
            var updatedUser = new User(username, hashedNewPassword);

            userRepository.save(updatedUser);

            return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{username}")
    private ResponseEntity<Void> deleteAccount(@PathVariable String username, @RequestBody String password, Principal principal) { 
        var userFromDB = userRepository.findById(username).orElse(null);
        if(!principal.getName().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();  
        }
        if(userFromDB == null) {
            return ResponseEntity.notFound().build(); 
        }
        if(!passwordEncoder.matches(password, userFromDB.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userRepository.deleteById(username);
        return ResponseEntity.noContent().build();
    }
}
