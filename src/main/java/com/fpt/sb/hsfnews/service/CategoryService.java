package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.Category;
import com.fpt.sb.hsfnews.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAllForHeader() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }
}

