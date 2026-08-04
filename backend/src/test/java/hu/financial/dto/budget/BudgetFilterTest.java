package hu.financial.dto.budget;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import hu.financial.exception.InvalidRequestParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BudgetFilterTest {

    @Test
    void of_ParsesACalendarMonth() {
        BudgetFilter filter = BudgetFilter.of("2026-02", 7L);

        assertEquals(YearMonth.of(2026, 2), filter.month());
        assertEquals(Long.valueOf(7), filter.categoryId());
    }

    @Test
    void of_WithoutMonth_LeavesTheMonthUnset() {
        BudgetFilter filter = BudgetFilter.of(null, 7L);

        assertNull(filter.month());
        assertEquals(Long.valueOf(7), filter.categoryId());
    }

    @ParameterizedTest
    @ValueSource(strings = { "2026-02-01", "2026-2", "2026", "02-2026", "", "   ", "next month" })
    void of_RejectsAnythingThatIsNotYearDashMonth(String month) {
        InvalidRequestParameterException exception = assertThrows(InvalidRequestParameterException.class,
                () -> BudgetFilter.of(month, null));

        assertEquals("month", exception.getParameter());
    }

    @Test
    void unfiltered_HoldsNoCriteria() {
        BudgetFilter filter = BudgetFilter.unfiltered();

        assertNull(filter.month());
        assertNull(filter.categoryId());
        assertEquals(new BudgetFilter(null, null), filter);
    }
}
