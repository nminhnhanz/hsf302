package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.Tag;
import com.fpt.sb.hsfnews.repository.ArticleRepository;
import com.fpt.sb.hsfnews.repository.TagRepository; // Bổ sung import bị thiếu
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final ArticleRepository articleRepository;

    // Hợp nhất Constructor Injection
    public TagService(TagRepository tagRepository, ArticleRepository articleRepository) {
        this.tagRepository = tagRepository;
        this.articleRepository = articleRepository;
    }

    public List<Tag> findAllById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return tagRepository.findAllById(ids);
    }

    public List<Tag> searchByName(String q) {
        String keyword = q == null ? "" : q.trim();
        if (keyword.isEmpty()) {
            return tagRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        }
        return tagRepository.findByNameContainingIgnoreCase(
                keyword,
                PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "name"))
        ).getContent();
    }

    public Page<Tag> searchByNamePaged(String q, int page, int size) {
        String keyword = q == null ? "" : q.trim();
        int currentPage = Math.max(0, page);
        int pageSize = size <= 0 ? 10 : Math.min(size, 50);
        var pageable = PageRequest.of(currentPage, pageSize, Sort.by(Sort.Direction.ASC, "name"));

        if (keyword.isEmpty()) {
            return tagRepository.findAll(pageable);
        }
        return tagRepository.findByNameContainingIgnoreCase(keyword, pageable);
    }

    @Transactional
    public Tag getOrCreateByName(String name) {
        String normalized = normalizeName(name);
        return tagRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    Tag t = new Tag();
                    t.setName(normalized);
                    return tagRepository.save(t);
                });
    }

    @Transactional
    public Tag create(String name) {
        String normalized = normalizeName(name);
        if (tagRepository.findByNameIgnoreCase(normalized).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag already exists");
        }
        Tag t = new Tag();
        t.setName(normalized);
        return tagRepository.save(t);
    }

    @Transactional
    public Tag update(Long id, String name) {
        Tag t = tagRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String normalized = normalizeName(name);
        tagRepository.findByNameIgnoreCase(normalized)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag already exists");
                });

        t.setName(normalized);
        return tagRepository.save(t);
    }

    @Transactional
    public void delete(Long id) {
        Tag t = tagRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Xử lý gỡ bỏ Tag khỏi các Article liên quan trước khi xóa Tag
        var articles = articleRepository.findByTags_Id(id);
        for (var article : articles) {
            Set<Tag> retained = article.getTags().stream()
                    .filter(tag -> !tag.getId().equals(id))
                    .collect(Collectors.toSet());
            article.setTags(retained);
        }
        articleRepository.saveAll(articles);
        tagRepository.delete(t);
    }

    private String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tag name is required");
        }
        return name.trim();
    }
}