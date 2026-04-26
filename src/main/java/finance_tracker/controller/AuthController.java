package finance_tracker.controller;

import finance_tracker.model.User;
import finance_tracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            User user = userService.registerUser(
                request.get("fullName"),
                request.get("email"),
                request.get("password")
            );
            return ResponseEntity.ok(Map.of(
                "message", "User registered successfully!",
                "email", user.getEmail(),
                "fullName", user.getFullName()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestParam String email) {
        return userService.findByEmail(email)
                .map(user -> ResponseEntity.ok(Map.of(
                        "email", user.getEmail(),
                        "fullName", user.getFullName(),
                        "monthlyBudget", user.getMonthlyBudget()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
