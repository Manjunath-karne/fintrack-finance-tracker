package com.financetracker.financetracker.controller;

import com.financetracker.financetracker.model.Transaction;
import com.financetracker.financetracker.model.User;
import com.financetracker.financetracker.service.TransactionService;
import com.financetracker.financetracker.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    private User getUser(Authentication auth) {
        return userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    public ResponseEntity<?> addTransaction(@RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            User user = getUser(auth);
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
    public ResponseEntity<List<Transaction>> getAllTransactions(Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(transactionService.getAllTransactions(user));
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<Transaction>> getMonthlyTransactions(
            @RequestParam int month,
            @RequestParam int year,
            Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(transactionService.getTransactionsByMonth(user, month, year));
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(Authentication auth) {
        User user = getUser(auth);
        Double income = transactionService.getTotalIncome(user);
        Double expense = transactionService.getTotalExpense(user);
        return ResponseEntity.ok(Map.of(
                "totalIncome", income,
                "totalExpense", expense,
                "balance", income - expense
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok(Map.of("message", "Transaction deleted successfully!"));
    }
}