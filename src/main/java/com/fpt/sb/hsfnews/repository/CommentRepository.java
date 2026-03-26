package com.fpt.sb.hsfnews.repository;

import com.fpt.sb.hsfnews.entity.Article;
import com.fpt.sb.hsfnews.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // --- TRUY VẤN THEO BÀI VIẾT (ARTICLE) ---

    List<Comment> findByArticle(Article article);

    List<Comment> findByArticleAndReplacedByIsNull(Article article);

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByArticleIdAndReplacedByIsNullOrderByCreatedAtDesc(Long articleId);

    // Lấy danh sách bình luận gốc (không có bình luận cha) của một bài viết
    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByArticleIdAndParentCommentIsNullAndReplacedByIsNullOrderByCreatedAtDesc(Long articleId);

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByArticleIdAndParentCommentIsNullOrderByCreatedAtDesc(Long articleId);


    // --- TRUY VẤN THEO BÌNH LUẬN CHA (PARENT/REPLIES) ---

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByParentCommentOrderByCreatedAtDesc(Comment parentComment);

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByParentCommentIdOrderByCreatedAtDesc(Long parentCommentId);

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByParentCommentAndReplacedByIsNullOrderByCreatedAtDesc(Comment parentComment);

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByParentCommentIdAndReplacedByIsNullOrderByCreatedAtDesc(Long parentCommentId);

    @EntityGraph(attributePaths = {"author", "article", "article.author", "parentComment", "parentComment.author"})
    @Query("select c from Comment c where c.id = :id")
    Optional<Comment> findHydratedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"author", "parentComment"})
    @Query("select c from Comment c where c.id = :id")
    Optional<Comment> findWithAuthorAndParentById(@Param("id") Long id);

    boolean existsByIdAndAuthorUsernameIgnoreCase(Long id, String authorUsername);

    long countByParentCommentIdAndReplacedByIsNull(Long parentCommentId);


    // --- TRUY VẤN LỊCH SỬ CHỈNH SỬA (REPLACED BY) ---

    List<Comment> findByReplacedByIdOrderByCreatedAtDesc(Long replacedById);

    Comment findFirstByReplacedByIdOrderByCreatedAtDesc(Long replacedById);

    long countByReplacedById(Long replacedById);


    // --- TRUY VẤN NÂNG CAO (CUSTOM QUERIES) ---

    /**
     * Đếm số lượng phản hồi cho một danh sách các ID bình luận cha.
     * Chỉ đếm những bình luận chưa bị thay thế (replacedBy is null).
     */
    @Query("""
            SELECT c.parentComment.id, COUNT(c.id)
            FROM Comment c
            WHERE c.parentComment.id IN :parentIds AND c.replacedBy IS NULL
            GROUP BY c.parentComment.id
            """)
    List<Object[]> countRepliesGroupByParentId(@Param("parentIds") List<Long> parentIds);
}