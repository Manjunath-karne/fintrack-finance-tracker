package com.financetracker.financetracker.repository;

import com.financetracker.financetracker.model.Transaction;
import com.financetracker.financetracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserOrderByDateDesc(User user);
    List<Transaction> findByUserAndTypeOrderByDateDesc(User user, Transaction.TransactionType type);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = ?1 AND t.type = ?2")
    Double sumAmountByUserAndType(User user, Transaction.TransactionType type);
    
    @Query("SELECT t FROM Transaction t WHERE t.user = ?1 AND MONTH(t.date) = ?2 AND YEAR(t.date) = ?3")
    List<Transaction> findByUserAndMonthAndYear(User user, int month, int year);
}