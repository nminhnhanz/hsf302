package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.service.ArticleService;
import com.fpt.sb.hsfnews.service.CategoryService;
import com.fpt.sb.hsfnews.service.TagService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class BlogController {

    private final ArticleService articleService;
    private final CategoryService categoryService;
    private final TagService tagService;

    public BlogController(ArticleService articleService, CategoryService categoryService, TagService tagService) {
        this.articleService = articleService;
        this.categoryService = categoryService;
        this.tagService = tagService;
    }

    @GetMapping("/blogs")
    public String search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "tagIds", required = false) List<Long> tagIds,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
            Model model
    ) {
        model.addAttribute("allCategories", categoryService.findAllForHeader());
        // model.addAttribute("allTags", tagService.findAll());

        // Gọi hàm search bằng ID có sẵn
        model.addAttribute("page", articleService.searchPublished(q, categoryId, tagIds, page, size, sortDir));

        // --- TRẢ TÊN VỀ CHO VIEW ĐỂ HIỂN THỊ TRÊN GIAO DIỆN ---
        if (categoryId != null) {
            // Lưu ý: Bạn cần tạo hàm findById trong CategoryService
            categoryService.findById(categoryId).ifPresent(c -> model.addAttribute("selectedCategory", c));
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            // Lưu ý: Bạn cần tạo hàm findAllById trong TagService
            model.addAttribute("selectedTags", tagService.findAllById(tagIds));
        }

        // Giữ lại tham số để phân trang
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("tagIds", tagIds);
        model.addAttribute("size", size);
        model.addAttribute("sortDir", sortDir);

        return "blogs";
    }
}