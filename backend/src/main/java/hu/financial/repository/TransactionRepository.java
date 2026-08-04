package hu.financial.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import hu.financial.model.Transaction;
import hu.financial.model.enums.TransactionType;
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
}
