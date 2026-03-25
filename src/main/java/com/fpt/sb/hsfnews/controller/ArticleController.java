package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.entity.Article;
import com.fpt.sb.hsfnews.entity.User;
import com.fpt.sb.hsfnews.service.ArticleService;
import com.fpt.sb.hsfnews.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/admin/articles")
public class ArticleController {

    private final ArticleService articleService;
    private final CategoryService categoryService;

    public ArticleController(ArticleService articleService, CategoryService categoryService) {
        this.articleService = articleService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String listArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            @AuthenticationPrincipal User user) {
        Page<Article> articles = articleService.getArticlesByAuthor(user, page, size);
        model.addAttribute("articles", articles);
        return "admin/articles/list";
    }

    @GetMapping("/new")
    public String createArticleForm(Model model) {
        model.addAttribute("article", new Article());
        model.addAttribute("categories", categoryService.findAllForHeader());
        return "admin/articles/form";
    }

    @PostMapping("/new")
    public String createArticle(
            @Valid @ModelAttribute("article") Article article,
            BindingResult bindingResult,
            @RequestParam("thumbnailFile") MultipartFile thumbnailFile,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) throws IOException {
        
        if (bindingResult.hasErrors()) {
            return "admin/articles/form";
        }
        
        if (!thumbnailFile.isEmpty()) {
            String thumbnailPath = "/uploads/images/" + thumbnailFile.getOriginalFilename();
            article.setThumbnail(thumbnailPath);
        }
        
        Article savedArticle = articleService.createArticle(article, user);
        redirectAttributes.addFlashAttribute("message", "Article created successfully!");
        return "redirect:/admin/articles";
    }

    @GetMapping("/edit/{id}")
    public String editArticleForm(@PathVariable Long id, Model model, @AuthenticationPrincipal User user) {
        Article article = articleService.getArticleById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        if (!article.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        
        model.addAttribute("article", article);
        model.addAttribute("categories", categoryService.findAllForHeader());
        return "admin/articles/form";
    }

    @PostMapping("/edit/{id}")
    public String updateArticle(
            @PathVariable Long id,
            @Valid @ModelAttribute("article") Article article,
            BindingResult bindingResult,
            @RequestParam("thumbnailFile") MultipartFile thumbnailFile,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) throws IOException {
        
        if (bindingResult.hasErrors()) {
            return "admin/articles/form";
        }
        
        Article existingArticle = articleService.getArticleById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        if (!existingArticle.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        
        if (!thumbnailFile.isEmpty()) {
            String thumbnailPath = "/uploads/images/" + thumbnailFile.getOriginalFilename();
            article.setThumbnail(thumbnailPath);
        }
        
        articleService.updateArticle(id, article);
        redirectAttributes.addFlashAttribute("message", "Article updated successfully!");
        return "redirect:/admin/articles";
    }

    @PostMapping("/delete/{id}")
    public String deleteArticle(@PathVariable Long id, @AuthenticationPrincipal User user, RedirectAttributes redirectAttributes) {
        Article article = articleService.getArticleById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        if (!article.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        
        articleService.deleteArticle(id);
        redirectAttributes.addFlashAttribute("message", "Article deleted successfully!");
        return "redirect:/admin/articles";
    }

    @PostMapping("/publish/{id}")
    public String publishArticle(@PathVariable Long id, @AuthenticationPrincipal User user, RedirectAttributes redirectAttributes) {
        Article article = articleService.getArticleById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        if (!article.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        
        articleService.publishArticle(id);
        redirectAttributes.addFlashAttribute("message", "Article published successfully!");
        return "redirect:/admin/articles";
    }

    @PostMapping("/draft/{id}")
    public String setDraftArticle(@PathVariable Long id, @AuthenticationPrincipal User user, RedirectAttributes redirectAttributes) {
        Article article = articleService.getArticleById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        if (!article.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        
        articleService.setDraftArticle(id);
        redirectAttributes.addFlashAttribute("message", "Article set to draft!");
        return "redirect:/admin/articles";
    }
}



