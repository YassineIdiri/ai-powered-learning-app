package com.yassine.expensetracker.expense;

import com.yassine.expensetracker.category.Category;
import com.yassine.expensetracker.category.CategoryRepository;
import com.yassine.expensetracker.common.PageResponse;
import com.yassine.expensetracker.expense.ExpenseDtos.*;
import com.yassine.expensetracker.user.User;
import com.yassine.expensetracker.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock ExpenseRepository expenseRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ExpenseService expenseService;

    private static User user(UUID id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPasswordHash("x");
        return u;
    }

    private static Category category(UUID id, User owner, String name) {
        Category c = new Category();
        c.setId(id);
        c.setUser(owner);
        c.setName(name);
        return c;
    }

    private static Expense expense(UUID id, User owner, Category cat, BigDecimal amount, LocalDate date) {
        Expense e = new Expense();
        e.setId(id);
        e.setUser(owner);
        e.setCategory(cat);
        e.setAmount(amount);
        e.setExpenseDate(date);
        e.setMerchant("Carrefour");
        e.setNote("note");
        return e;
    }

    @Test
    void list_shouldFetchBetweenDates_andMapToResponses() {
        UUID userId = UUID.randomUUID();
        User u = user(userId, "it@test.com");

        LocalDate from = LocalDate.of(2025, 12, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        Category cat = category(UUID.randomUUID(), u, "Food");

        Expense e1 = expense(UUID.randomUUID(), u, cat, new BigDecimal("10.50"), LocalDate.of(2025, 12, 10));
        Expense e2 = expense(UUID.randomUUID(), u, cat, new BigDecimal("5.00"), LocalDate.of(2025, 12, 5));

        when(expenseRepository.findByUserIdAndExpenseDateBetweenOrderByExpenseDateDesc(userId, from, to))
                .thenReturn(List.of(e1, e2));

        List<ExpenseResponse> res = expenseService.list(userId, from, to);

        assertThat(res).hasSize(2);
        ExpenseResponse r1 = res.getFirst();

        assertThat(r1.id()).isEqualTo(e1.getId());
        assertThat(r1.amount()).isEqualByComparingTo("10.50");
        assertThat(r1.currency()).isEqualTo("EUR");
        assertThat(r1.expenseDate()).isEqualTo(LocalDate.of(2025, 12, 10));
        assertThat(r1.categoryId()).isEqualTo(cat.getId());
        assertThat(r1.categoryName()).isEqualTo("Food");

        verify(expenseRepository).findByUserIdAndExpenseDateBetweenOrderByExpenseDateDesc(userId, from, to);
        verifyNoMoreInteractions(expenseRepository, categoryRepository, userRepository);
    }

    @Test
    void create_shouldThrow_whenCategoryNotFoundForUser() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        CreateExpenseRequest req = new CreateExpenseRequest(
                new BigDecimal("12.34"),
                LocalDate.of(2025, 12, 20),
                categoryId,
                "Shop",
                "Note"
        );

        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.create(userId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");

        verify(categoryRepository).findByIdAndUserId(categoryId, userId);
        verifyNoMoreInteractions(expenseRepository, categoryRepository, userRepository);
    }

    @Test
    void create_shouldThrow_whenUserNotFound() {
        UUID userId = UUID.randomUUID();
        User u = user(userId, "it@test.com");
        UUID categoryId = UUID.randomUUID();
        Category cat = category(categoryId, u, "Food");

        CreateExpenseRequest req = new CreateExpenseRequest(
                new BigDecimal("12.34"),
                LocalDate.of(2025, 12, 20),
                categoryId,
                "Shop",
                "Note"
        );

        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(cat));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.create(userId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Authenticated user not found");

        verify(categoryRepository).findByIdAndUserId(categoryId, userId);
        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(expenseRepository, categoryRepository, userRepository);
    }

    @Test
    void create_shouldSaveExpense_andReturnMappedResponse() {
        UUID userId = UUID.randomUUID();
        User u = user(userId, "it@test.com");

        UUID categoryId = UUID.randomUUID();
        Category cat = category(categoryId, u, "Food");

        CreateExpenseRequest req = new CreateExpenseRequest(
                new BigDecimal("12.34"),
                LocalDate.of(2025, 12, 20),
                categoryId,
                "Shop",
                "Note"
        );

        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(cat));
        when(userRepository.findById(userId)).thenReturn(Optional.of(u));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);

        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
            Expense saved = inv.getArgument(0);
            if (saved.getId() == null) saved.setId(UUID.randomUUID());
            return saved;
        });

        ExpenseResponse res = expenseService.create(userId, req);

        verify(expenseRepository).save(captor.capture());
        Expense saved = captor.getValue();

        assertThat(saved.getUser()).isEqualTo(u);
        assertThat(saved.getCategory()).isEqualTo(cat);
        assertThat(saved.getAmount()).isEqualByComparingTo("12.34");
        assertThat(saved.getExpenseDate()).isEqualTo(LocalDate.of(2025, 12, 20));
        assertThat(saved.getMerchant()).isEqualTo("Shop");
        assertThat(saved.getNote()).isEqualTo("Note");
        assertThat(saved.getCurrency()).isEqualTo("EUR");

        assertThat(res.id()).isNotNull();
        assertThat(res.amount()).isEqualByComparingTo("12.34");
        assertThat(res.currency()).isEqualTo("EUR");
        assertThat(res.categoryId()).isEqualTo(categoryId);
        assertThat(res.categoryName()).isEqualTo("Food");

        verify(categoryRepository).findByIdAndUserId(categoryId, userId);
        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(expenseRepository, categoryRepository, userRepository);
    }

    @Test
    void update_shouldThrow_whenExpenseNotFoundOrNotOwned() {
        UUID userId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();

        UpdateExpenseRequest req = new UpdateExpenseRequest(
                new BigDecimal("9.99"),
                LocalDate.of(2025, 12, 2),
                UUID.randomUUID(),
                "Shop",
                "Note"
        );

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.update(userId, expenseId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expense not found");

        verify(expenseRepository).findById(expenseId);
        verifyNoMoreInteractions(expenseRepository, categoryRepository, userRepository);
    }

    @Test
    void update_shouldThrow_whenCategoryNotFoundForUser() {
        UUID userId = UUID.randomUUID();
        User u = user(userId, "it@test.com");

        UUID expenseId = UUID.randomUUID();
        Category oldCat = category(UUID.randomUUID(), u, "Old");
        Expense existing = expense(expenseId, u, oldCat, new BigDecimal("1.00"), LocalDate.of(2025, 12, 1));

        UUID newCategoryId = UUID.randomUUID();

        UpdateExpenseRequest req = new UpdateExpenseRequest(
                new BigDecimal("9.99"),
                LocalDate.of(2025, 12, 2),
                newCategoryId,
                "Shop",
                "Note"
        );

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserId(newCategoryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.update(userId, expenseId, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");

        verify(expenseRepository).findById(expenseId);
        verify(categoryRepository).findByIdAndUserId(newCategoryId, userId);
        verifyNoMoreInteractions(expenseRepository, categoryRepository, userRepository);
    }

    @Test
    void update_shouldThrow_whenCategoryNotFoundForUser2() {

    }

    @Test
    void update_shouldMutateEntity_andReturnMappedResponse() {
        UUID userId = UUID.randomUUID();
        User u = user(userId, "it@test.com");

        UUID expenseId = UUID.randomUUID();
        Category oldCat = category(UUID.randomUUID(), u, "Old");
        Expense existing = expense(expenseId, u, oldCat, new BigDecimal("1.00"), LocalDate.of(2025, 12, 1));

        UUID newCategoryId = UUID.randomUUID();
        Category newCat = category(newCategoryId, u, "Food");

        UpdateExpenseRequest req = new UpdateExpenseRequest(
                new BigDecimal("9.99"),
                LocalDate.of(2025, 12, 2),
                newCategoryId,
                "New Shop",
                "New Note"
        );

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserId(newCategoryId, userId)).thenReturn(Optional.of(newCat));

        ExpenseResponse res = expenseService.update(userId, expenseId, req);

        assertThat(existing.getCategory()).isEqualTo(newCat);
        assertThat(existing.getAmount()).isEqualByComparingTo("9.99");
        assertThat(existing.getExpenseDate()).isEqualTo(LocalDate.of(2025, 12, 2));
        assertThat(existing.getMerchant()).isEqualTo("New Shop");
        assertThat(existing.getNote()).isEqualTo("New Note");
        assertThat(existing.getCurrency()).isEqualTo("EUR"); // inchangé

        assertThat(res.id()).isEqualTo(expenseId);
        assertThat(res.categoryId()).isEqualTo(newCategoryId);
        assertThat(res.categoryName()).isEqualTo("Food");
        assertThat(res.amount()).isEqualByComparingTo("9.99");
        assertThat(res.currency()).isEqualTo("EUR");

        verify(expenseRepository).findById(expenseId);
        verify(categoryRepository).findByIdAndUserId(newCategoryId, userId);
        verifyNoMoreInteractions(expenseRepository, categoryRepository, userRepository);
    }

    @Test
    void delete_shouldThrow_whenExpenseNotFoundOrNotOwned() {
        UUID userId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.delete(userId, expenseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expense not found");

        verify(expenseRepository).findById(expenseId);
        verifyNoMoreInteractions(expenseRepository, categoryRepository, userRepository);
    }

    @Test
    void delete_shouldDelete_whenOwnedByUser() {
        UUID userId = UUID.randomUUID();
        User u = user(userId, "it@test.com");

        UUID expenseId = UUID.randomUUID();
        Category cat = category(UUID.randomUUID(), u, "Food");
        Expense existing = expense(expenseId, u, cat, new BigDecimal("1.00"), LocalDate.of(2025, 12, 1));

        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(existing));

        expenseService.delete(userId, expenseId);

        verify(expenseRepository).findById(expenseId);
        verify(expenseRepository).delete(existing);
        verifyNoMoreInteractions(expenseRepository, categoryRepository, userRepository);
    }

    @Test
    void search_shouldBuildLikePattern_whenQProvided_andMapPageResponse() {
        UUID userId = UUID.randomUUID();
        User u = user(userId, "it@test.com");

        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        UUID catId = UUID.randomUUID();
        Category cat = category(catId, u, "Food");

        Expense e = expense(UUID.randomUUID(), u, cat, new BigDecimal("12.00"), LocalDate.of(2025, 5, 10));

        Pageable pageable = PageRequest.of(1, 10);
        Page<Expense> page = new PageImpl<>(List.of(e), pageable, 21);

        ArgumentCaptor<String> qCaptor = ArgumentCaptor.forClass(String.class);

        when(expenseRepository.search(
                eq(userId), eq(from), eq(to), eq(catId),
                isNull(), isNull(),
                anyString(),
                eq(pageable)
        )).thenReturn(page);

        PageResponse<ExpenseResponse> res = expenseService.search(
                userId, from, to, catId,
                null, null,
                "  CaRRe  ",
                pageable
        );

        verify(expenseRepository).search(
                eq(userId), eq(from), eq(to), eq(catId),
                isNull(), isNull(),
                qCaptor.capture(),
                eq(pageable)
        );

        assertThat(qCaptor.getValue()).isEqualTo("%carre%");

        assertThat(res.page()).isEqualTo(1);
        assertThat(res.size()).isEqualTo(10);
        assertThat(res.totalItems()).isEqualTo(21);
        assertThat(res.totalPages()).isEqualTo(3);

        assertThat(res.items()).hasSize(1);
        assertThat(res.items().get(0).categoryName()).isEqualTo("Food");
    }

    @Test
    void search_shouldPassNullPattern_whenQBlank() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        Pageable pageable = PageRequest.of(0, 20);
        Page<Expense> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(expenseRepository.search(
                eq(userId), eq(from), eq(to),
                isNull(), isNull(), isNull(),
                isNull(),
                eq(pageable)
        )).thenReturn(emptyPage);

        PageResponse<ExpenseResponse> res = expenseService.search(
                userId, from, to,
                null, null, null,
                "   ",
                pageable
        );

        verify(expenseRepository).search(
                eq(userId), eq(from), eq(to),
                isNull(), isNull(), isNull(),
                isNull(),
                eq(pageable)
        );

        assertThat(res.items()).isEmpty();
        assertThat(res.totalItems()).isZero();
    }

    @Test
    void summary_shouldMapProjectionToDto() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 12, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        var view = mock(ExpenseDtos.ExpenseSummaryView.class);
        when(view.getTotalAmount()).thenReturn(new BigDecimal("99.99"));
        when(view.getTotalCount()).thenReturn(3L);

        when(expenseRepository.summary(userId, from, to)).thenReturn(view);

        ExpenseSummaryResponse res = expenseService.summary(userId, from, to);

        assertThat(res.totalAmount()).isEqualByComparingTo("99.99");
        assertThat(res.totalCount()).isEqualTo(3L);

        verify(expenseRepository).summary(userId, from, to);
        verifyNoMoreInteractions(expenseRepository, categoryRepository, userRepository);
    }
}
