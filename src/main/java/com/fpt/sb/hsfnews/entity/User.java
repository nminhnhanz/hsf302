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
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
        createdAt = LocalDateTime.now();
    }

    // getters setters
}