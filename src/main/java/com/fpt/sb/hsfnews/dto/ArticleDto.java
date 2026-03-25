package com.fpt.sb.hsfnews.dto;

import com.fpt.sb.hsfnews.entity.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleDto {
    
    private Long id;
    
    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;
    
    @Size(max = 500, message = "Summary must not exceed 500 characters")
    private String summary;
    
    @NotBlank(message = "Content is required")
    private String content;
    
    private String thumbnail;
    
    private Long categoryId;
    
    private String tags;
    
    private ArticleStatus status;
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
}
