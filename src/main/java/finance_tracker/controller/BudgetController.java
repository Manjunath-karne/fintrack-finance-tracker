package finance_tracker.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import finance_tracker.model.Budget;
import finance_tracker.model.User;
import finance_tracker.service.BudgetService;
import finance_tracker.service.UserService;

@RestController
@RequestMapping("/api/budgets")
@CrossOrigin(origins = "*")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private UserService userService;

    private User getUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    public ResponseEntity<?> setBudget(@RequestBody Map<String, String> request) {
        try {
            User user = getUser();
            Budget budget = budgetService.setBudget(
                    user,
                    request.get("category"),
                    Double.parseDouble(request.get("limitAmount")),
                    Integer.parseInt(request.get("month")),
                    Integer.parseInt(request.get("year"))
            );
            return ResponseEntity.ok(budget);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getBudgets(@RequestParam int month, @RequestParam int year) {
        User user = getUser();
        List<Budget> budgets = budgetService.getBudgets(user, month, year);
        List<Map<String, Object>> result = budgets.stream().map(b -> {
            Double spent = budgetService.getSpentAmount(user, b.getCategory(), month, year);
            return Map.of(
                    "id", (Object) b.getId(),
                    "category", b.getCategory(),
                    "limitAmount", b.getLimitAmount(),
                    "spentAmount", spent,
                    "remaining", b.getLimitAmount() - spent
            );
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.ok(Map.of("message", "Budget deleted!"));
    }

    @GetMapping("/alerts")
    public ResponseEntity<?> getBudgetAlerts() {
        try {
            User user = getUser();
            java.time.LocalDate now = java.time.LocalDate.now();
            List<Budget> budgets = budgetService.getBudgets(user, now.getMonthValue(), now.getYear());
            List<Map<String, Object>> alerts = new ArrayList<>();
            for (Budget b : budgets) {
                Double spent = budgetService.getSpentAmount(user, b.getCategory(), now.getMonthValue(), now.getYear());
                double pct = b.getLimitAmount() > 0 ? (spent / b.getLimitAmount()) * 100 : 0;
                if (pct >= 80) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("category", b.getCategory());
                    alert.put("spent", spent);
                    alert.put("limit", b.getLimitAmount());
                    alert.put("percentage", Math.round(pct));
                    alert.put("isOver", pct >= 100);
                    alerts.add(alert);
                }
            }
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}