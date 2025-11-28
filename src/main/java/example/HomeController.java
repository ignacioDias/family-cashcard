package example;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Principal principal) {
        if (principal == null) {
            // User not logged in, redirect to register
            return "redirect:/register.html";
        }
        // User is logged in, redirect to dashboard
        return "redirect:/cashcards.html";
    }
}
