package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "size", defaultValue = "10") int size,
                       Model model) {
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("categories", categoryService.searchByNamePaged(q, page, size));
        model.addAttribute("size", size);
        return "admin/categories/list";
    }

    @PostMapping
    public String create(@RequestParam("name") String name,
                         @RequestParam(name = "description", required = false) String description,
                         @RequestParam(name = "q", required = false) String q,
                         @RequestParam(name = "page", defaultValue = "0") int page,
                         @RequestParam(name = "size", defaultValue = "10") int size,
                         Model model,
                         RedirectAttributes ra) {
        try {
            categoryService.create(name, description);
            ra.addFlashAttribute("message", "Category created successfully");
            return "redirect:/admin/categories";
        } catch (ResponseStatusException ex) {
            model.addAttribute("error", ex.getReason() == null ? "Cannot create category" : ex.getReason());
            model.addAttribute("q", q == null ? "" : q);
            model.addAttribute("categories", categoryService.searchByNamePaged(q, page, size));
            model.addAttribute("size", size);
            return "admin/categories/list";
        }
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam("name") String name,
                         @RequestParam(name = "description", required = false) String description,
                         @RequestParam(name = "q", required = false) String q,
                         @RequestParam(name = "page", defaultValue = "0") int page,
                         @RequestParam(name = "size", defaultValue = "10") int size,
                         Model model,
                         RedirectAttributes ra) {
        try {
            categoryService.update(id, name, description);
            ra.addFlashAttribute("message", "Category updated successfully");
            var pageData = categoryService.searchByNamePaged(q, page, size);
            if (pageData.isEmpty()) {
                return "redirect:/admin/categories";
            }

            return "redirect:" + UriComponentsBuilder.fromPath("/admin/categories")
                    .queryParam("q", q == null ? "" : q)
                    .queryParam("page", Math.max(0, page))
                    .queryParam("size", size)
                    .build()
                    .toUriString();
        } catch (ResponseStatusException ex) {
            model.addAttribute("error", ex.getReason() == null ? "Cannot update category" : ex.getReason());
            model.addAttribute("q", q == null ? "" : q);
            model.addAttribute("categories", categoryService.searchByNamePaged(q, page, size));
            model.addAttribute("size", size);
            return "admin/categories/list";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        categoryService.delete(id);
        ra.addFlashAttribute("message", "Category deleted successfully");
        return "redirect:/admin/categories";
    }
}







