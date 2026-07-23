package hu.financial.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import hu.financial.repository.BudgetRepository;
import hu.financial.model.Budget;
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
    
    public Budget getBudgetById(Long id) {
        return budgetRepository.findById(id).orElse(null);
    }

    public Budget updateBudget(Long id, Budget budget) {
        Budget existingBudget = budgetRepository.findById(id).orElse(null);
        validateBudgetForUpdate(existingBudget, budget);
        existingBudget.setAmount(budget.getAmount());
        return budgetRepository.save(existingBudget);
    }
    
    public void deleteBudget(Long id) {
        budgetRepository.deleteById(id);
    }
    
    private void validateBudgetForCreation(Budget budget) {
        if (budget.getAmount() <= 0) {
            throw new IllegalArgumentException("Budget amount must be greater than 0");
        }
    }
    
    private void validateBudgetForUpdate(Budget existingBudget, Budget budget) {
        if (budget.getAmount() <= 0) {
            throw new IllegalArgumentException("Budget amount must be greater than 0");
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
