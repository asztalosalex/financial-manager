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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

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

        assertEquals(1, result.getCategories().size());
        assertEquals("groceries", result.getCategories().get(0).getName());
    }
}
