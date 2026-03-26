package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.Article;
import com.fpt.sb.hsfnews.entity.ArticleStatus;
import com.fpt.sb.hsfnews.entity.User;
import com.fpt.sb.hsfnews.repository.ArticleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    // --- QUERIES FOR PUBLIC FRONTEND ---

    public List<Article> latestPublished6() {
        return articleRepository.findTop6ByStatusOrderByCreatedAtDesc(ArticleStatus.PUBLISHED);
    }

    public Optional<Article> getPublishedDetail(Long id) {
        return articleRepository.findByIdAndStatus(id, ArticleStatus.PUBLISHED);
    }

    public Optional<Article> getPublishedByTitle(String title) {
        return articleRepository.findByTitleIgnoreCaseAndStatus(title, ArticleStatus.PUBLISHED);
    }

    public Page<Article> searchPublished(String q, Long categoryId, List<Long> tagIds, int page, int size, String sortDir) {
        Sort.Direction direction = parseCreatedAtDirection(sortDir);
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(direction, "createdAt"));

        if (tagIds == null || tagIds.isEmpty()) {
            return articleRepository.searchPublished(ArticleStatus.PUBLISHED, q, categoryId, pageable);
        }
        return articleRepository.searchPublishedWithTags(ArticleStatus.PUBLISHED, q, categoryId, tagIds, pageable);
    }

    // --- QUERIES FOR ADMIN/AUTHOR MANAGEMENT ---

    public Optional<Article> getArticleById(Long id) {
        return articleRepository.findById(id);
    }

    public Page<Article> getAllArticles(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return articleRepository.findAllWithTags(pageable);
    }

    /**
     * Tìm kiếm bài viết theo tác giả với bộ lọc nâng cao (Keyword, Status, Category, Tags)
     */
    public Page<Article> getArticlesByAuthor(User author, String q, ArticleStatus status, Long categoryId, List<Long> tagIds, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        String keyword = (q == null) ? "" : q.trim();

        if (tagIds == null || tagIds.isEmpty()) {
            return articleRepository.findByAuthorWithFilters(author, keyword, status, categoryId, pageable);
        }
        return articleRepository.findByAuthorWithFiltersAndTags(author, keyword, status, categoryId, tagIds, pageable);
    }

    // --- WRITE OPERATIONS (MUTATIONS) ---

    @Transactional
    public Article createArticle(Article article, User author) {
        article.setAuthor(author);
        if (article.getStatus() == null) {
            article.setStatus(ArticleStatus.DRAFT);
        }
        return articleRepository.save(article);
    }

    @Transactional
    public Article updateArticle(Long id, Article updatedArticle) {
        Article existingArticle = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));

        existingArticle.setTitle(updatedArticle.getTitle());
        existingArticle.setSummary(updatedArticle.getSummary());
        existingArticle.setContent(updatedArticle.getContent());
        existingArticle.setThumbnail(updatedArticle.getThumbnail());
        existingArticle.setCategory(updatedArticle.getCategory());
        existingArticle.setTags(updatedArticle.getTags());
        existingArticle.setStatus(updatedArticle.getStatus());

        return articleRepository.save(existingArticle);
    }

    @Transactional
    public void deleteArticle(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new RuntimeException("Article not found");
        }
        articleRepository.deleteById(id);
    }

    @Transactional
    public Article publishArticle(Long id) {
        return updateStatus(id, ArticleStatus.PUBLISHED);
    }

    @Transactional
    public Article setDraftArticle(Long id) {
        return updateStatus(id, ArticleStatus.DRAFT);
    }

    // --- PRIVATE HELPERS ---

    private Article updateStatus(Long id, ArticleStatus status) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Article not found"));
        article.setStatus(status);
        return articleRepository.save(article);
    }

    private Sort.Direction parseCreatedAtDirection(String sortDir) {
        return "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private int clampSize(int size) {
        return (size <= 0) ? 10 : Math.min(size, 50);
    }
}