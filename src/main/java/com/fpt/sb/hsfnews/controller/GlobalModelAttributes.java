package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.entity.Notification;
import com.fpt.sb.hsfnews.service.CategoryService;
import com.fpt.sb.hsfnews.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalModelAttributes {

    private final CategoryService categoryService;
    private final NotificationService notificationService;

    public GlobalModelAttributes(CategoryService categoryService,
                                 NotificationService notificationService) {
        this.categoryService = categoryService;
        this.notificationService = notificationService;
    }

    @ModelAttribute("categories")
    public Object categories() {
        return categoryService.findAllForHeader();
    }

    @ModelAttribute("headerNotifications")
    public List<Notification> headerNotifications(Authentication authentication) {
        if (authentication == null) {
            return List.of();
        }
        return notificationService.getRecentNotificationsForUser(authentication.getName());
    }

    @ModelAttribute("headerUnreadCount")
    public long headerUnreadCount(Authentication authentication) {
        if (authentication == null) {
            return 0;
        }
        return notificationService.getUnreadCount(authentication.getName());
    }
}

