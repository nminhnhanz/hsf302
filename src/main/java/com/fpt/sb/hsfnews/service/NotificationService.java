package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.Notification;
import com.fpt.sb.hsfnews.entity.User;
import com.fpt.sb.hsfnews.repository.NotificationRepository;
import com.fpt.sb.hsfnews.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Notification createNotification(User recipient,
                                           String type,
                                           String message,
                                           String actorUsername,
                                           Long articleId,
                                           Long commentId,
                                           Long parentCommentId) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setMessage(message);
        notification.setActorUsername(actorUsername);
        notification.setArticleId(articleId);
        notification.setCommentId(commentId);
        notification.setParentCommentId(parentCommentId);
        return notificationRepository.save(notification);
    }

    public List<Notification> getRecentNotificationsForUser(String username) {
        User user = findByUsername(username);
        return notificationRepository.findTop30ByRecipientIdOrderByCreatedAtDesc(user.getId());
    }

    public long getUnreadCount(String username) {
        User user = findByUsername(username);
        return notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public Optional<Notification> markAsRead(Long notificationId, String username) {
        User user = findByUsername(username);
        Optional<Notification> target = notificationRepository.findByIdAndRecipientId(notificationId, user.getId());
        target.ifPresent(notification -> {
            if (!notification.isRead()) {
                notification.setRead(true);
                notification.setReadAt(LocalDateTime.now());
                notificationRepository.save(notification);
            }
        });
        return target;
    }

    private User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}

