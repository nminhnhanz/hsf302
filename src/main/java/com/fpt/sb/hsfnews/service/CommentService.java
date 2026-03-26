package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.*;
import com.fpt.sb.hsfnews.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    // --- CREATE METHODS ---

    public Comment createComment(User author, String content, Article article) {
        Comment comment = new Comment();
        comment.setAuthor(author);
        comment.setContent(content);
        comment.setArticle(article);
        return commentRepository.save(comment);
    }

    public Comment createReply(User author, String content, Article article, Comment parentComment) {
        Comment reply = new Comment();
        reply.setAuthor(author);
        reply.setContent(content);
        reply.setArticle(article);
        reply.setParentComment(parentComment);
        return commentRepository.save(reply);
    }

    // --- READ METHODS ---

    public List<Comment> getCommentsByArticleId(Long articleId) {
        return dedupeBySignature(commentRepository.findByArticleIdAndReplacedByIsNullOrderByCreatedAtDesc(articleId));
    }

    public List<Comment> getParentCommentsByArticleId(Long articleId) {
        return dedupeBySignature(commentRepository.findByArticleIdAndParentCommentIsNullAndReplacedByIsNullOrderByCreatedAtDesc(articleId));
    }

    public List<Comment> getRepliesByCommentId(Long commentId) {
        return dedupeBySignature(commentRepository.findByParentCommentIdAndReplacedByIsNullOrderByCreatedAtDesc(commentId));
    }

    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId).orElse(null);
    }

    public List<Long> getCommentPathIds(Long commentId) {
        if (commentId == null) {
            return List.of();
        }

        List<Long> path = new ArrayList<>();
        Long cursorId = commentId;
        Set<Long> visited = new HashSet<>();
        while (cursorId != null && visited.add(cursorId)) {
            Comment cursor = commentRepository.findWithAuthorAndParentById(cursorId).orElse(null);
            if (cursor == null) {
                break;
            }
            path.add(0, cursor.getId());
            cursorId = cursor.getParentComment() != null ? cursor.getParentComment().getId() : null;
        }
        return path;
    }

    // --- UPDATE METHODS ---

    @Transactional
    public Comment editComment(Long commentId, String content) {
        Comment existing = commentRepository.findById(commentId).orElse(null);
        if (existing == null) {
            return null;
        }
        existing.setContent(content);
        commentRepository.save(existing);
        return commentRepository.findHydratedById(existing.getId()).orElse(existing);
    }

    public long getDirectReplyCount(Long commentId) {
        return commentRepository.countByParentCommentIdAndReplacedByIsNull(commentId);
    }

    public Map<Long, Long> countRepliesByParentIds(List<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : commentRepository.countRepliesGroupByParentId(parentIds)) {
            Long parentId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();
            counts.put(parentId, count);
        }
        return counts;
    }

    // --- REACTION METHODS ---
    // --- DELETE METHODS ---

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    @Transactional
    public HardDeleteResult hardDeleteCommentTree(Long commentId) {
        Comment root = commentRepository.findById(commentId).orElse(null);
        if (root == null) return null;

        Long articleId = root.getArticle() != null ? root.getArticle().getId() : null;
        Set<Long> deletedIds = new LinkedHashSet<>();
        Set<Long> visitedIds = new LinkedHashSet<>();

        // Dọn dẹp các bản trùng lặp nếu có cùng signature
        for (Comment duplicate : findSameSignatureSiblings(root)) {
            collectHardDeleteIdsRecursively(duplicate, deletedIds, visitedIds);
        }

        collectHardDeleteIdsRecursively(root, deletedIds, visitedIds);

        if (!deletedIds.isEmpty()) {
            commentRepository.deleteAllByIdInBatch(deletedIds);
        }
        return new HardDeleteResult(articleId, new ArrayList<>(deletedIds));
    }

    // --- HELPER METHODS ---

    private void collectHardDeleteIdsRecursively(Comment comment, Set<Long> deletedIds, Set<Long> visitedIds) {
        if (comment == null || comment.getId() == null || !visitedIds.add(comment.getId())) return;

        // Xóa các reply
        List<Comment> children = commentRepository.findByParentCommentIdAndReplacedByIsNullOrderByCreatedAtDesc(comment.getId());
        for (Comment child : children) {
            collectHardDeleteIdsRecursively(child, deletedIds, visitedIds);
        }

        // Xóa lịch sử các phiên bản cũ
        List<Comment> previousVersions = commentRepository.findByReplacedByIdOrderByCreatedAtDesc(comment.getId());
        for (Comment previous : previousVersions) {
            collectHardDeleteIdsRecursively(previous, deletedIds, visitedIds);
        }

        deletedIds.add(comment.getId());
    }

    private List<Comment> findSameSignatureSiblings(Comment root) {
        List<Comment> siblings;
        if (root.getParentComment() != null) {
            siblings = commentRepository.findByParentCommentIdAndReplacedByIsNullOrderByCreatedAtDesc(root.getParentComment().getId());
        } else if (root.getArticle() != null) {
            siblings = commentRepository.findByArticleIdAndParentCommentIsNullAndReplacedByIsNullOrderByCreatedAtDesc(root.getArticle().getId());
        } else {
            return List.of();
        }

        return siblings.stream()
                .filter(s -> !Objects.equals(s.getId(), root.getId()))
                .filter(s -> Objects.equals(s.getContent(), root.getContent()))
                .filter(s -> Objects.equals(authorUsernameOf(s), authorUsernameOf(root)))
                .toList();
    }

    private List<Comment> dedupeBySignature(List<Comment> comments) {
        Map<String, Comment> bySignature = new LinkedHashMap<>();
        for (Comment c : comments) {
            if (c == null) continue;
            String signature = authorUsernameOf(c) + "|" +
                    (c.getContent() != null ? c.getContent().trim() : "") + "|" +
                    (c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
            bySignature.putIfAbsent(signature, c);
        }
        return new ArrayList<>(bySignature.values());
    }

    private String authorUsernameOf(Comment comment) {
        if (comment == null || comment.getAuthor() == null || comment.getAuthor().getUsername() == null) {
            return "unknown";
        }
        return comment.getAuthor().getUsername();
    }

    public boolean isOwner(Long commentId, String username) {
        if (commentId == null || username == null || username.isBlank()) {
            return false;
        }
        return commentRepository.existsByIdAndAuthorUsernameIgnoreCase(commentId, username);
    }

    // Record data structures
    public record HardDeleteResult(Long articleId, List<Long> deletedCommentIds) {}
}