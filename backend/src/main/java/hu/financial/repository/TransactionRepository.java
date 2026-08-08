package hu.financial.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import hu.financial.model.Transaction;
import hu.financial.model.enums.TransactionType;
import hu.financial.repository.projection.CategoryExpenseTotal;
import hu.financial.repository.projection.MonthlyTotals;
import hu.financial.repository.projection.TransactionTotals;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @Query("""
            SELECT new hu.financial.repository.projection.TransactionTotals(
                SUM(CASE WHEN t.type = :income AND t.date >= :currentStart AND t.date <= :currentEnd THEN t.amount END),
                SUM(CASE WHEN t.type = :expense AND t.date >= :currentStart AND t.date <= :currentEnd THEN t.amount END),
                SUM(CASE WHEN t.type = :income AND t.date >= :previousStart AND t.date <= :previousEnd THEN t.amount END),
                SUM(CASE WHEN t.type = :expense AND t.date >= :previousStart AND t.date <= :previousEnd THEN t.amount END),
                SUM(CASE WHEN t.type = :income THEN t.amount END),
                SUM(CASE WHEN t.type = :expense THEN t.amount END),
                SUM(CASE WHEN t.type = :income AND t.date <= :previousEnd THEN t.amount END),
                SUM(CASE WHEN t.type = :expense AND t.date <= :previousEnd THEN t.amount END))
            FROM Transaction t
            WHERE t.user.id = :userId AND t.date <= :currentEnd
            """)
    TransactionTotals summarize(
            @Param("userId") Long userId,
            @Param("currentStart") LocalDate currentStart,
            @Param("currentEnd") LocalDate currentEnd,
            @Param("previousStart") LocalDate previousStart,
            @Param("previousEnd") LocalDate previousEnd,
            @Param("income") TransactionType income,
            @Param("expense") TransactionType expense);

    @Query("""
            SELECT new hu.financial.repository.projection.CategoryExpenseTotal(
                t.category.id,
                t.category.name,
                SUM(t.amount))
            FROM Transaction t
            WHERE t.user.id = :userId
                AND t.type = :expense
                AND t.date >= :start
                AND t.date <= :end
            GROUP BY t.category.id, t.category.name
            ORDER BY SUM(t.amount) DESC, t.category.id ASC
            """)
    List<CategoryExpenseTotal> summarizeExpensesByCategory(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("expense") TransactionType expense);

    @Query("""
            SELECT new hu.financial.repository.projection.MonthlyTotals(
                YEAR(t.date),
                MONTH(t.date),
                SUM(CASE WHEN t.type = :income THEN t.amount END),
                SUM(CASE WHEN t.type = :expense THEN t.amount END))
            FROM Transaction t
            WHERE t.user.id = :userId
                AND t.date >= :start
                AND t.date <= :end
            GROUP BY YEAR(t.date), MONTH(t.date)
            ORDER BY YEAR(t.date) ASC, MONTH(t.date) ASC
            """)
    List<MonthlyTotals> summarizeMonthlyTotals(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("income") TransactionType income,
            @Param("expense") TransactionType expense);
}
