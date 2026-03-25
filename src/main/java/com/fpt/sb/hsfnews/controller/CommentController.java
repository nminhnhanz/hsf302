package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.entity.Article;
import com.fpt.sb.hsfnews.entity.Comment;
import com.fpt.sb.hsfnews.entity.CommentReaction;
import com.fpt.sb.hsfnews.entity.ReactionType;
import com.fpt.sb.hsfnews.service.ArticleService;
import com.fpt.sb.hsfnews.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private ArticleService articleService;

    public static class CommentForm {
        @NotBlank(message = "Author name is required")
        @Size(min = 2, max = 100, message = "Author name must be between 2 and 100 characters")
        private String authorName;

        @NotBlank(message = "Comment content is required")
        @Size(min = 5, max = 1000, message = "Comment must be between 5 and 1000 characters")
        private String content;

        private Long parentCommentId;

        public String getAuthorName() {
            return authorName;
        }

        public void setAuthorName(String authorName) {
            this.authorName = authorName;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Long getParentCommentId() {
            return parentCommentId;
        }

        public void setParentCommentId(Long parentCommentId) {
            this.parentCommentId = parentCommentId;
        }
    }

    @PostMapping
    public String addComment(@Valid @ModelAttribute CommentForm commentForm, 
                           BindingResult bindingResult,
                           @RequestParam("articleId") Long articleId,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please fix the errors in the form");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.commentForm", bindingResult);
            redirectAttributes.addFlashAttribute("commentForm", commentForm);
            return "redirect:/articles/" + articleId;
        }

        Article article = articleService.getPublishedDetail(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (commentForm.getParentCommentId() != null) {
            // This is a reply
            Comment parentComment = commentService.getCommentById(commentForm.getParentCommentId());
            if (parentComment != null) {
                commentService.createReply(commentForm.getAuthorName(), commentForm.getContent(), article, parentComment);
                redirectAttributes.addFlashAttribute("success", "Reply added successfully!");
            }
        } else {
            // This is a new comment
            commentService.createComment(commentForm.getAuthorName(), commentForm.getContent(), article);
            redirectAttributes.addFlashAttribute("success", "Comment added successfully!");
        }

        return "redirect:/articles/" + articleId + "#comments";
    }

    @PostMapping("/edit/{id}")
    public String editComment(@PathVariable("id") Long commentId,
                             @Valid @ModelAttribute CommentForm commentForm,
                             BindingResult bindingResult,
                             @RequestParam("articleId") Long articleId,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please fix the errors in the form");
            redirectAttributes.addFlashAttribute("editCommentId", commentId);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.commentForm", bindingResult);
            redirectAttributes.addFlashAttribute("commentForm", commentForm);
            return "redirect:/articles/" + articleId;
        }

        Comment updatedComment = commentService.updateComment(commentId, commentForm.getContent());
        if (updatedComment != null) {
            redirectAttributes.addFlashAttribute("success", "Comment updated successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Comment not found!");
        }

        return "redirect:/articles/" + articleId + "#comments";
    }

    @PostMapping("/delete/{id}")
    public String deleteComment(@PathVariable("id") Long commentId,
                               @RequestParam("articleId") Long articleId,
                               RedirectAttributes redirectAttributes) {
        Comment comment = commentService.getCommentById(commentId);
        if (comment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        commentService.deleteComment(commentId);
        redirectAttributes.addFlashAttribute("success", "Comment deleted successfully!");
        return "redirect:/articles/" + articleId + "#comments";
    }

    @PostMapping("/react")
    @ResponseBody
    public Map<String, Object> addReaction(@RequestParam("commentId") Long commentId,
                                         @RequestParam("reactionType") ReactionType reactionType,
                                         @RequestParam("userName") String userName,
                                         HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            CommentReaction reaction = commentService.addReaction(commentId, reactionType, userName);
            List<CommentReaction> reactions = commentService.getReactionsByCommentId(commentId);
            
            response.put("success", true);
            response.put("reactions", reactions);
            response.put("added", reaction != null);
            
            // Count reactions by type
            Map<String, Long> reactionCounts = new HashMap<>();
            for (ReactionType type : ReactionType.values()) {
                reactionCounts.put(type.name(), commentService.getReactionCount(commentId, type));
            }
            response.put("reactionCounts", reactionCounts);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }

    @GetMapping("/api/article/{articleId}")
    @ResponseBody
    public List<Comment> getCommentsByArticle(@PathVariable Long articleId) {
        return commentService.getCommentsByArticleId(articleId);
    }

    @GetMapping("/api/parent/{articleId}")
    @ResponseBody
    public List<Comment> getParentCommentsByArticle(@PathVariable Long articleId) {
        return commentService.getParentCommentsByArticleId(articleId);
    }

    @GetMapping("/api/replies/{commentId}")
    @ResponseBody
    public List<Comment> getRepliesByComment(@PathVariable Long commentId) {
        return commentService.getRepliesByCommentId(commentId);
    }
}
