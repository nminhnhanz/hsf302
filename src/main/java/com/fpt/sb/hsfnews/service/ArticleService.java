package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.Article;
import com.fpt.sb.hsfnews.entity.ArticleStatus;
import com.fpt.sb.hsfnews.repository.ArticleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public List<Article> latestPublished6() {
        return articleRepository.findTop6ByStatusOrderByCreatedAtDesc(ArticleStatus.PUBLISHED);
    }

    public Optional<Article> getPublishedDetail(Long id) {
        return articleRepository.findByIdAndStatus(id, ArticleStatus.PUBLISHED);
    }

    public Optional<Article> getPublishedByTitle(String title) {
        return articleRepository.findByTitleIgnoreCaseAndStatus(title, ArticleStatus.PUBLISHED);
    }

    // Hàm search chính
    public Page<Article> searchPublished(String q, Long categoryId, List<Long> tagIds, int page, int size, String sortDir) {
        Sort.Direction direction = parseCreatedAtDirection(sortDir);
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(direction, "createdAt"));

        // Nếu người dùng KHÔNG nhập tag nào -> Gọi hàm không có mệnh đề JOIN Tags
        if (tagIds == null || tagIds.isEmpty()) {
            return articleRepository.searchPublished(ArticleStatus.PUBLISHED, q, categoryId, pageable);
        }

        // Nếu người dùng CÓ nhập tag -> Gọi hàm có mệnh đề IN
        return articleRepository.searchPublishedWithTags(ArticleStatus.PUBLISHED, q, categoryId, tagIds, pageable);
    }

    private Sort.Direction parseCreatedAtDirection(String sortDir) {
        if (sortDir == null) {
            return Sort.Direction.DESC;
        }
        return "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 50);
    }
}