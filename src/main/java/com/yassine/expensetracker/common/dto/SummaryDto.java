package com.yassine.expensetracker.common.dto;
import java.math.BigDecimal;

public record SummaryDto(
        BigDecimal total,
        long count
) {}
