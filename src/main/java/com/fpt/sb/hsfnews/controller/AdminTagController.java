package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.service.TagService;
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
@RequestMapping("/admin/tags")
public class AdminTagController {

    private final TagService tagService;

    public AdminTagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "size", defaultValue = "10") int size,
                       Model model) {
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("tags", tagService.searchByNamePaged(q, page, size));
        model.addAttribute("size", size);
        return "admin/tags/list";
    }

    @PostMapping
    public String create(@RequestParam("name") String name,
                         @RequestParam(name = "q", required = false) String q,
                         @RequestParam(name = "page", defaultValue = "0") int page,
                         @RequestParam(name = "size", defaultValue = "10") int size,
                         Model model,
                         RedirectAttributes ra) {
        try {
            tagService.create(name);
            ra.addFlashAttribute("message", "Tag created successfully");
            return "redirect:/admin/tags";
        } catch (ResponseStatusException ex) {
            model.addAttribute("error", ex.getReason() == null ? "Cannot create tag" : ex.getReason());
            model.addAttribute("q", q == null ? "" : q);
            model.addAttribute("tags", tagService.searchByNamePaged(q, page, size));
            model.addAttribute("size", size);
            return "admin/tags/list";
        }
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam("name") String name,
                         @RequestParam(name = "q", required = false) String q,
                         @RequestParam(name = "page", defaultValue = "0") int page,
                         @RequestParam(name = "size", defaultValue = "10") int size,
                         Model model,
                         RedirectAttributes ra) {
        try {
            tagService.update(id, name);
            ra.addFlashAttribute("message", "Tag updated successfully");
            var pageData = tagService.searchByNamePaged(q, page, size);
            if (pageData.isEmpty()) {
                return "redirect:/admin/tags";
            }

            return "redirect:" + UriComponentsBuilder.fromPath("/admin/tags")
                    .queryParam("q", q == null ? "" : q)
                    .queryParam("page", Math.max(0, page))
                    .queryParam("size", size)
                    .build()
                    .toUriString();
        } catch (ResponseStatusException ex) {
            model.addAttribute("error", ex.getReason() == null ? "Cannot update tag" : ex.getReason());
            model.addAttribute("q", q == null ? "" : q);
            model.addAttribute("tags", tagService.searchByNamePaged(q, page, size));
            model.addAttribute("size", size);
            return "admin/tags/list";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        tagService.delete(id);
        ra.addFlashAttribute("message", "Tag deleted successfully");
        return "redirect:/admin/tags";
    }
}







