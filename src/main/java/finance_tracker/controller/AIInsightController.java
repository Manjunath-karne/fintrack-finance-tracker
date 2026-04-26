package finance_tracker.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import finance_tracker.model.Transaction;
import finance_tracker.model.User;
import finance_tracker.service.TransactionService;
import finance_tracker.service.UserService;

@RestController
@RequestMapping("/api/insights")
@CrossOrigin(origins = "*")
public class AIInsightController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    private User getUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public ResponseEntity<?> getInsights() {
        try {
            User user = getUser();
            List<Transaction> transactions = transactionService.getAllTransactions(user);

            if (transactions.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "summary", "No transactions found yet.",
                    "insights", List.of("Start by adding your income and expenses to get personalized insights!"),
                    "score", 50,
                    "scoreLabel", "Getting Started"
                ));
            }

            double totalIncome = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount).sum();

            double totalExpense = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount).sum();

            Map<String, Double> categorySpend = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                    Transaction::getCategory,
                    Collectors.summingDouble(Transaction::getAmount)
                ));

            double savingsRate = totalIncome > 0 ? ((totalIncome - totalExpense) / totalIncome) * 100 : 0;
            double balance = totalIncome - totalExpense;

            List<String> insights = new ArrayList<>();
            List<String> tips = new ArrayList<>();

            // Savings rate analysis
            if (savingsRate >= 30) {
                insights.add("🌟 Excellent! You're saving " + String.format("%.1f", savingsRate) + "% of your income. You're on the right track!");
            } else if (savingsRate >= 20) {
                insights.add("✅ Good job! You're saving " + String.format("%.1f", savingsRate) + "% of your income. Try to push it to 30%.");
            } else if (savingsRate >= 10) {
                insights.add("⚠️ You're saving " + String.format("%.1f", savingsRate) + "% of your income. Financial experts recommend saving at least 20%.");
            } else if (savingsRate > 0) {
                insights.add("🔴 Your savings rate is only " + String.format("%.1f", savingsRate) + "%. Try to cut expenses to save more.");
            } else {
                insights.add("🚨 You're spending more than you earn! Immediate action needed to avoid debt.");
            }

            // Top spending category
            if (!categorySpend.isEmpty()) {
                String topCategory = Collections.max(categorySpend.entrySet(), Map.Entry.comparingByValue()).getKey();
                double topAmount = categorySpend.get(topCategory);
                double topPercent = totalExpense > 0 ? (topAmount / totalExpense) * 100 : 0;
                insights.add("📊 Your highest spending category is " + topCategory + " (₹" + String.format("%.0f", topAmount) + " — " + String.format("%.0f", topPercent) + "% of expenses).");

                if (topPercent > 40) {
                    tips.add("💡 " + topCategory + " takes up " + String.format("%.0f", topPercent) + "% of your budget. Consider setting a strict limit for this category.");
                }
            }

            // Food spending
            if (categorySpend.containsKey("Food")) {
                double foodSpend = categorySpend.get("Food");
                double foodPercent = totalIncome > 0 ? (foodSpend / totalIncome) * 100 : 0;
                if (foodPercent > 30) {
                    tips.add("🍔 You're spending " + String.format("%.1f", foodPercent) + "% of income on Food. Try cooking at home more often to save ₹" + String.format("%.0f", foodSpend * 0.3) + " per month.");
                }
            }

            // Entertainment spending
            if (categorySpend.containsKey("Entertainment")) {
                double entSpend = categorySpend.get("Entertainment");
                double entPercent = totalIncome > 0 ? (entSpend / totalIncome) * 100 : 0;
                if (entPercent > 10) {
                    tips.add("🎬 Entertainment spending is at " + String.format("%.1f", entPercent) + "% of income. Consider reducing it to under 10% to save ₹" + String.format("%.0f", entSpend * 0.3) + ".");
                }
            }

            // Shopping spending
            if (categorySpend.containsKey("Shopping")) {
                double shopSpend = categorySpend.get("Shopping");
                tips.add("🛍 You spent ₹" + String.format("%.0f", shopSpend) + " on Shopping. Try the 24-hour rule — wait a day before making non-essential purchases.");
            }

            // Balance tips
            if (balance > 0) {
                tips.add("💰 You have ₹" + String.format("%.0f", balance) + " saved. Consider investing 50% in a mutual fund or fixed deposit.");
                tips.add("📈 Set up an automatic transfer of ₹" + String.format("%.0f", balance * 0.5) + " to a savings account each month.");
            }

            // General tips
            tips.add("📱 Track every expense — even small ones. Small leaks sink big ships!");
            tips.add("🎯 Set a monthly budget for each category and stick to it.");

            // Financial health score
            int score;
            String scoreLabel;
            if (savingsRate >= 30 && balance > 0) { score = 90; scoreLabel = "Excellent"; }
            else if (savingsRate >= 20) { score = 75; scoreLabel = "Good"; }
            else if (savingsRate >= 10) { score = 55; scoreLabel = "Average"; }
            else if (savingsRate > 0) { score = 35; scoreLabel = "Needs Work"; }
            else { score = 15; scoreLabel = "Critical"; }

            String summary = "Based on your " + transactions.size() + " transactions — Income: ₹" +
                String.format("%.0f", totalIncome) + ", Expenses: ₹" +
                String.format("%.0f", totalExpense) + ", Balance: ₹" +
                String.format("%.0f", balance) + ".";

            Map<String, Object> result = new HashMap<>();
            result.put("summary", summary);
            result.put("insights", insights);
            result.put("tips", tips);
            result.put("score", score);
            result.put("scoreLabel", scoreLabel);
            result.put("savingsRate", String.format("%.1f", savingsRate));
            result.put("totalIncome", totalIncome);
            result.put("totalExpense", totalExpense);
            result.put("balance", balance);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}