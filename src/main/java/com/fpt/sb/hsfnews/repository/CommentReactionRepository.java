package com.fpt.sb.hsfnews.repository;

import com.fpt.sb.hsfnews.entity.Comment;
import com.fpt.sb.hsfnews.entity.CommentReaction;
import com.fpt.sb.hsfnews.entity.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {
    
    List<CommentReaction> findByCommentId(Long commentId);
    
    List<CommentReaction> findByComment(Comment comment);
    
    CommentReaction findByCommentAndReactionTypeAndUserName(Comment comment, ReactionType reactionType, String userName);
    
    List<CommentReaction> findByCommentIdAndReactionType(Long commentId, ReactionType reactionType);
    
    void deleteByCommentAndReactionTypeAndUserName(Comment comment, ReactionType reactionType, String userName);
}
