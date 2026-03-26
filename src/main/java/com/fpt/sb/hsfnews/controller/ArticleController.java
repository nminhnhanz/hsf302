package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.entity.Article;
import com.fpt.sb.hsfnews.entity.ArticleStatus;
import com.fpt.sb.hsfnews.entity.Tag;
import com.fpt.sb.hsfnews.entity.User;
import com.fpt.sb.hsfnews.repository.UserRepository;
import com.fpt.sb.hsfnews.service.ArticleService;
import com.fpt.sb.hsfnews.service.CategoryService;
import com.fpt.sb.hsfnews.service.TagService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/admin/articles")
public class ArticleController {

    private final ArticleService articleService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final UserRepository userRepository;

    private static final Pattern BASE64_IMAGE_PATTERN = Pattern.compile("data:image/[^;]+;base64,([A-Za-z0-9+/=\\r\\n]+)");

    public ArticleController(ArticleService articleService,
                             CategoryService categoryService,
                             TagService tagService,
                             UserRepository userRepository) {
        this.articleService = articleService;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.userRepository = userRepository;
    }

    // --- DANH SÁCH BÀI VIẾT ---
    @GetMapping
    public String listArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "tagIds", required = false) String tagIdsRaw,
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        User user = getCurrentUser(principal);
        ArticleStatus filterStatus = parseStatus(status);
        List<Long> tagIds = parseTagIds(tagIdsRaw);

        Page<Article> articles = articleService.getArticlesByAuthor(user, q, filterStatus, categoryId, tagIds, page, size);

        model.addAttribute("articles", articles);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", filterStatus == null ? "" : filterStatus.name());
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("tagIds", tagIds);
        model.addAttribute("categories", categoryService.findAll());
        return "admin/articles/list";
    }

    // --- TẠO MỚI BÀI VIẾT ---
    @GetMapping("/new")
    public String createArticleForm(Model model) {
        model.addAttribute("article", new Article());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/articles/form";
    }

    @PostMapping("/new")
    public String saveArticle(@Valid @ModelAttribute Article article,
                              BindingResult bindingResult,
                              @RequestParam("thumbnailFile") MultipartFile thumbnailFile,
                              @RequestParam(name = "action", defaultValue = "draft") String action,
                              @AuthenticationPrincipal UserDetails principal,
                              RedirectAttributes redirectAttributes) throws IOException {

        User user = getCurrentUser(principal);

        if (bindingResult.hasErrors()) return "admin/articles/form";

        if (!thumbnailFile.isEmpty()) {
            article.setThumbnail(toBase64DataUrl(thumbnailFile));
        }

        article.setStatus(resolveStatusFromAction(action));
        articleService.createArticle(article, user);

        redirectAttributes.addFlashAttribute("message", "Article created successfully!");
        return "redirect:/admin/articles";
    }

    // --- CHỈNH SỬA BÀI VIẾT ---
    @GetMapping("/edit/{id}")
    public String editArticleForm(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails principal) {
        User user = getCurrentUser(principal);
        Article article = articleService.getArticleById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!article.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        model.addAttribute("article", article);
        model.addAttribute("categories", categoryService.findAll());
        return "admin/articles/form";
    }

    // --- CÁC THAO TÁC TRẠNG THÁI ---
    @PostMapping("/publish/{id}")
    public String publishArticle(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, RedirectAttributes redirectAttributes) {
        validateOwner(id, principal);
        articleService.publishArticle(id);
        redirectAttributes.addFlashAttribute("message", "Article published!");
        return "redirect:/admin/articles";
    }

    @PostMapping("/draft/{id}")
    public String setDraftArticle(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, RedirectAttributes redirectAttributes) {
        validateOwner(id, principal);
        articleService.setDraftArticle(id);
        redirectAttributes.addFlashAttribute("message", "Moved to draft!");
        return "redirect:/admin/articles";
    }

    @PostMapping("/delete/{id}")
    public String deleteArticle(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, RedirectAttributes redirectAttributes) {
        validateOwner(id, principal);
        articleService.deleteArticle(id);
        redirectAttributes.addFlashAttribute("message", "Article deleted!");
        return "redirect:/admin/articles";
    }

    // --- PHƯƠNG THỨC BỔ TRỢ (HELPERS) ---

    private User getCurrentUser(UserDetails principal) {
        return userRepository.findByUsernameIgnoreCase(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private void validateOwner(Long articleId, UserDetails principal) {
        User user = getCurrentUser(principal);
        Article article = articleService.getArticleById(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!article.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private String toBase64DataUrl(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String encoded = Base64.getEncoder().encodeToString(bytes);
        return "data:" + file.getContentType() + ";base64," + encoded;
    }

    private ArticleStatus resolveStatusFromAction(String action) {
        return "publish".equalsIgnoreCase(action) ? ArticleStatus.PUBLISHED : ArticleStatus.DRAFT;
    }

    private ArticleStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try { return ArticleStatus.valueOf(status.trim().toUpperCase()); }
        catch (Exception e) { return null; }
    }

    private List<Long> parseTagIds(String raw) {
        List<Long> ids = new ArrayList<>();
        if (raw == null || raw.isBlank()) return ids;
        for (String idStr : raw.split(",")) {
            try { ids.add(Long.parseLong(idStr.trim())); } catch (Exception ignored) {}
        }
        return ids;
    }
}