package com.fpt.sb.hsfnews.repository;

import com.fpt.sb.hsfnews.entity.Article;
import com.fpt.sb.hsfnews.entity.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findByTitleIgnoreCase(String title);

    Optional<Article> findByTitleIgnoreCaseAndStatus(String title, ArticleStatus status);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    List<Article> findTop6ByStatusOrderByCreatedAtDesc(ArticleStatus status);

    @EntityGraph(attributePaths = {"author", "category", "tags", "comments"})
    Optional<Article> findByIdAndStatus(Long id, ArticleStatus status);

    // --- 1. HÀM TÌM KIẾM KHI NGƯỜI DÙNG KHÔNG CHỌN TAG NÀO ---
    @EntityGraph(attributePaths = {"author", "category"})
    @Query("""
            select distinct a
            from Article a
            where a.status = :status
              and (
                   :q is null
                   or :q = ''
                   or lower(a.title) like lower(concat('%', :q, '%'))
                   or lower(a.summary) like lower(concat('%', :q, '%'))
                   or lower(a.content) like lower(concat('%', :q, '%'))
              )
              and (:categoryId is null or a.category.id = :categoryId)
            """)
    Page<Article> searchPublished(
            @Param("status") ArticleStatus status,
            @Param("q") String q,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    // --- 2. HÀM TÌM KIẾM KHI NGƯỜI DÙNG CÓ CHỌN TAGS ---
    @EntityGraph(attributePaths = {"author", "category", "tags"})
    @Query("""
            select distinct a
            from Article a
            join a.tags t
            where a.status = :status
              and (
                   :q is null
                   or :q = ''
                   or lower(a.title) like lower(concat('%', :q, '%'))
                   or lower(a.summary) like lower(concat('%', :q, '%'))
                   or lower(a.content) like lower(concat('%', :q, '%'))
              )
              and (:categoryId is null or a.category.id = :categoryId)
              and (t.id in :tagIds)
            """)
    Page<Article> searchPublishedWithTags(
            @Param("status") ArticleStatus status,
            @Param("q") String q,
            @Param("categoryId") Long categoryId,
            @Param("tagIds") List<Long> tagIds,
            Pageable pageable
    );
}