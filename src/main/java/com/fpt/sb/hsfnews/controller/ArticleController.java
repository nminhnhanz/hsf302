package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.service.ArticleService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/articles/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        var article = articleService.getPublishedDetail(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        model.addAttribute("article", article);
        return "article-detail";
    }

    /**
     * Convenience lookup: /articles?title=Exact+Title
     * Redirects to /articles/{id} if found (published only).
     */
    @GetMapping("/articles")
    public String byTitle(@RequestParam(name = "title") String title) {
        var article = articleService.getPublishedByTitle(title)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return "redirect:/articles/" + article.getId();
    }
}



