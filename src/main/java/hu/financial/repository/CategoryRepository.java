package hu.financial.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.financial.model.Category;
import hu.financial.model.User;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Category findByName(String name);

    Category findByDescription(String description);

    Category findByUser(User user);

    Category findByUserAndName(User user, String name);
}
