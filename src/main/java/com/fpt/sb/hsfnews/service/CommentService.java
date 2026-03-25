package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.Article;
import com.fpt.sb.hsfnews.entity.Comment;
import com.fpt.sb.hsfnews.entity.CommentReaction;
import com.fpt.sb.hsfnews.entity.ReactionType;
import com.fpt.sb.hsfnews.repository.CommentReactionRepository;
import com.fpt.sb.hsfnews.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentReactionRepository reactionRepository;

    public List<Comment> getCommentsByArticleId(Long articleId) {
        return commentRepository.findByArticleIdOrderByCreatedAtDesc(articleId);
    }

    public List<Comment> getParentCommentsByArticleId(Long articleId) {
        return commentRepository.findByArticleIdAndParentCommentIsNullOrderByCreatedAtDesc(articleId);
    }

    public List<Comment> getRepliesByCommentId(Long commentId) {
        return commentRepository.findByParentCommentIdOrderByCreatedAtDesc(commentId);
    }

    public Comment createComment(String authorName, String content, Article article) {
        Comment comment = new Comment();
        comment.setAuthorName(authorName);
        comment.setContent(content);
        comment.setArticle(article);
        return commentRepository.save(comment);
    }

    public Comment createReply(String authorName, String content, Article article, Comment parentComment) {
        Comment reply = new Comment();
        reply.setAuthorName(authorName);
        reply.setContent(content);
        reply.setArticle(article);
        reply.setParentComment(parentComment);
        return commentRepository.save(reply);
    }

    public Comment updateComment(Long commentId, String content) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment != null) {
            comment.setContent(content);
            return commentRepository.save(comment);
        }
        return null;
    }

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId).orElse(null);
    }

    public CommentReaction addReaction(Long commentId, ReactionType reactionType, String userName) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) return null;

        // Check if user already reacted with this type
        CommentReaction existingReaction = reactionRepository
                .findByCommentAndReactionTypeAndUserName(comment, reactionType, userName);

        if (existingReaction != null) {
            // Remove reaction if it already exists
            reactionRepository.delete(existingReaction);
            return null;
        } else {
            // Add new reaction
            CommentReaction reaction = new CommentReaction();
            reaction.setComment(comment);
            reaction.setReactionType(reactionType);
            reaction.setUserName(userName);
            return reactionRepository.save(reaction);
        }
    }

    public List<CommentReaction> getReactionsByCommentId(Long commentId) {
        return reactionRepository.findByCommentId(commentId);
    }

    public long getReactionCount(Long commentId, ReactionType reactionType) {
        return reactionRepository.findByCommentIdAndReactionType(commentId, reactionType).size();
    }
}
