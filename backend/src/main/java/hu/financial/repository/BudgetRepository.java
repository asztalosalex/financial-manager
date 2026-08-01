package hu.financial.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import hu.financial.model.Budget;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserId(Long userId);

}

