package hu.financial;

import hu.financial.dto.user.GetUserByIdDto;
import hu.financial.model.Category;
import hu.financial.model.User;
import hu.financial.repository.CategoryRepository;
import hu.financial.repository.UserRepository;
import hu.financial.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FlywayPostgresIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:15"));

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void applicationContextStartsCleanly_WhenFlywayMigrationMatchesEntityMapping() {
        assertNotNull(userRepository);
    }

    @Test
    void emailUniqueConstraint_RejectsDuplicateAtDatabaseLevel() {
        userRepository.saveAndFlush(new User("uniqueuser1", "encoded-password", "duplicate@example.com"));
        User duplicate = new User("uniqueuser2", "encoded-password", "duplicate@example.com");

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(duplicate));
    }

    @Test
    void getUserByIdDto_DoesNotThrowLazyInitializationException_WhenUserHasCategories() {
        User user = userRepository.saveAndFlush(new User("lazyloaduser", "encoded-password", "lazyload@example.com"));
        categoryRepository.saveAndFlush(new Category("groceries", "food and household", user));

        GetUserByIdDto result = userService.getUserByIdDto(user.getId());

        assertEquals(1, result.categories().size());
        assertEquals("groceries", result.categories().get(0).name());
    }

    @Test
    void flywayHistory_ContainsEveryMigration_UpToTheIndexMigration() {
        List<String> applied = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = true order by installed_rank",
                String.class);

        assertEquals(List.of("1", "2", "3"), applied);
    }

    @Test
    void indexMigration_CreatesTheCompositeIndexBehindTheTransactionDefaultSort() {
        assertEquals(List.of("date", "user_id"), indexedColumns("idx_transactions_user_id_date"));
    }

    @Test
    void indexMigration_CreatesTheCompositeIndexBehindTheBudgetMonthFilter() {
        assertEquals(List.of("month_value", "user_id"), indexedColumns("idx_budgets_user_id_month_value"));
    }

    @Test
    void indexMigration_DoesNotCollideWithTheSingleColumnIndexesFromTheInitialMigration() {
        assertEquals(List.of("user_id"), indexedColumns("idx_transactions_user_id"));
        assertEquals(List.of("user_id"), indexedColumns("idx_budgets_user_id"));
        assertEquals(List.of("date"), indexedColumns("idx_transactions_date"));
    }

    private List<String> indexedColumns(String indexName) {
        return jdbcTemplate.queryForList(
                "select a.attname from pg_index i "
                        + "join pg_class c on c.oid = i.indexrelid "
                        + "join pg_attribute a on a.attrelid = i.indrelid and a.attnum = any(i.indkey) "
                        + "where c.relname = ? order by a.attname",
                String.class, indexName);
    }
}
