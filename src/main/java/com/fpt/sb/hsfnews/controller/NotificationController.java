package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.entity.Notification;
import com.fpt.sb.hsfnews.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/notifications/api")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationListResponse getNotifications(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String username = authentication.getName();
        List<NotificationItemResponse> items = notificationService.getRecentNotificationsForUser(username)
                .stream()
                .map(this::toResponse)
                .toList();
        long unreadCount = notificationService.getUnreadCount(username);
        return new NotificationListResponse(items, unreadCount);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationReadResponse> markAsRead(@PathVariable("id") Long notificationId,
                                                               Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String username = authentication.getName();
        Notification notification = notificationService.markAsRead(notificationId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        long unreadCount = notificationService.getUnreadCount(username);
        return ResponseEntity.ok(new NotificationReadResponse(notification.getId(), true, unreadCount));
    }

    private NotificationItemResponse toResponse(Notification n) {
        return new NotificationItemResponse(
                n.getId(),
                n.getType(),
                n.getMessage(),
                n.getActorUsername(),
                n.getArticleId(),
                n.getCommentId(),
                n.getParentCommentId(),
                n.getCreatedAt(),
                n.isRead());
    }

    public record NotificationItemResponse(Long id,
                                           String type,
                                           String message,
                                           String actorUsername,
                                           Long articleId,
                                           Long commentId,
                                           Long parentCommentId,
                                           LocalDateTime createdAt,
                                           boolean read) {
    }

    public record NotificationListResponse(List<NotificationItemResponse> items,
                                           long unreadCount) {
    }

    public record NotificationReadResponse(Long id,
                                           boolean read,
                                           long unreadCount) {
    }
}

