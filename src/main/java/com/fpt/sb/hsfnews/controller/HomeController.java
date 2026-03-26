package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.service.ArticleService;
import com.fpt.sb.hsfnews.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ArticleService articleService;
    private final CategoryService categoryService;

    // Hợp nhất thành một Constructor duy nhất để thực hiện Dependency Injection
    public HomeController(ArticleService articleService, CategoryService categoryService) {
        this.articleService = articleService;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String index(Model model) {
        // Lấy 6 bài viết mới nhất đã xuất bản
        model.addAttribute("latestArticles", articleService.latestPublished6());

        // Lấy 4 chuyên mục có nhiều bài viết nhất (giả định Service có hàm này)
        model.addAttribute("topCategories", categoryService.topByArticleCount(4));

        return "index";
    }
}