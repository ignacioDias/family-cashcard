package example.user;

import java.net.URI;
import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
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
    private ResponseEntity<Void> register(@RequestBody User user, UriComponentsBuilder ucb) {
        if(user == null || user.getUsername() == null || user.getPassword() == null || user.getUsername().isBlank() || user.getPassword().isBlank()) { 
            return ResponseEntity.badRequest().build();
        }
        if (userRepository.findById(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        var password = passwordEncoder.encode(user.getPassword());
        var registeredUser = new User(user.getUsername(), password);
        userRepository.save(registeredUser);
        
        URI locationOfNewCashCard = ucb
                .path("/users/{username}")
                .buildAndExpand(registeredUser.getUsername())
                .toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }
}
