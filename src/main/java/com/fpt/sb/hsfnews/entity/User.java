package com.fpt.sb.hsfnews.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, columnDefinition = "nvarchar(50)")
    @Size(min = 5, max = 50, message = "The name must be from 5 to 50 characters length")
    @NotBlank(message = "Username is required")
    private String username;
    @Column(nullable = false, columnDefinition = "varchar(100)")
    @Size(min = 6, max = 50, message = "Password must be from 6 to 100 characters length")
    @NotBlank(message = "Password is required")
    private String password;
    @Column(nullable = false, columnDefinition = "nvarchar(50)")
    @Size(min = 5, max = 50, message = "The name must be from 5 to 50 characters length")
    @NotBlank(message = "Fullname is required")
    private String fullName;
    @Column(nullable = false, columnDefinition = "varchar(100)")
    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Role is required")
    private Role role;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "author")
    private List<Article> articles;

    @PrePersist
    public void prePersist(){
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Article> getArticles() {
        return articles;
    }

    public void setArticles(List<Article> articles) {
        this.articles = articles;
    }
}