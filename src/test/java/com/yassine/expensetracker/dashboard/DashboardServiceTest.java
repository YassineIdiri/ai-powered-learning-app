package com.yassine.expensetracker.dashboard;

import com.yassine.expensetracker.common.dto.DashboardResponse;
import com.yassine.expensetracker.common.dto.MonthlySpendDto;
import com.yassine.expensetracker.common.dto.SummaryDto;
import com.yassine.expensetracker.common.dto.TopCategoryDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    DashboardRepository dashboardRepository;

    @InjectMocks
    DashboardService dashboardService;

    private static DashboardRepository.SummaryView summaryView(BigDecimal total, long count) {
        return new DashboardRepository.SummaryView() {
            @Override public BigDecimal getTotal() { return total; }
            @Override public long getCount() { return count; }
        };
    }

    private static DashboardRepository.TopCategoryView topCategoryView(UUID id, String name, BigDecimal total) {
        return new DashboardRepository.TopCategoryView() {
            @Override public UUID getCategoryId() { return id; }
            @Override public String getCategoryName() { return name; }
            @Override public BigDecimal getTotal() { return total; }
        };
    }

    private static DashboardRepository.MonthlySpendView monthlySpendView(String month, BigDecimal total) {
        return new DashboardRepository.MonthlySpendView() {
            @Override public String getMonth() { return month; }
            @Override public BigDecimal getTotal() { return total; }
        };
    }

    @Test
    void getDashboard_shouldMapRepositoryViewsToDtos() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 12, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);
        int top = 5;

        UUID foodId = UUID.randomUUID();
        UUID rentId = UUID.randomUUID();

        when(dashboardRepository.summary(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(summaryView(new BigDecimal("123.45"), 7));

        when(dashboardRepository.topCategories(eq(userId), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(List.of(
                        topCategoryView(foodId, "Food", new BigDecimal("80.00")),
                        topCategoryView(rentId, "Rent", new BigDecimal("43.45"))
                ));

        when(dashboardRepository.monthlySpend(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        monthlySpendView("2025-11", new BigDecimal("10.00")),
                        monthlySpendView("2025-12", new BigDecimal("113.45"))
                ));

        DashboardResponse res = dashboardService.getDashboard(userId, from, to, top);

        assertThat(res.summary()).isEqualTo(new SummaryDto(new BigDecimal("123.45"), 7));

        assertThat(res.topCategories()).containsExactly(
                new TopCategoryDto(foodId, "Food", new BigDecimal("80.00")),
                new TopCategoryDto(rentId, "Rent", new BigDecimal("43.45"))
        );

        assertThat(res.monthlySeries()).containsExactly(
                new MonthlySpendDto("2025-11", new BigDecimal("10.00")),
                new MonthlySpendDto("2025-12", new BigDecimal("113.45"))
        );
    }

    @Test
    void getDashboard_shouldPassTopAsPageSizeToRepository() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 12, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);
        int top = 12;

        when(dashboardRepository.summary(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(summaryView(BigDecimal.ZERO, 0));
        when(dashboardRepository.topCategories(eq(userId), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(dashboardRepository.monthlySpend(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        dashboardService.getDashboard(userId, from, to, top);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(dashboardRepository).topCategories(eq(userId), any(LocalDate.class), any(LocalDate.class), pageableCaptor.capture());

        Pageable p = pageableCaptor.getValue();
        assertThat(p.getPageNumber()).isEqualTo(0);
        assertThat(p.getPageSize()).isEqualTo(top);
    }

    @Test
    void getDashboard_shouldCallEachRepositoryMethodOnce() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2025, 12, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(dashboardRepository.summary(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(summaryView(BigDecimal.ZERO, 0));
        when(dashboardRepository.topCategories(eq(userId), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(dashboardRepository.monthlySpend(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        dashboardService.getDashboard(userId, from, to, 5);

        verify(dashboardRepository, times(1)).summary(eq(userId), any(LocalDate.class), any(LocalDate.class));
        verify(dashboardRepository, times(1)).topCategories(eq(userId), any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
        verify(dashboardRepository, times(1)).monthlySpend(eq(userId), any(LocalDate.class), any(LocalDate.class));
        verifyNoMoreInteractions(dashboardRepository);
    }
}
