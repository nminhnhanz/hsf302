package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.repository.CategoryRepository;
import com.fpt.sb.hsfnews.repository.TagRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LookupApiController {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    public LookupApiController(CategoryRepository categoryRepository, TagRepository tagRepository) {
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
    }

    // Sửa Record để chứa cả ID
    public record ItemResult(Long id, String name) {}

    @GetMapping("/categories")
    public List<ItemResult> categories(@RequestParam("q") String q,
                                       @RequestParam(name = "limit", defaultValue = "8") int limit) {
        int l = Math.max(1, Math.min(limit, 20));
        var pageable = PageRequest.of(0, l, Sort.by(Sort.Direction.ASC, "name"));
        return categoryRepository.findByNameContainingIgnoreCase(q, pageable)
                .stream()
                .map(c -> new ItemResult(c.getId(), c.getName())) // Trả về cả ID
                .toList();
    }

    @GetMapping("/tags")
    public List<ItemResult> tags(@RequestParam("q") String q,
                                 @RequestParam(name = "limit", defaultValue = "8") int limit) {
        int l = Math.max(1, Math.min(limit, 20));
        var pageable = PageRequest.of(0, l, Sort.by(Sort.Direction.ASC, "name"));
        return tagRepository.findByNameContainingIgnoreCase(q, pageable)
                .stream()
                .map(t -> new ItemResult(t.getId(), t.getName())) // Trả về cả ID
                .toList();
    }
}