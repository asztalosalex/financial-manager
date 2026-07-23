package hu.financial.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import hu.financial.model.Budget;
import hu.financial.model.User;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Budget findByUser(User user);

    Budget findByUserAndId(User user, Long id);

    List<Budget> findByUserId(Long userId);

}

