package com.yassine.smartexpensetracker.category;

import com.yassine.smartexpensetracker.category.CategoryDtos.*;
import com.yassine.smartexpensetracker.user.User;
import com.yassine.smartexpensetracker.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(UUID userId) {
        return categoryRepository.findAllByUserIdOrderByNameAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(UUID userId, CreateCategoryRequest req) {
        String name = req.name().trim();

        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new IllegalArgumentException("Category name already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Category c = new Category();
        c.setUser(user);
        c.setName(name);
        c.setColor(req.color().trim());
        c.setIcon(req.icon().trim());
        c.setBudgetLimit(req.budgetLimit());

        categoryRepository.save(c);
        return toResponse(c);
    }


    @Transactional
    public CategoryResponse update(UUID userId, UUID categoryId, UpdateCategoryRequest req) {
        Category c = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        String newName = req.name().trim();

        if (!c.getName().equalsIgnoreCase(newName)
                && categoryRepository.existsByUserIdAndNameIgnoreCase(userId, newName)) {
            throw new IllegalArgumentException("Category name already exists");
        }

        c.setName(newName);
        c.setColor(req.color().trim());
        c.setIcon(req.icon().trim());
        c.setBudgetLimit(req.budgetLimit());

        return toResponse(c);
    }

    @Transactional
    public void delete(UUID userId, UUID categoryId) {
        Category c = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        // NOTE: plus tard, on gérera le cas "catégorie utilisée par des dépenses"
        categoryRepository.delete(c);
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getColor(),
                c.getIcon(),
                c.getBudgetLimit()
        );
    }
}
