package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.Category;
import com.fpt.sb.hsfnews.repository.ArticleRepository;
import com.fpt.sb.hsfnews.repository.CategoryRepository; // Bổ sung import bị thiếu
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;

    // Hợp nhất thành một Constructor duy nhất để thực hiện Dependency Injection
    public CategoryService(CategoryRepository categoryRepository, ArticleRepository articleRepository) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
    }

    /**
     * Lấy danh sách Category hiển thị trên Header (thường sắp xếp theo tên hoặc ID)
     */
    public List<Category> findAllForHeader() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public List<Category> findAll() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    public List<Category> topByArticleCount(int limit) {
        int safeLimit = Math.max(limit, 1);
        return categoryRepository.findTopByArticleCount(PageRequest.of(0, safeLimit));
    }

    public Page<Category> searchByNamePaged(String q, int page, int size) {
        String keyword = q == null ? "" : q.trim();
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : size;
        return categoryRepository.findByNameContainingIgnoreCase(keyword, PageRequest.of(safePage, safeSize));
    }

    @Transactional
    public Category create(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        return create(category);
    }

    @Transactional
    public Category create(Category category) {
        if (categoryRepository.findByNameIgnoreCase(category.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name already exists");
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, Category updatedCategory) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        categoryRepository.findByNameIgnoreCase(updatedCategory.getName())
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name already exists");
                });

        existing.setName(updatedCategory.getName());
        existing.setDescription(updatedCategory.getDescription());
        return categoryRepository.save(existing);
    }

    @Transactional
    public Category update(Long id, String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        return update(id, category);
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        // Kiểm tra nếu có bài viết thuộc Category này thì xử lý (tùy logic: xóa hoặc báo lỗi)
        categoryRepository.deleteById(id);
    }
}