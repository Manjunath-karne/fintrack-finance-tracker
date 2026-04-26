package finance_tracker.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import finance_tracker.model.Transaction;
import finance_tracker.model.User;
import finance_tracker.service.TransactionService;
import finance_tracker.service.UserService;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    private User getUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    public ResponseEntity<?> addTransaction(@RequestBody Map<String, String> request) {
        try {
            User user = getUser();
            Transaction transaction = transactionService.addTransaction(
                    user,
                    request.get("title"),
                    Double.parseDouble(request.get("amount")),
                    Transaction.TransactionType.valueOf(request.get("type")),
                    request.get("category"),
                    request.get("description")
            );
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        User user = getUser();
        return ResponseEntity.ok(transactionService.getAllTransactions(user));
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<Transaction>> getMonthlyTransactions(
            @RequestParam int month,
            @RequestParam int year) {
        User user = getUser();
        return ResponseEntity.ok(transactionService.getTransactionsByMonth(user, month, year));
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        try {
            User user = getUser();
            double income = transactionService.getTotalIncome(user);
            double expense = transactionService.getTotalExpense(user);
            Map<String, Object> result = new HashMap<>();
            result.put("totalIncome", income);
            result.put("totalExpense", expense);
            result.put("balance", income - expense);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok(Map.of("message", "Transaction deleted!"));
    }
}