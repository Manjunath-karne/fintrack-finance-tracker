package com.financetracker.financetracker.service;

import com.financetracker.financetracker.model.Budget;
import com.financetracker.financetracker.model.User;
import com.financetracker.financetracker.repository.BudgetRepository;
import com.financetracker.financetracker.repository.TransactionRepository;
import com.financetracker.financetracker.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public Budget setBudget(User user, String category, Double limitAmount, int month, int year) {
        Optional<Budget> existing = budgetRepository
                .findByUserAndCategoryAndMonthAndYear(user, category, month, year);
        Budget budget = existing.orElse(new Budget());
        budget.setUser(user);
        budget.setCategory(category);
        budget.setLimitAmount(limitAmount);
        budget.setMonth(month);
        budget.setYear(year);
        return budgetRepository.save(budget);
    }

    public List<Budget> getBudgets(User user, int month, int year) {
        return budgetRepository.findByUserAndMonthAndYear(user, month, year);
    }

    public Double getSpentAmount(User user, String category, int month, int year) {
        List<Transaction> transactions = transactionRepository
                .findByUserAndMonthAndYear(user, month, year);
        return transactions.stream()
                .filter(t -> t.getCategory().equals(category)
                        && t.getType() == Transaction.TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public void deleteBudget(Long id) {
        budgetRepository.deleteById(id);
    }
}