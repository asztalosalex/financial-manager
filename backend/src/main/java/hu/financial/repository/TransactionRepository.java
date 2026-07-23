package hu.financial.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import hu.financial.model.Transaction;
import hu.financial.model.User;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Transaction findByUser(User user);

    Transaction findByUserAndId(User user, Long id);

    List<Transaction> findByUserId(Long userId);
    
}
