package com.fpt.sb.hsfnews.repository;

import com.fpt.sb.hsfnews.entity.Article;
import com.fpt.sb.hsfnews.entity.ArticleStatus;
import com.fpt.sb.hsfnews.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional; // Bổ sung import bị thiếu

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    // --- TRUY VẤN CƠ BẢN ---

    Optional<Article> findByTitleIgnoreCase(String title);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Optional<Article> findByTitleIgnoreCaseAndStatus(String title, ArticleStatus status);

    @Override
    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Optional<Article> findById(Long id);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    Optional<Article> findByIdAndStatus(Long id, ArticleStatus status);

    long countByStatus(ArticleStatus status);

    List<Article> findByCreatedAtGreaterThanEqual(LocalDateTime createdAt);

    @Query("""
            SELECT a, COUNT(c)
            FROM Article a
            LEFT JOIN a.comments c
            GROUP BY a
            ORDER BY COUNT(c) DESC, a.createdAt DESC
            """)
    List<Object[]> findTopArticlesByCommentCount(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    List<Article> findTop6ByStatusOrderByCreatedAtDesc(ArticleStatus status);


    // --- HÀM TÌM KIẾM CHO NGƯỜI DÙNG (PUBLIC SEARCH) ---

    // 1. Khi KHÔNG lọc theo tags
    @EntityGraph(attributePaths = {"author", "category", "tags"})
    @Query("""
            SELECT DISTINCT a
            FROM Article a
            WHERE a.status = :status
              AND (
                   :q IS NULL OR :q = ''
                   OR LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.summary) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.content) LIKE LOWER(CONCAT('%', :q, '%'))
              )
              AND (:categoryId IS NULL OR a.category.id = :categoryId)
            """)
    Page<Article> searchPublished(
            @Param("status") ArticleStatus status,
            @Param("q") String q,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    // 2. Khi CÓ lọc theo tags
    @EntityGraph(attributePaths = {"author", "category", "tags"})
    @Query("""
            SELECT DISTINCT a
            FROM Article a
            JOIN a.tags t
            WHERE a.status = :status
              AND (
                   :q IS NULL OR :q = ''
                   OR LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.summary) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.content) LIKE LOWER(CONCAT('%', :q, '%'))
              )
              AND (:categoryId IS NULL OR a.category.id = :categoryId)
              AND (t.id IN :tagIds)
            """)
    Page<Article> searchPublishedWithTags(
            @Param("status") ArticleStatus status,
            @Param("q") String q,
            @Param("categoryId") Long categoryId,
            @Param("tagIds") List<Long> tagIds,
            Pageable pageable
    );


    // --- HÀM CHO QUẢN TRỊ/TÁC GIẢ (ADMIN/AUTHOR) ---

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    @Query("SELECT a FROM Article a")
    Page<Article> findAllWithTags(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    @Query("SELECT a FROM Article a WHERE a.author = :author")
    Page<Article> findByAuthorWithTags(@Param("author") User author, Pageable pageable);

    // 3. Lọc bài viết của tác giả (Không kèm Tags)
    @EntityGraph(attributePaths = {"author", "category", "tags"})
    @Query("""
            SELECT DISTINCT a
            FROM Article a
            WHERE a.author = :author
              AND (
                   :q IS NULL OR :q = ''
                   OR LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.summary) LIKE LOWER(CONCAT('%', :q, '%'))
              )
              AND (:status IS NULL OR a.status = :status)
              AND (:categoryId IS NULL OR a.category.id = :categoryId)
            """)
    Page<Article> findByAuthorWithFilters(@Param("author") User author,
                                          @Param("q") String q,
                                          @Param("status") ArticleStatus status,
                                          @Param("categoryId") Long categoryId,
                                          Pageable pageable);

    // 4. Lọc bài viết của tác giả (Kèm Tags)
    @EntityGraph(attributePaths = {"author", "category", "tags"})
    @Query("""
            SELECT DISTINCT a
            FROM Article a
            JOIN a.tags t
            WHERE a.author = :author
              AND (
                   :q IS NULL OR :q = ''
                   OR LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.summary) LIKE LOWER(CONCAT('%', :q, '%'))
              )
              AND (:status IS NULL OR a.status = :status)
              AND (:categoryId IS NULL OR a.category.id = :categoryId)
              AND t.id IN :tagIds
            """)
    Page<Article> findByAuthorWithFiltersAndTags(@Param("author") User author,
                                                 @Param("q") String q,
                                                 @Param("status") ArticleStatus status,
                                                 @Param("categoryId") Long categoryId,
                                                 @Param("tagIds") List<Long> tagIds,
                                                 Pageable pageable);


    // --- CÁC TRUY VẤN LIÊN QUAN ---

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    List<Article> findByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"author", "category", "tags"})
    List<Article> findByTags_Id(Long tagId);
}