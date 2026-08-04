package hu.financial.dto.transaction;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import hu.financial.exception.InvalidRequestParameterException;
import hu.financial.model.enums.TransactionType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionFilterTest {

    private static final LocalDate JANUARY = LocalDate.of(2026, 1, 1);

    private static final LocalDate FEBRUARY = LocalDate.of(2026, 2, 1);

    @Test
    void fromAfterTo_IsRejectedWithTheFromParameterAsKey() {
        InvalidRequestParameterException exception = assertThrows(InvalidRequestParameterException.class,
                () -> new TransactionFilter(FEBRUARY, JANUARY, null, null));

        assertEquals("from", exception.getParameter());
    }

    @Test
    void equalFromAndTo_IsAcceptedBecauseTheBoundsAreInclusive() {
        TransactionFilter filter = new TransactionFilter(JANUARY, JANUARY, null, null);

        assertEquals(JANUARY, filter.from());
        assertEquals(JANUARY, filter.to());
    }

    @Test
    void aMissingBoundNeverTriggersTheRangeCheck() {
        assertEquals(FEBRUARY, new TransactionFilter(FEBRUARY, null, null, null).from());
        assertEquals(JANUARY, new TransactionFilter(null, JANUARY, null, null).to());
    }

    @Test
    void unfiltered_HoldsNoCriteria() {
        TransactionFilter filter = TransactionFilter.unfiltered();

        assertNull(filter.from());
        assertNull(filter.to());
        assertNull(filter.categoryId());
        assertNull(filter.type());
        assertEquals(new TransactionFilter(null, null, null, null), filter);
    }

    @Test
    void everyCriterionIsCarried() {
        TransactionFilter filter = new TransactionFilter(JANUARY, FEBRUARY, 7L, TransactionType.EXPENSE);

        assertEquals(JANUARY, filter.from());
        assertEquals(FEBRUARY, filter.to());
        assertEquals(Long.valueOf(7), filter.categoryId());
        assertEquals(TransactionType.EXPENSE, filter.type());
    }
}
