package com.fpt.sb.hsfnews.repository;

import com.fpt.sb.hsfnews.entity.Article;
import com.fpt.sb.hsfnews.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    List<Comment> findByArticleIdOrderByCreatedAtDesc(Long articleId);
    
    List<Comment> findByArticle(Article article);
    
    List<Comment> findByParentCommentOrderByCreatedAtDesc(Comment parentComment);
    
    List<Comment> findByParentCommentIdOrderByCreatedAtDesc(Long parentCommentId);
    
    List<Comment> findByArticleIdAndParentCommentIsNullOrderByCreatedAtDesc(Long articleId);
}

