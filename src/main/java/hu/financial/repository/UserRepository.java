package hu.financial.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import hu.financial.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    
    User findByUsername(String username);

    User findByEmail(String email);

   
}

