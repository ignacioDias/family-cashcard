package example.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import example.cashcard.CashCardRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    private final CashCardRepository cashcards;

    public DatabaseUserDetailsService(UserRepository users, CashCardRepository cashcards) {
        this.users = users;
        this.cashcards = cashcards;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = users.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user: " + username));

        boolean isOwner = cashcards.existsByOwner(username);
        String role = isOwner ? "CARD_OWNER" : "NON_OWNER";

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(role)
                .build();
    }

    
}
