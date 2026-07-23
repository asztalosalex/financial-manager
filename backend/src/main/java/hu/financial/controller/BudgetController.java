package hu.financial.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import hu.financial.model.Budget;
import hu.financial.service.BudgetService;
import hu.financial.dto.budget.CreateBudgetDto;
import hu.financial.dto.budget.BudgetResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/budgets")
@Tag(name = "Budget", description = "Budgets Handler")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @Operation(summary = "Create a new budget")
    @PostMapping
    public ResponseEntity<BudgetResponseDto> createBudget(@Valid @RequestBody CreateBudgetDto dto) {
        Budget budget = budgetService.mapToEntity(dto);
        Budget savedBudget = budgetService.createBudget(budget);
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.mapToDto(savedBudget));
    }

    @Operation(summary = "Get all budgets")
    @GetMapping
    public ResponseEntity<List<BudgetResponseDto>> getAllBudgets() {
        List<BudgetResponseDto> budgets = budgetService.getAllBudgets()
                .stream().map(budgetService::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(budgets);
    }

    @Operation(summary = "Get a budget by id")
    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponseDto> getBudgetById(@PathVariable Long id) {
        Budget budget = budgetService.getBudgetById(id);
        BudgetResponseDto responseDto = budget == null ? null : budgetService.mapToDto(budget);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(summary = "Update a budget by id")
    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponseDto> updateBudget(@PathVariable Long id, @Valid @RequestBody CreateBudgetDto dto) {
        Budget updatedBudget = budgetService.updateBudget(id, budgetService.mapToEntity(dto));
        return ResponseEntity.ok(budgetService.mapToDto(updatedBudget));
    }

    @Operation(summary = "Delete a budget by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }


}
