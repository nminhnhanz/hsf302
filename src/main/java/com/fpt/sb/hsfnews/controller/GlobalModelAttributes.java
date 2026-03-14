package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.service.CategoryService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final CategoryService categoryService;

    public GlobalModelAttributes(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @ModelAttribute("categories")
    public Object categories() {
        return categoryService.findAllForHeader();
    }
}