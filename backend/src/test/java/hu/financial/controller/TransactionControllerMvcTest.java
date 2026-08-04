package hu.financial.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.financial.config.SecurityConfig;
import hu.financial.dto.transaction.CreateTransactionDto;
import hu.financial.filter.JwtAuthenticationFilter;
import hu.financial.model.Category;
import hu.financial.model.Transaction;
import hu.financial.model.User;
import hu.financial.model.enums.TransactionType;
import hu.financial.repository.CategoryRepository;
import hu.financial.repository.TransactionRepository;
import hu.financial.security.CookieProperties;
import hu.financial.security.CsrfCookieFilter;
import hu.financial.security.RestAccessDeniedHandler;
import hu.financial.security.SecurityCookieFactory;
import hu.financial.service.CategoryService;
import hu.financial.service.JwtService;
import hu.financial.service.TransactionService;
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

@WebMvcTest(TransactionController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, CookieProperties.class, RestAccessDeniedHandler.class,
        SecurityCookieFactory.class, CsrfCookieFilter.class, JwtService.class, TransactionService.class,
        CategoryService.class })
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "security.jwt.expiration-time=3600",
        "security.cookie.secure=false",
        "security.cookie.same-site=Lax"
})
class TransactionControllerMvcTest {

    private static final Long OWN_TRANSACTION_ID = 5L;
    private static final Long FOREIGN_TRANSACTION_ID = 6L;
    private static final Long UNKNOWN_TRANSACTION_ID = 404L;
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
    private TransactionRepository transactionRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    private User currentUser;

    private Transaction ownTransaction;

    @BeforeEach
    void setUp() {
        currentUser = new User("testuser", "encoded-password", "test@example.com");
        currentUser.setId(1L);
        User otherUser = new User("other", "encoded-password", "other@example.com");
        otherUser.setId(2L);

        Category ownCategory = new Category("groceries", "food and household", currentUser);
        ownCategory.setId(OWN_CATEGORY_ID);
        Category otherOwnCategory = new Category("salary", "monthly income", currentUser);
        otherOwnCategory.setId(OTHER_OWN_CATEGORY_ID);
        Category foreignCategory = new Category("foreign", "not yours", otherUser);
        foreignCategory.setId(FOREIGN_CATEGORY_ID);

        ownTransaction = new Transaction(OWN_TRANSACTION_ID, TransactionType.EXPENSE, "weekly shopping",
                ownCategory, currentUser, new BigDecimal("100.00"), LocalDate.of(2026, 1, 1));
        Transaction foreignTransaction = new Transaction(FOREIGN_TRANSACTION_ID, TransactionType.EXPENSE, "not yours",
                foreignCategory, otherUser, new BigDecimal("50.00"), LocalDate.of(2026, 1, 1));

        when(userService.loadUserByUsername("testuser")).thenReturn(currentUser);
        when(userService.getCurrentUser()).thenReturn(currentUser);

        when(categoryRepository.findById(OWN_CATEGORY_ID)).thenReturn(Optional.of(ownCategory));
        when(categoryRepository.findById(OTHER_OWN_CATEGORY_ID)).thenReturn(Optional.of(otherOwnCategory));
        when(categoryRepository.findById(FOREIGN_CATEGORY_ID)).thenReturn(Optional.of(foreignCategory));

        when(transactionRepository.findById(OWN_TRANSACTION_ID)).thenReturn(Optional.of(ownTransaction));
        when(transactionRepository.findById(FOREIGN_TRANSACTION_ID)).thenReturn(Optional.of(foreignTransaction));
        when(transactionRepository.findById(UNKNOWN_TRANSACTION_ID)).thenReturn(Optional.empty());
        when(transactionRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<Transaction>(List.of(), invocation.getArgument(1), 0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Cookie authCookie() {
        return new Cookie("authToken", jwtService.generateToken(currentUser));
    }

    private Cookie csrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/transactions").cookie(authCookie()))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie, "XSRF-TOKEN cookie must be issued so the SPA can echo it back");
        return cookie;
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private CreateTransactionDto validUpdate() {
        return new CreateTransactionDto(TransactionType.INCOME, "monthly salary", OTHER_OWN_CATEGORY_ID,
                new BigDecimal("250.55"), LocalDate.of(2026, 2, 1));
    }

    @Test
    void updateTransaction_WithForeignCategoryId_Returns404WithErrorResponseBody() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/transactions/{id}", OWN_TRANSACTION_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new CreateTransactionDto(TransactionType.EXPENSE, "stolen", FOREIGN_CATEGORY_ID,
                        new BigDecimal("250.00"), LocalDate.of(2026, 2, 1)))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_WithForeignCategoryId_Returns404() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(post("/api/transactions")
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new CreateTransactionDto(TransactionType.EXPENSE, "stolen", FOREIGN_CATEGORY_ID,
                        new BigDecimal("250.00"), LocalDate.of(2026, 2, 1)))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_AppliesEveryField_BecausePutIsAFullReplacement() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/transactions/{id}", OWN_TRANSACTION_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(validUpdate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(OWN_TRANSACTION_ID))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.description").value("monthly salary"))
                .andExpect(jsonPath("$.categoryId").value(OTHER_OWN_CATEGORY_ID))
                .andExpect(jsonPath("$.categoryName").value("salary"))
                .andExpect(jsonPath("$.amount").value(250.55))
                .andExpect(jsonPath("$.date").value("2026-02-01"));

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(saved.capture());
        assertEquals(TransactionType.INCOME, saved.getValue().getType());
        assertEquals("monthly salary", saved.getValue().getDescription());
        assertEquals(OTHER_OWN_CATEGORY_ID, saved.getValue().getCategory().getId());
        assertEquals(0, new BigDecimal("250.55").compareTo(saved.getValue().getAmount()));
        assertEquals(LocalDate.of(2026, 2, 1), saved.getValue().getDate());
    }

    @Test
    void updateTransaction_InvalidBody_Returns400WithFieldErrorsKeyedByJavaPropertyNames() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/transactions/{id}", OWN_TRANSACTION_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new CreateTransactionDto(null, "no type", null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.type").exists())
                .andExpect(jsonPath("$.fieldErrors.categoryId").exists())
                .andExpect(jsonPath("$.fieldErrors.amount").exists())
                .andExpect(jsonPath("$.fieldErrors.date").exists());

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_ForeignTransactionId_Returns404() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/transactions/{id}", FOREIGN_TRANSACTION_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(validUpdate())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_UnknownTransactionId_Returns404() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/transactions/{id}", UNKNOWN_TRANSACTION_ID)
                .cookie(authCookie(), csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(validUpdate())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_WithoutCsrfToken_Returns403() throws Exception {
        mockMvc.perform(put("/api/transactions/{id}", OWN_TRANSACTION_ID)
                .cookie(authCookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(validUpdate())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    private static Specification<Transaction> anySpecification() {
        return ArgumentMatchers.any();
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findAll(anySpecification(), pageable.capture());
        return pageable.getValue();
    }

    @Test
    void getMyTransactions_ReturnsOwnPageWrapper_NotABareArray() throws Exception {
        when(transactionRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(ownTransaction), invocation.getArgument(1), 1));

        mockMvc.perform(get("/api/transactions").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(OWN_TRANSACTION_ID))
                .andExpect(jsonPath("$.content[0].categoryId").value(OWN_CATEGORY_ID))
                .andExpect(jsonPath("$.content[0].categoryName").value("groceries"))
                .andExpect(jsonPath("$.content[0].amount").value(100.00))
                .andExpect(jsonPath("$.content[0].date").value("2026-01-01"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getMyTransactions_WithoutParameters_UsesPageZeroSizeTwentyAndDateDescending() throws Exception {
        mockMvc.perform(get("/api/transactions").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0));

        Pageable pageable = capturedPageable();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id")), pageable.getSort());
    }

    @Test
    void getMyTransactions_EmptyPage_HasEmptyContentArrayNotNull() throws Exception {
        mockMvc.perform(get("/api/transactions").param("page", "3").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void getMyTransactions_MiddlePage_ReportsTotalsAndBoundaryFlags() throws Exception {
        when(transactionRepository.findAll(anySpecification(), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(ownTransaction), invocation.getArgument(1), 137));

        mockMvc.perform(get("/api/transactions")
                .param("page", "1")
                .param("size", "20")
                .cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(137))
                .andExpect(jsonPath("$.totalPages").value(7))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));

        assertEquals(1, capturedPageable().getPageNumber());
    }

    @Test
    void getMyTransactions_SortByAmount_AlwaysAppendsIdDescendingSoPagesDoNotOverlap() throws Exception {
        mockMvc.perform(get("/api/transactions").param("sort", "amount,asc").cookie(authCookie()))
                .andExpect(status().isOk());

        assertEquals(Sort.by(Sort.Order.asc("amount"), Sort.Order.desc("id")), capturedPageable().getSort());
    }

    @Test
    void getMyTransactions_SortById_StaysSingleKeyBecauseIdIsAlreadyUnique() throws Exception {
        mockMvc.perform(get("/api/transactions").param("sort", "id,asc").cookie(authCookie()))
                .andExpect(status().isOk());

        assertEquals(Sort.by(Sort.Order.asc("id")), capturedPageable().getSort());
    }

    @Test
    void getMyTransactions_UnknownSortField_Returns400WithSortFieldErrorAndNoQuery() throws Exception {
        mockMvc.perform(get("/api/transactions").param("sort", "user.password").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.sort").exists());

        verify(transactionRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyTransactions_UnknownSortDirection_Returns400WithSortFieldError() throws Exception {
        mockMvc.perform(get("/api/transactions").param("sort", "date,sideways").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.sort").exists());

        verify(transactionRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyTransactions_SizeAboveHundred_Returns400WithUnprefixedSizeFieldError() throws Exception {
        mockMvc.perform(get("/api/transactions").param("size", "101").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.size").exists())
                .andExpect(jsonPath("$.fieldErrors['getMyTransactions.size']").doesNotExist());

        verify(transactionRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyTransactions_SizeBelowOne_Returns400WithSizeFieldError() throws Exception {
        mockMvc.perform(get("/api/transactions").param("size", "0").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.size").exists());

        verify(transactionRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyTransactions_NegativePage_Returns400WithPageFieldError() throws Exception {
        mockMvc.perform(get("/api/transactions").param("page", "-1").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.page").exists())
                .andExpect(jsonPath("$.fieldErrors['getMyTransactions.page']").doesNotExist());

        verify(transactionRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyTransactions_SizeAtHundred_IsAccepted() throws Exception {
        mockMvc.perform(get("/api/transactions").param("size", "100").cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));

        assertEquals(100, capturedPageable().getPageSize());
    }

    @Test
    void getMyTransactions_IgnoresUserIdParameter_BecauseTheScopeComesFromTheSession() throws Exception {
        mockMvc.perform(get("/api/transactions").param("userId", "2").cookie(authCookie()))
                .andExpect(status().isOk());

        verify(userService).getCurrentUser();
        verify(transactionRepository).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyTransactions_WithEveryFilter_IsAcceptedAndStillQueriesOncePerRequest() throws Exception {
        mockMvc.perform(get("/api/transactions")
                .param("from", "2026-01-01")
                .param("to", "2026-01-31")
                .param("categoryId", String.valueOf(OWN_CATEGORY_ID))
                .param("type", "EXPENSE")
                .cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(transactionRepository).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyTransactions_ForeignCategoryIdFilter_IsNotAnError() throws Exception {
        mockMvc.perform(get("/api/transactions")
                .param("categoryId", String.valueOf(FOREIGN_CATEGORY_ID))
                .cookie(authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getMyTransactions_FromAfterTo_Returns400WithFromFieldError() throws Exception {
        mockMvc.perform(get("/api/transactions")
                .param("from", "2026-02-01")
                .param("to", "2026-01-31")
                .cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.from").exists());

        verify(transactionRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyTransactions_EqualFromAndTo_IsAcceptedBecauseTheBoundsAreInclusive() throws Exception {
        mockMvc.perform(get("/api/transactions")
                .param("from", "2026-01-01")
                .param("to", "2026-01-01")
                .cookie(authCookie()))
                .andExpect(status().isOk());

        verify(transactionRepository).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyTransactions_MalformedFromDate_Returns400WithFromFieldError() throws Exception {
        mockMvc.perform(get("/api/transactions").param("from", "01-01-2026").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.from").exists());

        verify(transactionRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyTransactions_UnknownType_Returns400WithTypeFieldError() throws Exception {
        mockMvc.perform(get("/api/transactions").param("type", "REFUND").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.type").exists());

        verify(transactionRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyTransactions_NonNumericCategoryId_Returns400WithCategoryIdFieldError() throws Exception {
        mockMvc.perform(get("/api/transactions").param("categoryId", "abc").cookie(authCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.categoryId").exists());

        verify(transactionRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void getMyTransactions_WithoutAuthCookie_Returns401() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());

        verify(transactionRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void updateTransaction_WithoutAuthCookie_Returns401() throws Exception {
        Cookie csrf = csrfCookie();

        mockMvc.perform(put("/api/transactions/{id}", OWN_TRANSACTION_ID)
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(validUpdate())))
                .andExpect(status().isUnauthorized());

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getMyTransactions_UnknownSortField_RejectsBeforeResolvingTheCurrentUser() throws Exception {
        mockMvc.perform(get("/api/transactions").param("sort", "user.password").cookie(authCookie()))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getCurrentUser();
    }

    @Test
    void getMyTransactions_SizeAboveHundred_RejectsBeforeResolvingTheCurrentUser() throws Exception {
        mockMvc.perform(get("/api/transactions").param("size", "101").cookie(authCookie()))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getCurrentUser();
    }

    @Test
    void getMyTransactions_NegativePage_RejectsBeforeResolvingTheCurrentUser() throws Exception {
        mockMvc.perform(get("/api/transactions").param("page", "-1").cookie(authCookie()))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getCurrentUser();
    }
}
