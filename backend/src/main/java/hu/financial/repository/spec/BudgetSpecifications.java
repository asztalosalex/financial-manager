package hu.financial.repository.spec;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import hu.financial.dto.budget.BudgetFilter;
import hu.financial.model.Budget;
import jakarta.persistence.criteria.Predicate;

public final class BudgetSpecifications {

    private BudgetSpecifications() {
    }

    public static Specification<Budget> ownedBy(Long userId, BudgetFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("user").get("id"), userId));
            if (filter.month() != null) {
                predicates.add(builder.between(root.<LocalDate>get("month"),
                        filter.month().atDay(1), filter.month().atEndOfMonth()));
            }
            if (filter.categoryId() != null) {
                predicates.add(builder.equal(root.get("category").get("id"), filter.categoryId()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
