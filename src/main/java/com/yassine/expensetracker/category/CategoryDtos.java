package com.yassine.expensetracker.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public class CategoryDtos {

    public record CategoryResponse(
            UUID id,
            String name,
            String color,
            String icon,
            BigDecimal budgetLimit
    ) {}

    public record CreateCategoryRequest(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Size(max = 16) String color,
            @NotBlank @Size(max = 50) String icon,
            BigDecimal budgetLimit
    ) {}

    public record UpdateCategoryRequest(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Size(max = 16) String color,
            @NotBlank @Size(max = 50) String icon,
            BigDecimal budgetLimit
    ) {}
}
