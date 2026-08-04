package hu.financial.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

import hu.financial.exception.InvalidRequestParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SortWhitelistTest {

    private final SortWhitelist whitelist =
            SortWhitelist.of(List.of("date", "amount", "id"), Sort.Order.desc("id"));

    @Test
    void toSort_AppendsIdDescendingAsSecondaryKey() {
        assertEquals(Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id")), whitelist.toSort("date,desc"));
    }

    @Test
    void toSort_KeepsRequestedDirection() {
        assertEquals(Sort.by(Sort.Order.asc("amount"), Sort.Order.desc("id")), whitelist.toSort("amount,asc"));
    }

    @Test
    void toSort_AcceptsAnyDirectionCase() {
        assertEquals(Sort.by(Sort.Order.desc("amount"), Sort.Order.desc("id")), whitelist.toSort("amount,DESC"));
    }

    @Test
    void toSort_WithoutDirection_DefaultsToAscending() {
        assertEquals(Sort.by(Sort.Order.asc("date"), Sort.Order.desc("id")), whitelist.toSort("date"));
    }

    @Test
    void toSort_OnTheTieBreakerField_DoesNotRepeatIt() {
        assertEquals(Sort.by(Sort.Order.asc("id")), whitelist.toSort("id,asc"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "user.password", "password", "Date", "userId", "date,sideways", "date,desc,amount",
            "", "   ", "," })
    void toSort_RejectsAnythingOutsideTheWhitelist(String sort) {
        InvalidRequestParameterException exception =
                assertThrows(InvalidRequestParameterException.class, () -> whitelist.toSort(sort));
        assertEquals("sort", exception.getParameter());
    }

    @Test
    void toSort_RejectsNull() {
        assertThrows(InvalidRequestParameterException.class, () -> whitelist.toSort(null));
    }

    @Test
    void of_RejectsEmptyWhitelist() {
        assertThrows(IllegalArgumentException.class, () -> SortWhitelist.of(List.of(), Sort.Order.desc("id")));
    }

    @Test
    void of_RejectsMissingTieBreaker() {
        assertThrows(IllegalArgumentException.class, () -> SortWhitelist.of(List.of("date"), null));
    }
}
