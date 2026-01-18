package com.yassine.expensetracker.common.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopCategoryDto(
        UUID categoryId,
        String categoryName,
        BigDecimal total
) {}
