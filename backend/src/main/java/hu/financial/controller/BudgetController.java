package hu.financial.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import hu.financial.model.Budget;
import hu.financial.service.BudgetService;
import hu.financial.service.UserService;
import hu.financial.exception.budget.BudgetNotFoundException;
import hu.financial.dto.budget.BudgetFilter;
import hu.financial.dto.budget.CreateBudgetDto;
import hu.financial.dto.budget.BudgetResponseDto;
import hu.financial.dto.common.PageResponse;
import hu.financial.web.SortWhitelist;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;


@RestController
@RequestMapping("/api/budgets")
@Validated
@Tag(name = "Budget", description = "Budgets Handler")
public class BudgetController {

    private static final String DEFAULT_PAGE = "0";

    private static final String DEFAULT_SIZE = "20";

    private static final String DEFAULT_SORT = "month,desc";

    private static final SortWhitelist SORT_WHITELIST =
            SortWhitelist.of(List.of("month", "amount", "id"), Sort.Order.desc("id"));

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private UserService userService;

    @Operation(summary = "Create a new budget")
    @PostMapping
    public ResponseEntity<BudgetResponseDto> createBudget(@Valid @RequestBody CreateBudgetDto dto) {
        Budget budget = budgetService.mapToEntity(dto);
        Budget savedBudget = budgetService.createBudget(budget);
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.mapToDto(savedBudget));
    }

    @Operation(summary = "Get the current user's budgets")
    @GetMapping
    public ResponseEntity<PageResponse<BudgetResponseDto>> getMyBudgets(
            @RequestParam(defaultValue = DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Long categoryId) {
        Pageable pageable = PageRequest.of(page, size, SORT_WHITELIST.toSort(sort));
        BudgetFilter filter = BudgetFilter.of(month, categoryId);
        Long userId = userService.getCurrentUser().getId();
        Page<Budget> budgets = budgetService.getBudgetsByUserId(userId, filter, pageable);
        return ResponseEntity.ok(PageResponse.from(budgets.map(budgetService::mapToDto)));
    }

    @Operation(summary = "Get one of the current user's budgets by id")
    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponseDto> getBudgetById(@PathVariable Long id) {
        Budget budget = getOwnedBudget(id);
        return ResponseEntity.ok(budgetService.mapToDto(budget));
    }

    @Operation(summary = "Update one of the current user's budgets by id")
    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponseDto> updateBudget(@PathVariable Long id, @Valid @RequestBody CreateBudgetDto dto) {
        getOwnedBudget(id);
        Budget updatedBudget = budgetService.updateBudget(id, budgetService.mapToEntity(dto));
        return ResponseEntity.ok(budgetService.mapToDto(updatedBudget));
    }

    @Operation(summary = "Delete one of the current user's budgets by id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        getOwnedBudget(id);
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }

    private Budget getOwnedBudget(Long id) {
        Long userId = userService.getCurrentUser().getId();
        Budget budget = budgetService.getBudgetById(id);
        if (budget == null || !budget.getUser().getId().equals(userId)) {
            throw new BudgetNotFoundException(id);
        }
        return budget;
    }
}
