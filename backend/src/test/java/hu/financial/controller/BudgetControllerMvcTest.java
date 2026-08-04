package hu.financial.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.financial.config.SecurityConfig;
import hu.financial.dto.budget.CreateBudgetDto;
import hu.financial.filter.JwtAuthenticationFilter;
import hu.financial.model.Budget;
import hu.financial.model.Category;
import hu.financial.model.User;
import hu.financial.repository.BudgetRepository;
import hu.financial.repository.CategoryRepository;
import hu.financial.security.CookieProperties;
import hu.financial.security.CsrfCookieFilter;
import hu.financial.security.RestAccessDeniedHandler;
import hu.financial.security.SecurityCookieFactory;
import hu.financial.service.BudgetService;
import hu.financial.service.CategoryService;
import hu.financial.service.JwtService;
import hu.financial.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BudgetController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, CookieProperties.class, RestAccessDeniedHandler.class,
        SecurityCookieFactory.class, CsrfCookieFilter.class, JwtService.class, BudgetService.class,
        CategoryService.class })
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.jwt.expiration-time=3600",
        "security.cookie.secure=false",
        "security.cookie.same-site=Lax"
})
class BudgetControllerMvcTest {

    private static final Long OWN_BUDGET_ID = 5L;
    private static final Long FOREIGN_BUDGET_ID = 6L;
    private static final Long UNKNOWN_BUDGET_ID = 404L;
    private static final Long OWN_CATEGORY_ID = 10L;
    private static final Long OTHER_OWN_CATEGORY_ID = 11L;
    private static final Long FOREIGN_CATEGORY_ID = 99L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    @MockBean
    private BudgetRepository budgetRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    private User currentUser;

    private Budget ownBudget;

    @BeforeEach
    void setUp() {
        currentUser = new User("testuser", "encoded-password", "test@example.com");
        currentUser.setId(1L);
        User otherUser = new User("other", "encoded-password", "other@example.com");
        otherUser.setId(2L);

        Category ownCategory = new Category("groceries", "food and household", currentUser);
        ownCategory.setId(OWN_CATEGORY_ID);
        Category otherOwnCategory = new Category("travel", "trips", currentUser);
        otherOwnCategory.setId(OTHER_OWN_CATEGORY_ID);
        Category foreignCategory = new Category("foreign", "not yours", otherUser);
        foreignCategory.setId(FOREIGN_CATEGORY_ID);

        ownBudget = new Budget(OWN_BUDGET_ID, new BigDecimal("100.00"), LocalDate.of(2026, 1, 1),
                currentUser, ownCategory);
        Budget foreignBudget = new Budget(FOREIGN_BUDGET_ID, new BigDecimal("50.00"), LocalDate.of(2026, 1, 1),
                otherUser, foreignCategory);

        when(userService.loadUserByUsername("testuser")).thenReturn(currentUser);
        when(userService.getCurrentUser()).thenReturn(currentUser);

        when(categoryRepository.findById(OWN_CATEGORY_ID)).thenReturn(Optional.of(ownCategory));
        when(categoryRepository.findById(OTHER_OWN_CATEGORY_ID)).thenReturn(Optional.of(otherOwnCategory));
        when(categoryRepository.findById(FOREIGN_CATEGORY_ID)).thenReturn(Optional.of(foreignCategory));

        when(budgetRepository.findById(OWN_BUDGET_ID)).thenReturn(Optional.of(ownBudget));
        when(budgetRepository.findById(FOREIGN_BUDGET_ID)).thenReturn(Optional.of(foreignBudget));
        when(budgetRepository.findById(UNKNOWN_BUDGET_ID)).thenReturn(Optional.empty());
        when(budgetRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<Budget>(List.of(), invocation.getArgument(1), 0));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Cookie authCookie() {
        return new Cookie("authToken", jwtService.generateToken(currentUser));
    }

    private Cookie csrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/budgets").cookie(authCookie()))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie, "XSRF-TOKEN cookie must be issued so the SPA can echo it back");
        return cookie;
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private static Specification<Budget> anySpecification() {
        return ArgumentMatchers.any();
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(budgetRepository).findAll(anySpecification(), pageable.capture());
        return pageable.getValue();
    }

    @Test
    void getMyBudgets_ReturnsPageWrapper_NotABareArray() throws Exception {
        when(budgetRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(ownBudget), invocation.getArgument(1), 1));

        mockMvc.perform(get("/api/budgets").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(OWN_BUDGET_ID))
                .andExpect(jsonPath("$.content[0].amount").value(100.00))
                .andExpect(jsonPath("$.content[0].month").value("2026-01-01"))
                .andExpect(jsonPath("$.content[0].categoryId").value(OWN_CATEGORY_ID))
                .andExpect(jsonPath("$.content[0].categoryName").value("groceries"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getMyBudgets_WithoutParameters_UsesPageZeroSizeTwentyAndMonthDescending() throws Exception {
        mockMvc.perform(get("/api/budgets").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));

        Pageable pageable = capturedPageable();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Order.desc("month"), Sort.Order.desc("id")), pageable.getSort());
    }

    @Test
    void getMyBudgets_MiddlePage_ReportsTotalsAndBoundaryFlags() throws Exception {
        when(budgetRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(ownBudget), invocation.getArgument(1), 137));

        mockMvc.perform(get("/api/budgets").param("page", "1").param("size", "20").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(137))
                .andExpect(jsonPath("$.totalPages").value(7))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void getMyBudgets_SortByAmount_AlwaysAppendsIdDescendingSoPagesDoNotOverlap() throws Exception {
        mockMvc.perform(get("/api/budgets").param("sort", "amount,asc").cookie(authCookie()))
                .andExpect(status().isOk());

        assertEquals(Sort.by(Sort.Order.asc("amount"), Sort.Order.desc("id")), capturedPageable().getSort());
    }

    @Test
    void getMyBudgets_SortById_StaysSingleKeyBecauseIdIsAlreadyUnique() throws Exception {
        mockMvc.perform(get("/api/budgets").param("sort", "id,asc").cookie(authCookie()))
                .andExpect(status().isOk());

        assertEquals(Sort.by(Sort.Order.asc("id")), capturedPageable().getSort());
    }

    @Test
    void getMyBudgets_UnknownSortField_Returns400WithSortFieldErrorAndNoQuery() throws Exception {
        mockMvc.perform(get("/api/budgets").param("sort", "date").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.sort").exists());

        verify(budgetRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyBudgets_SizeAboveHundred_Returns400WithUnprefixedSizeFieldError() throws Exception {
        mockMvc.perform(get("/api/budgets").param("size", "101").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.size").exists())
                .andExpect(jsonPath("$.fieldErrors['getMyBudgets.size']").doesNotExist());

        verify(budgetRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyBudgets_NegativePage_Returns400WithPageFieldError() throws Exception {
        mockMvc.perform(get("/api/budgets").param("page", "-1").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.page").exists());

        verify(budgetRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyBudgets_WithMonthAndCategoryFilters_IsAcceptedAndQueriesOncePerRequest() throws Exception {
        mockMvc.perform(get("/api/budgets")
                .param("month", "2026-01")
                .param("categoryId", String.valueOf(OWN_CATEGORY_ID))
                .cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(budgetRepository).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyBudgets_ForeignCategoryIdFilter_IsNotAnError() throws Exception {
        mockMvc.perform(get("/api/budgets")
                .param("categoryId", String.valueOf(FOREIGN_CATEGORY_ID))
                .cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getMyBudgets_MalformedMonth_Returns400WithMonthFieldError() throws Exception {
        mockMvc.perform(get("/api/budgets").param("month", "2026-01-01").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.month").exists());

        verify(budgetRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyBudgets_NonNumericCategoryId_Returns400WithCategoryIdFieldError() throws Exception {
        mockMvc.perform(get("/api/budgets").param("categoryId", "abc").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.categoryId").exists());

        verify(budgetRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyBudgets_WithoutAuthCookie_Returns401() throws Exception {
        mockMvc.perform(get("/api/budgets"))
                .andExpect(status().isUnauthorized());

        verify(budgetRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void updateBudget_WithForeignCategoryId_Returns404WithErrorResponseBody() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/budgets/{id}", OWN_BUDGET_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new CreateBudgetDto(new BigDecimal("250.00"), LocalDate.of(2026, 2, 1),
                        FOREIGN_CATEGORY_ID))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void createBudget_WithForeignCategoryId_Returns404() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(post("/api/budgets")
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new CreateBudgetDto(new BigDecimal("250.00"), LocalDate.of(2026, 2, 1),
                        FOREIGN_CATEGORY_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void updateBudget_AppliesEveryField_BecausePutIsAFullReplacement() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/budgets/{id}", OWN_BUDGET_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new CreateBudgetDto(new BigDecimal("250.55"), LocalDate.of(2026, 2, 1),
                        OTHER_OWN_CATEGORY_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(OWN_BUDGET_ID))
                .andExpect(jsonPath("$.amount").value(250.55))
                .andExpect(jsonPath("$.month").value("2026-02-01"))
                .andExpect(jsonPath("$.categoryId").value(OTHER_OWN_CATEGORY_ID))
                .andExpect(jsonPath("$.categoryName").value("travel"));

        ArgumentCaptor<Budget> saved = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).save(saved.capture());
        assertEquals(0, new BigDecimal("250.55").compareTo(saved.getValue().getAmount()));
        assertEquals(LocalDate.of(2026, 2, 1), saved.getValue().getMonth());
        assertEquals(OTHER_OWN_CATEGORY_ID, saved.getValue().getCategory().getId());
    }

    @Test
    void updateBudget_InvalidBody_Returns400WithFieldErrorsKeyedByJavaPropertyNames() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/budgets/{id}", OWN_BUDGET_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new CreateBudgetDto(null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.amount").exists())
                .andExpect(jsonPath("$.fieldErrors.month").exists())
                .andExpect(jsonPath("$.fieldErrors.categoryId").exists());

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void updateBudget_ForeignBudgetId_Returns404() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/budgets/{id}", FOREIGN_BUDGET_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new CreateBudgetDto(new BigDecimal("250.00"), LocalDate.of(2026, 2, 1),
                        OWN_CATEGORY_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void updateBudget_UnknownBudgetId_Returns404() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/budgets/{id}", UNKNOWN_BUDGET_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new CreateBudgetDto(new BigDecimal("250.00"), LocalDate.of(2026, 2, 1),
                        OWN_CATEGORY_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void updateBudget_WithoutCsrfToken_Returns403() throws Exception {
        mockMvc.perform(put("/api/budgets/{id}", OWN_BUDGET_ID)
                .cookie(authCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new CreateBudgetDto(new BigDecimal("250.00"), LocalDate.of(2026, 2, 1),
                        OWN_CATEGORY_ID))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void unexpectedFailure_Returns500_WithoutLeakingInternalDetail() throws Exception {
        when(budgetRepository.findAll(anySpecification(), any(Pageable.class))).thenThrow(new IllegalStateException(
                "ERROR: duplicate key value violates unique constraint \"uk_budgets_user_category\""));

        mockMvc.perform(get("/api/budgets").cookie(authCookie()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("uk_budgets_user_category"))));
    }

    @Test
    void updateBudget_WithoutAuthCookie_Returns401() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/budgets/{id}", OWN_BUDGET_ID)
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new CreateBudgetDto(new BigDecimal("250.00"), LocalDate.of(2026, 2, 1),
                        OWN_CATEGORY_ID))))
                .andExpect(status().isUnauthorized());

        verify(budgetRepository, never()).save(any(Budget.class));
    }
}
