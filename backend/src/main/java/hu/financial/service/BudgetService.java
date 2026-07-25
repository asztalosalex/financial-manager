package hu.financial.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import hu.financial.repository.BudgetRepository;
import hu.financial.model.Budget;
import hu.financial.exception.budget.BudgetNotFoundException;
import hu.financial.exception.budget.BudgetValidationException;
import hu.financial.dto.budget.CreateBudgetDto;
import hu.financial.dto.budget.BudgetResponseDto;
import java.util.List;

@Service
public class BudgetService {


    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

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

    public Budget updateBudget(Long id, Budget budget) {
        Budget existingBudget = budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));
        validateBudgetForUpdate(existingBudget, budget);
        existingBudget.setAmount(budget.getAmount());
        return budgetRepository.save(existingBudget);
    }

    public void deleteBudget(Long id) {
        Budget existingBudget = budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));
        budgetRepository.delete(existingBudget);
    }

    private void validateBudgetForCreation(Budget budget) {
        if (budget.getAmount() <= 0) {
            throw new BudgetValidationException("Budget amount must be greater than 0");
        }
    }

    private void validateBudgetForUpdate(Budget existingBudget, Budget budget) {
        if (budget.getAmount() <= 0) {
            throw new BudgetValidationException("Budget amount must be greater than 0");
        }
    }

    public Budget mapToEntity(CreateBudgetDto dto) {
        Budget budget = new Budget();
        budget.setAmount(dto.getAmount());
        budget.setMonth(dto.getMonth());
        budget.setCategory(categoryService.getCategoryById(dto.getCategoryId()));
        budget.setUser(userService.getCurrentUser());
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
