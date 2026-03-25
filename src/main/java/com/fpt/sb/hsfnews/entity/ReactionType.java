package com.fpt.sb.hsfnews.entity;

public enum ReactionType {
    LIKE("👍"),
    LOVE("❤️"),
    LAUGH("😂"),
    SAD("😢"),
    ANGRY("😠"),
    WOW("😮");

    private final String emoji;

    ReactionType(String emoji) {
        this.emoji = emoji;
    }

    public String getEmoji() {
        return emoji;
    }
}
