package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.repository.CategoryRepository;
import com.fpt.sb.hsfnews.repository.TagRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    // Record dùng chung để trả về kết quả tìm kiếm nhanh
    public record ItemResult(Long id, String name) {}

    /**
     * API Tìm kiếm Category theo tên
     */
    @GetMapping("/categories")
    public List<ItemResult> searchCategories(@RequestParam(value = "q", defaultValue = "") String q,
                                             @RequestParam(value = "limit", defaultValue = "10") int limit) {
        int l = Math.max(1, Math.min(limit, 50));
        Pageable pageable = PageRequest.of(0, l, Sort.by(Sort.Direction.ASC, "name"));

        return categoryRepository.findByNameContainingIgnoreCase(q, pageable)
                .getContent()
                .stream()
                .map(c -> new ItemResult(c.getId(), c.getName()))
                .toList();
    }

    /**
     * API Tìm kiếm Tag theo tên
     */
    @GetMapping("/tags")
    public List<ItemResult> searchTags(@RequestParam(value = "q", defaultValue = "") String q,
                                       @RequestParam(value = "limit", defaultValue = "10") int limit) {
        int l = Math.max(1, Math.min(limit, 50));
        Pageable pageable = PageRequest.of(0, l, Sort.by(Sort.Direction.ASC, "name"));

        return tagRepository.findByNameContainingIgnoreCase(q, pageable)
                .getContent()
                .stream()
                .map(t -> new ItemResult(t.getId(), t.getName()))
                .toList();
    }
}