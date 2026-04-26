package com.financetracker.financetracker.service;

import com.financetracker.financetracker.model.Transaction;
import com.financetracker.financetracker.model.User;
import com.financetracker.financetracker.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public Transaction addTransaction(User user, String title, Double amount,
            Transaction.TransactionType type, String category, String description) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setTitle(title);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setCategory(category);
        transaction.setDescription(description);
        return transactionRepository.save(transaction);
    }

    public List<Transaction> getAllTransactions(User user) {
        return transactionRepository.findByUserOrderByDateDesc(user);
    }

    public List<Transaction> getTransactionsByMonth(User user, int month, int year) {
        return transactionRepository.findByUserAndMonthAndYear(user, month, year);
    }

    public Double getTotalIncome(User user) {
        Double total = transactionRepository.sumAmountByUserAndType(user, Transaction.TransactionType.INCOME);
        return total != null ? total : 0.0;
    }

    public Double getTotalExpense(User user) {
        Double total = transactionRepository.sumAmountByUserAndType(user, Transaction.TransactionType.EXPENSE);
        return total != null ? total : 0.0;
    }

    public void deleteTransaction(Long id) {
        transactionRepository.deleteById(id);
    }
}