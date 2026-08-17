package hu.financial;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class V3BudgetNormalizationMigrationIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:15"));

    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetToACleanSchemaAtVersionTwo() {
        dataSource = buildDataSource();
        jdbcTemplate = new JdbcTemplate(dataSource);

        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(dataSource).target(MigrationVersion.fromVersion("2")).load().migrate();
    }

    private DataSource buildDataSource() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(postgres.getJdbcUrl());
        ds.setUser(postgres.getUsername());
        ds.setPassword(postgres.getPassword());
        return ds;
    }

    private void migrateToLatest() {
        Flyway.configure().dataSource(dataSource).load().migrate();
    }

    private Long insertUser(String username) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO users (username, password, email) VALUES (?, ?, ?) RETURNING id",
                Long.class, username, "encoded-password", username + "@example.com");
    }

    private Long insertCategory(Long userId, String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO categories (name, description, user_id) VALUES (?, ?, ?) RETURNING id",
                Long.class, name, "legacy category", userId);
    }

    private Long insertBudget(Long userId, Long categoryId, LocalDate month, String amount) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO budgets (amount, month_value, user_id, category_id) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, new BigDecimal(amount), month, userId, categoryId);
    }

    @Test
    void v3Migration_NormalizesAPreExistingNonFirstDayRow_ToDayOne() {
        Long userId = insertUser("legacynormalize");
        Long categoryId = insertCategory(userId, "legacy category normalize");
        Long budgetId = insertBudget(userId, categoryId, LocalDate.of(2026, 5, 17), "12345.00");

        migrateToLatest();

        LocalDate month = jdbcTemplate.queryForObject(
                "SELECT month_value FROM budgets WHERE id = ?", LocalDate.class, budgetId);
        assertEquals(LocalDate.of(2026, 5, 1), month);
    }

    @Test
    void v3Migration_LeavesADayOneRowsAmountUntouched_WhenItHasNoDuplicate() {
        Long userId = insertUser("legacyuntouched");
        Long categoryId = insertCategory(userId, "legacy category untouched");
        Long budgetId = insertBudget(userId, categoryId, LocalDate.of(2026, 7, 1), "20000.00");

        migrateToLatest();

        BigDecimal amount = jdbcTemplate.queryForObject(
                "SELECT amount FROM budgets WHERE id = ?", BigDecimal.class, budgetId);
        assertEquals(0, new BigDecimal("20000.00").compareTo(amount));
    }

    @Test
    void v3Migration_MergesATwoRowDuplicateGroup_SummingOntoTheLowestIdAndDeletingTheOther() {
        Long userId = insertUser("legacymergetwo");
        Long categoryId = insertCategory(userId, "legacy category merge two");
        Long keepId = insertBudget(userId, categoryId, LocalDate.of(2026, 6, 1), "10000.00");
        Long dropId = insertBudget(userId, categoryId, LocalDate.of(2026, 6, 15), "5000.00");

        migrateToLatest();

        Integer remainingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM budgets WHERE user_id = ? AND category_id = ?",
                Integer.class, userId, categoryId);
        assertEquals(1, remainingCount);

        BigDecimal amount = jdbcTemplate.queryForObject(
                "SELECT amount FROM budgets WHERE id = ?", BigDecimal.class, keepId);
        assertEquals(0, new BigDecimal("15000.00").compareTo(amount));

        Integer dropStillPresent = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM budgets WHERE id = ?", Integer.class, dropId);
        assertEquals(0, dropStillPresent);
    }

    @Test
    void v3Migration_MergesAThreeRowDuplicateGroup_SummingAllOntoTheLowestId() {
        Long userId = insertUser("legacymergethree");
        Long categoryId = insertCategory(userId, "legacy category merge three");
        Long keepId = insertBudget(userId, categoryId, LocalDate.of(2026, 9, 1), "1000.00");
        insertBudget(userId, categoryId, LocalDate.of(2026, 9, 10), "2000.00");
        insertBudget(userId, categoryId, LocalDate.of(2026, 9, 28), "3000.00");

        migrateToLatest();

        Integer remainingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM budgets WHERE user_id = ? AND category_id = ?",
                Integer.class, userId, categoryId);
        assertEquals(1, remainingCount);

        BigDecimal amount = jdbcTemplate.queryForObject(
                "SELECT amount FROM budgets WHERE id = ?", BigDecimal.class, keepId);
        assertEquals(0, new BigDecimal("6000.00").compareTo(amount));
    }

    @Test
    void v3Migration_DoesNotMergeRowsOfDifferentCategories_EvenInTheSameMonth() {
        Long userId = insertUser("legacydistinctcategory");
        Long categoryOneId = insertCategory(userId, "legacy category a");
        Long categoryTwoId = insertCategory(userId, "legacy category b");
        insertBudget(userId, categoryOneId, LocalDate.of(2026, 4, 1), "1000.00");
        insertBudget(userId, categoryTwoId, LocalDate.of(2026, 4, 1), "2000.00");

        migrateToLatest();

        Integer remainingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM budgets WHERE user_id = ?", Integer.class, userId);
        assertEquals(2, remainingCount);
    }

    @Test
    void v3Migration_AddsAConstraintThatRejectsARawDuplicateInsertAfterwards() {
        Long userId = insertUser("legacyconstraint");
        Long categoryId = insertCategory(userId, "legacy category constraint");
        insertBudget(userId, categoryId, LocalDate.of(2026, 8, 1), "1000.00");

        migrateToLatest();

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO budgets (amount, month_value, user_id, category_id) VALUES (?, ?, ?, ?)",
                new BigDecimal("500.00"), LocalDate.of(2026, 8, 1), userId, categoryId));
    }

    @Test
    void v3Migration_NormalizesLegacyData_SoAFutureExactMatchLookupWouldDetectIt() {
        Long userId = insertUser("legacyfuturelookup");
        Long categoryId = insertCategory(userId, "legacy category future lookup");
        insertBudget(userId, categoryId, LocalDate.of(2026, 10, 17), "40000.00");

        migrateToLatest();

        Integer matchingNormalizedRowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM budgets WHERE user_id = ? AND category_id = ? AND month_value = ?",
                Integer.class, userId, categoryId, LocalDate.of(2026, 10, 1));
        assertEquals(1, matchingNormalizedRowCount);
    }
}
