package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.entity.*;
import com.fpt.sb.hsfnews.repository.UserRepository;
import com.fpt.sb.hsfnews.service.ArticleService;
import com.fpt.sb.hsfnews.service.CommentRealtimeService;
import com.fpt.sb.hsfnews.service.CommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/comments")
@RequiredArgsConstructor // Sử dụng Lombok để tự động tạo Constructor cho final fields
public class CommentController {

    private final CommentService commentService;
    private final ArticleService articleService;
    private final UserRepository userRepository;
    private final CommentRealtimeService commentRealtimeService;

    // --- VIEW METHODS (Server-side rendering cho Thymeleaf) ---

    @PostMapping
    public String addComment(@Valid @ModelAttribute("commentForm") CommentForm commentForm,
                             BindingResult bindingResult,
                             @RequestParam("articleId") Long articleId,
                             @AuthenticationPrincipal UserDetails principal,
                             RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.commentForm", bindingResult);
            redirectAttributes.addFlashAttribute("commentForm", commentForm);
            redirectAttributes.addFlashAttribute("error", "Nội dung bình luận không hợp lệ.");
            return "redirect:/articles/" + articleId + "#comment-section";
        }

        User currentUser = getCurrentUser(principal);
        Article article = articleService.getArticleById(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bài viết không tồn tại"));

        Comment created;
        if (commentForm.getParentCommentId() != null) {
            Comment parentComment = commentService.getCommentById(commentForm.getParentCommentId());
            created = commentService.createReply(currentUser, commentForm.getContent(), article, parentComment);
            redirectAttributes.addFlashAttribute("success", "Đã phản hồi bình luận!");
        } else {
            created = commentService.createComment(currentUser, commentForm.getContent(), article);
            redirectAttributes.addFlashAttribute("success", "Đã thêm bình luận mới!");
        }

        if (created != null) {
            commentRealtimeService.publishCommentCreated(created);
        }

        return "redirect:/articles/" + articleId + "#comment-" + (created != null ? created.getId() : "");
    }

    @PostMapping("/delete/{id}")
    public String deleteComment(@PathVariable("id") Long commentId,
                                @RequestParam("articleId") Long articleId,
                                @AuthenticationPrincipal UserDetails principal,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {

        validateOwnership(commentId, principal, authentication);
        commentService.deleteComment(commentId);

        redirectAttributes.addFlashAttribute("success", "Bình luận đã được xóa.");
        return "redirect:/articles/" + articleId + "#comment-section";
    }

    // --- API METHODS (Trả về JSON cho AJAX/Frontend) ---

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> addCommentApi(@Valid @ModelAttribute CommentForm commentForm,
                                           BindingResult bindingResult,
                                           @RequestParam("articleId") Long articleId,
                                           @AuthenticationPrincipal UserDetails principal,
                                           Authentication authentication) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dữ liệu không hợp lệ"));
        }

        User currentUser = getCurrentUser(principal);
        Article article = articleService.getArticleById(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Comment created;
        if (commentForm.getParentCommentId() != null) {
            Comment parentComment = commentService.getCommentById(commentForm.getParentCommentId());
            created = commentService.createReply(currentUser, commentForm.getContent(), article, parentComment);
        } else {
            created = commentService.createComment(currentUser, commentForm.getContent(), article);
        }

        commentRealtimeService.publishCommentCreated(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(toCommentItemResponse(created, authentication));
    }

    @PostMapping("/api/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteCommentApi(@PathVariable("id") Long commentId,
                                              @AuthenticationPrincipal UserDetails principal,
                                              Authentication authentication) {

        validateOwnership(commentId, principal, authentication);

        CommentService.HardDeleteResult deleted = commentService.hardDeleteCommentTree(commentId);
        if (deleted == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        return ResponseEntity.ok(new ActionResponse(true, "Xóa thành công", commentId, deleted.deletedCommentIds()));
    }

    @GetMapping("/api/replies/{parentCommentId}")
    @ResponseBody
    public List<CommentItemResponse> getReplies(@PathVariable Long parentCommentId,
                                                Authentication authentication) {
        return commentService.getRepliesByCommentId(parentCommentId).stream()
                .map(comment -> toCommentItemResponse(comment, authentication))
                .collect(Collectors.toList());
    }

    @PostMapping("/api/edit/{id}")
    @ResponseBody
    public ResponseEntity<?> editCommentApi(@PathVariable("id") Long commentId,
                                            @RequestParam("content") String content,
                                            @AuthenticationPrincipal UserDetails principal,
                                            Authentication authentication) {
        validateOwnership(commentId, principal, authentication);
        Comment updated = commentService.editComment(commentId, content);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(toCommentItemResponse(updated, authentication));
    }

    @PostMapping("/edit/{id}")
    public String editComment(@PathVariable("id") Long commentId,
                              @RequestParam("content") String content,
                              @RequestParam("articleId") Long articleId,
                              @AuthenticationPrincipal UserDetails principal,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        validateOwnership(commentId, principal, authentication);
        Comment updated = commentService.editComment(commentId, content);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        redirectAttributes.addFlashAttribute("success", "Bình luận đã được cập nhật.");
        return "redirect:/articles/" + articleId + "#comment-" + updated.getId();
    }

    @GetMapping("/api/path/{commentId}")
    @ResponseBody
    public CommentPathResponse getCommentPath(@PathVariable Long commentId) {
        Comment target = commentService.getCommentById(commentId);
        if (target == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        List<Long> path = commentService.getCommentPathIds(commentId);
        return new CommentPathResponse(commentId, path);
    }

    // --- HELPERS ---

    private void validateOwnership(Long commentId, UserDetails principal, Authentication authentication) {
        User currentUser = getCurrentUser(principal);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !commentService.isOwner(commentId, currentUser.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền này.");
        }
    }

    private User getCurrentUser(UserDetails principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return userRepository.findByUsernameIgnoreCase(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private CommentItemResponse toCommentItemResponse(Comment comment, Authentication authentication) {
        String currentUsername = authentication != null ? authentication.getName() : null;
        String authorUsername = "unknown";
        try {
            User author = comment.getAuthor();
            if (author != null && author.getUsername() != null) {
                authorUsername = author.getUsername();
            }
        } catch (RuntimeException ignored) {
            authorUsername = "unknown";
        }
        String articleAuthorUsername = "";
        Long parentCommentId = null;
        Long articleId = null;
        try {
            if (comment.getParentComment() != null) {
                parentCommentId = comment.getParentComment().getId();
            }
            Article article = comment.getArticle();
            if (article != null) {
                articleId = article.getId();
                User articleAuthor = article.getAuthor();
                if (articleAuthor != null && articleAuthor.getUsername() != null) {
                    articleAuthorUsername = articleAuthor.getUsername();
                }
            }
        } catch (RuntimeException ignored) {
            articleAuthorUsername = "";
        }

        boolean canEdit = authorUsername.equalsIgnoreCase(currentUsername);
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return new CommentItemResponse(
                comment.getId(),
                parentCommentId,
                articleId,
                authorUsername,
                "@" + authorUsername,
                comment.getContent(),
                comment.getCreatedAt(),
                canEdit,
                canEdit || isAdmin, // canDelete
                authorUsername.equalsIgnoreCase(articleAuthorUsername),
                commentService.getDirectReplyCount(comment.getId())
        );
    }

    // --- DATA STRUCTURES (DTOs) ---

    public static class CommentForm {
        @NotBlank(message = "Nội dung bình luận không được để trống")
        @Size(min = 1, max = 1000, message = "Bình luận từ 1 đến 1000 ký tự")
        private String content;
        private Long parentCommentId;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Long getParentCommentId() { return parentCommentId; }
        public void setParentCommentId(Long parentCommentId) { this.parentCommentId = parentCommentId; }
    }

    public record CommentItemResponse(Long id, Long parentCommentId, Long articleId, String authorUsername,
                                      String authorDisplay, String content, LocalDateTime createdAt,
                                      boolean canEdit, boolean canDelete, boolean articleAuthor,
                                      long replyCount) {}

    public record ActionResponse(boolean success, String message, Long commentId, List<Long> deletedCommentIds) {}

    public record CommentPathResponse(Long commentId, List<Long> path) {}
}