package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.Comment;
import com.fpt.sb.hsfnews.entity.Notification;
import com.fpt.sb.hsfnews.entity.Role;
import com.fpt.sb.hsfnews.entity.User;
import com.fpt.sb.hsfnews.repository.CommentRepository;
import com.fpt.sb.hsfnews.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CommentRealtimeService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9._-]{3,100})");

    private final NotificationService notificationService;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentRealtimeService(NotificationService notificationService,
                                  CommentRepository commentRepository,
                                  UserRepository userRepository) {
        this.notificationService = notificationService;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    public void publishCommentCreated(Comment comment) {
        if (comment == null || comment.getId() == null) {
            return;
        }

        Comment hydrated = commentRepository.findHydratedById(comment.getId()).orElse(null);
        if (hydrated == null || hydrated.getArticle() == null || hydrated.getAuthor() == null) {
            return;
        }

        notifyUsers(hydrated);
    }

    private void notifyUsers(Comment newComment) {
        User actor = newComment.getAuthor();
        User articleOwner = newComment.getArticle() != null ? newComment.getArticle().getAuthor() : null;
        User rootCommentAuthor = resolveRootCommentAuthor(newComment.getParentComment());
        User parentCommentAuthor = newComment.getParentComment() != null ? newComment.getParentComment().getAuthor() : null;

        Map<Long, NotificationDispatch> recipients = new LinkedHashMap<>();

        User immediateRecipient = parentCommentAuthor != null ? parentCommentAuthor : rootCommentAuthor;
        if (isNotifyTarget(immediateRecipient, actor)) {
            recipients.put(immediateRecipient.getId(), new NotificationDispatch(
                    immediateRecipient,
                    buildPersonalReplyPayload(newComment, actor)
            ));
        }

        if (isNotifyTarget(rootCommentAuthor, actor)
                && !rootCommentAuthor.getId().equals(immediateRecipient.getId())) {
            recipients.put(rootCommentAuthor.getId(), new NotificationDispatch(
                    rootCommentAuthor,
                    buildPersonalReplyPayload(newComment, actor)
            ));
        }

        // Notify mentioned users as explicit direct targets, including admin->member mentions.
        for (User mentioned : resolveMentionedUsers(newComment.getContent())) {
            if (!isNotifyTarget(mentioned, actor) || recipients.containsKey(mentioned.getId())) {
                continue;
            }
            recipients.put(mentioned.getId(), new NotificationDispatch(
                    mentioned,
                    buildMentionPayload(newComment, actor)
            ));
        }

        boolean suppressAdminNotificationForMemberThread = newComment.getParentComment() != null
                && isAdmin(articleOwner)
                && isMember(actor)
                && isMember(rootCommentAuthor)
                && isMember(parentCommentAuthor);

        if (!suppressAdminNotificationForMemberThread
                && articleOwner != null
                && actor != null
                && !articleOwner.getId().equals(actor.getId())
                && !recipients.containsKey(articleOwner.getId())) {
            String title = newComment.getArticle() != null ? newComment.getArticle().getTitle() : "your article";
            recipients.put(articleOwner.getId(), new NotificationDispatch(
                    articleOwner,
                    new UserNotificationPayload(
                            "ADMIN",
                            "New comment activity on '" + title + "'",
                            safeUsername(actor),
                            newComment.getArticle().getId(),
                            newComment.getId(),
                            newComment.getParentComment() != null ? newComment.getParentComment().getId() : null,
                            null,
                            0,
                            LocalDateTime.now()
                    )
            ));
        }

        for (Map.Entry<Long, NotificationDispatch> entry : recipients.entrySet()) {
            User target = entry.getValue().target();
            UserNotificationPayload payloadData = entry.getValue().payload();
            if (target == null || target.getUsername() == null) {
                continue;
            }

            String destination = "PERSONAL".equals(payloadData.type())
                    ? "/queue/notifications"
                    : "/queue/admin-notifications";
            Notification saved = notificationService.createNotification(
                    target,
                    payloadData.type(),
                    payloadData.message(),
                    payloadData.actorUsername(),
                    payloadData.articleId(),
                    payloadData.commentId(),
                    payloadData.parentCommentId()
            );
            long unreadCount = notificationService.getUnreadCount(target.getUsername());
            UserNotificationPayload payload = new UserNotificationPayload(
                    payloadData.type(),
                    payloadData.message(),
                    payloadData.actorUsername(),
                    payloadData.articleId(),
                    payloadData.commentId(),
                    payloadData.parentCommentId(),
                    saved.getId(),
                    unreadCount,
                    payloadData.createdAt()
            );
            // WebSocket is removed: notifications are persisted and served via /notifications/api polling.
        }
    }

    private boolean isNotifyTarget(User target, User actor) {
        return target != null && actor != null && !target.getId().equals(actor.getId());
    }

    private UserNotificationPayload buildPersonalReplyPayload(Comment newComment, User actor) {
        String title = newComment.getArticle() != null ? newComment.getArticle().getTitle() : "an article";
        String replyMsg = "You have a new reply in '" + title + "'";
        if (newComment.getParentComment() != null && newComment.getParentComment().getContent() != null) {
            String pContent = newComment.getParentComment().getContent();
            if (pContent.length() > 30) {
                pContent = pContent.substring(0, 30) + "...";
            }
            replyMsg = "Replied to your comment: \"" + pContent + "\" in '" + title + "'";
        }

        return new UserNotificationPayload(
                "PERSONAL",
                replyMsg,
                safeUsername(actor),
                newComment.getArticle().getId(),
                newComment.getId(),
                newComment.getParentComment() != null ? newComment.getParentComment().getId() : null,
                null,
                0,
                LocalDateTime.now()
        );
    }

    private UserNotificationPayload buildMentionPayload(Comment newComment, User actor) {
        String title = newComment.getArticle() != null ? newComment.getArticle().getTitle() : "an article";
        return new UserNotificationPayload(
                "PERSONAL",
                "You were mentioned in '" + title + "'",
                safeUsername(actor),
                newComment.getArticle().getId(),
                newComment.getId(),
                newComment.getParentComment() != null ? newComment.getParentComment().getId() : null,
                null,
                0,
                LocalDateTime.now()
        );
    }

    private User resolveRootCommentAuthor(Comment parent) {
        if (parent == null) {
            return null;
        }

        Long cursorId = parent.getId();
        User rootAuthor = null;
        while (cursorId != null) {
            Comment cursor = commentRepository.findWithAuthorAndParentById(cursorId).orElse(null);
            if (cursor == null) {
                break;
            }
            rootAuthor = cursor.getAuthor();
            cursorId = cursor.getParentComment() != null ? cursor.getParentComment().getId() : null;
        }
        return rootAuthor;
    }

    private Set<User> resolveMentionedUsers(String content) {
        Set<User> users = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return users;
        }

        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String username = matcher.group(1);
            userRepository.findByUsernameIgnoreCase(username).ifPresent(users::add);
        }
        return users;
    }

    private String safeUsername(User user) {
        return user != null && user.getUsername() != null ? user.getUsername() : "unknown";
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    private boolean isMember(User user) {
        return user != null && user.getRole() == Role.MEMBER;
    }

    public record UserNotificationPayload(String type,
                                          String message,
                                          String actorUsername,
                                          Long articleId,
                                          Long commentId,
                                          Long parentCommentId,
                                          Long notificationId,
                                          long unreadCount,
                                          LocalDateTime createdAt) {
    }

    private record NotificationDispatch(User target, UserNotificationPayload payload) {
    }
}

