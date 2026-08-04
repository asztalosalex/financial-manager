package hu.financial.repository.spec;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import hu.financial.dto.transaction.TransactionFilter;
import hu.financial.model.Transaction;
import jakarta.persistence.criteria.Predicate;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> ownedBy(Long userId, TransactionFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("user").get("id"), userId));
            if (filter.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.<LocalDate>get("date"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.<LocalDate>get("date"), filter.to()));
            }
            if (filter.categoryId() != null) {
                predicates.add(builder.equal(root.get("category").get("id"), filter.categoryId()));
            }
            if (filter.type() != null) {
                predicates.add(builder.equal(root.get("type"), filter.type()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
