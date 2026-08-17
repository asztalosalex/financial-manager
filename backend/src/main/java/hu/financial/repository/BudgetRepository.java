package hu.financial.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import hu.financial.model.Budget;
import hu.financial.model.Category;
import hu.financial.model.User;
import hu.financial.repository.projection.CategoryBudgetTotal;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long>, JpaSpecificationExecutor<Budget> {

    Budget findByUserAndCategoryAndMonth(User user, Category category, LocalDate month);

    @Query("""
            SELECT new hu.financial.repository.projection.CategoryBudgetTotal(
                b.category.id,
                b.category.name,
                SUM(b.amount))
            FROM Budget b
            WHERE b.user.id = :userId
                AND b.month >= :start
                AND b.month <= :end
            GROUP BY b.category.id, b.category.name
            ORDER BY b.category.id ASC
            """)
    List<CategoryBudgetTotal> summarizeBudgetsByCategory(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
