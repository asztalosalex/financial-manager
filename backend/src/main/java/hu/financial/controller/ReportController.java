package hu.financial.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import hu.financial.dto.report.BudgetStatusResponseDto;
import hu.financial.dto.report.CategoryBreakdownResponseDto;
import hu.financial.dto.report.ReportPeriod;
import hu.financial.dto.report.SummaryResponseDto;
import hu.financial.dto.report.TrendPeriod;
import hu.financial.dto.report.TrendResponseDto;
import hu.financial.service.ReportService;
import hu.financial.service.UserService;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Report", description = "Reports Handler")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    @Operation(summary = "Get the current user's monthly summary with the preceding month for comparison")
    @GetMapping("/summary")
    public ResponseEntity<SummaryResponseDto> getSummary(@RequestParam(required = false) String month) {
        ReportPeriod period = ReportPeriod.of(month);
        Long userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(reportService.summarize(userId, period));
    }

    @Operation(summary = "Get the current user's expenses for a month broken down by category")
    @GetMapping("/categories")
    public ResponseEntity<CategoryBreakdownResponseDto> getCategoryBreakdown(
            @RequestParam(required = false) String month) {
        ReportPeriod period = ReportPeriod.of(month);
        Long userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(reportService.breakdownExpensesByCategory(userId, period));
    }

    @Operation(summary = "Get the current user's monthly income and expense series ending with the requested month")
    @GetMapping("/trend")
    public ResponseEntity<TrendResponseDto> getTrend(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer months) {
        TrendPeriod period = TrendPeriod.of(month, months);
        Long userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(reportService.trend(userId, period));
    }

    @Operation(summary = "Get the current user's budgets for a month measured against the spending of that month")
    @GetMapping("/budget-status")
    public ResponseEntity<BudgetStatusResponseDto> getBudgetStatus(@RequestParam(required = false) String month) {
        ReportPeriod period = ReportPeriod.of(month);
        Long userId = userService.getCurrentUser().getId();
        return ResponseEntity.ok(reportService.budgetStatus(userId, period));
    }
}
