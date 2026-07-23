package hu.financial.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import hu.financial.model.Category;
import hu.financial.model.User;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Category findByName(String name);

    Category findByDescription(String description);

    Category findByUser(User user);

    Category findByUserAndName(User user, String name);

    List<Category> findByUserId(Long userId);
}