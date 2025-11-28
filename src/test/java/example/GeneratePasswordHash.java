package example;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePasswordHash {
    @Test
    void generateHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("sarah1/abc123: " + encoder.encode("abc123"));
        System.out.println("kumar2/xyz789: " + encoder.encode("xyz789"));
        System.out.println("hank/qrs456: " + encoder.encode("qrs456"));
    }
}
