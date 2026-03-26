package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.Article;
import com.fpt.sb.hsfnews.entity.ArticleStatus;
import com.fpt.sb.hsfnews.entity.Comment;
import com.fpt.sb.hsfnews.entity.Notification;
import com.fpt.sb.hsfnews.entity.Role;
import com.fpt.sb.hsfnews.entity.User;
import com.fpt.sb.hsfnews.repository.ArticleRepository;
import com.fpt.sb.hsfnews.repository.CommentRepository;
import com.fpt.sb.hsfnews.repository.NotificationRepository;
import com.fpt.sb.hsfnews.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.seed-demo-data=false"
})
class CommentRealtimeServiceIntegrationTest {

    @Autowired
    private CommentRealtimeService commentRealtimeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void cleanTables() {
        notificationRepository.deleteAll();
        commentRepository.deleteAll();
        articleRepository.deleteAll();
    }

    @Test
    void publishCommentCreated_adminRepliesMember_sendsPersonalNotificationWithCommentTarget() {
        User admin = createUser("admin_it_", Role.ADMIN);
        User member02 = createUser("member02_it_", Role.MEMBER);

        Article article = new Article();
        article.setAuthor(admin);
        article.setTitle("Integration Test Article " + UUID.randomUUID());
        article.setContent("Body for integration testing");
        article.setStatus(ArticleStatus.PUBLISHED);
        article = articleRepository.save(article);

        Comment memberRootComment = new Comment();
        memberRootComment.setAuthor(member02);
        memberRootComment.setArticle(article);
        memberRootComment.setContent("Member root comment");
        memberRootComment = commentRepository.save(memberRootComment);

        Comment adminReply = new Comment();
        adminReply.setAuthor(admin);
        adminReply.setArticle(article);
        adminReply.setParentComment(memberRootComment);
        adminReply.setContent("@" + member02.getUsername() + " Please check this update.");
        adminReply = commentRepository.save(adminReply);

        commentRealtimeService.publishCommentCreated(adminReply);

        List<Notification> memberNotifications = notificationRepository
                .findTop30ByRecipientIdOrderByCreatedAtDesc(member02.getId());

        assertThat(memberNotifications).hasSize(1);
        Notification saved = memberNotifications.get(0);
        assertThat(saved.getRecipient().getId()).isEqualTo(member02.getId());
        assertThat(saved.getType()).isEqualTo("PERSONAL");
        assertThat(saved.getActorUsername()).isEqualTo(admin.getUsername());
        assertThat(saved.getArticleId()).isEqualTo(article.getId());
        assertThat(saved.getCommentId()).isEqualTo(adminReply.getId());
        assertThat(saved.getParentCommentId()).isEqualTo(memberRootComment.getId());
    }

    private User createUser(String prefix, Role role) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User user = new User();
        user.setUsername(prefix + suffix);
        user.setPassword("pass1234");
        user.setFullName("IT User " + suffix);
        user.setEmail(prefix + suffix + "@example.com");
        user.setRole(role);
        return userRepository.save(user);
    }
}

