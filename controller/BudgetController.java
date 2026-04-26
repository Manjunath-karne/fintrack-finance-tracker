package com.financetracker.financetracker.controller;

import com.financetracker.financetracker.model.Budget;
import com.financetracker.financetracker.model.User;
import com.financetracker.financetracker.service.BudgetService;
import com.financetracker.financetracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/budgets")
@CrossOrigin(origins = "*")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private UserService userService;

    private User getUser(Authentication auth) {
        return userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    public ResponseEntity<?> setBudget(@RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            User user = getUser(auth);
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
    public ResponseEntity<?> getBudgets(
            @RequestParam int month,
            @RequestParam int year,
            Authentication auth) {
        User user = getUser(auth);
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
        return ResponseEntity.ok(Map.of("message", "Budget deleted successfully!"));
    }
}