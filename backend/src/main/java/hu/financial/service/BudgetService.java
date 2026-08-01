package hu.financial.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import hu.financial.repository.BudgetRepository;
import hu.financial.model.Budget;
import hu.financial.model.User;
import hu.financial.exception.budget.BudgetNotFoundException;
import hu.financial.exception.budget.BudgetValidationException;
import hu.financial.dto.budget.CreateBudgetDto;
import hu.financial.dto.budget.BudgetResponseDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BudgetService {


    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Transactional
    public Budget createBudget(Budget budget) {
        validateBudgetForCreation(budget);
        return budgetRepository.save(budget);
    }
    
    public List<Budget> getAllBudgets() {
        return budgetRepository.findAll();
    }

    public List<Budget> getBudgetsByUserId(Long userId) {
        return budgetRepository.findByUserId(userId);
    }
    
    public Budget getBudgetById(Long id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));
    }

    @Transactional
    public Budget updateBudget(Long id, Budget budget) {
        Budget existingBudget = budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));
        validateAmount(budget.getAmount());
        existingBudget.setAmount(budget.getAmount());
        existingBudget.setMonth(budget.getMonth());
        existingBudget.setCategory(budget.getCategory());
        return budgetRepository.save(existingBudget);
    }

    @Transactional
    public void deleteBudget(Long id) {
        Budget existingBudget = budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));
        budgetRepository.delete(existingBudget);
    }

    private void validateBudgetForCreation(Budget budget) {
        validateAmount(budget.getAmount());
    }

    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BudgetValidationException("Budget amount must be greater than 0");
        }
    }

    public Budget mapToEntity(CreateBudgetDto dto) {
        User currentUser = userService.getCurrentUser();
        Budget budget = new Budget();
        budget.setAmount(dto.getAmount().setScale(2, RoundingMode.HALF_UP));
        budget.setMonth(dto.getMonth());
        budget.setCategory(categoryService.getOwnedCategoryById(dto.getCategoryId(), currentUser.getId()));
        budget.setUser(currentUser);
        return budget;
    }

    public BudgetResponseDto mapToDto(Budget budget) {
        return new BudgetResponseDto(
                budget.getId(),
                budget.getAmount(),
                budget.getMonth(),
                budget.getCategory().getId(),
                budget.getCategory().getName());
    }

}
