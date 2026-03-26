package com.fpt.sb.hsfnews.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.Authentication;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Giả sử bạn đã có class này để load user từ DB
    // private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Cấu hình CSRF (Disable nếu làm API hoặc theo yêu cầu file cũ của bạn)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Cấu hình phân quyền (Authorization)
                .authorizeHttpRequests(authz -> authz
                        // Các tài nguyên tĩnh và trang công khai
                        .requestMatchers("/", "/authen/**", "/css/**", "/js/**", "/images/**", "/uploads/**", "/blogs/**", "/articles/**").permitAll()
                        .requestMatchers("/comments/api/**").permitAll()

                        // Phân quyền cho Admin
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Tất cả các request khác cần đăng nhập
                        .anyRequest().authenticated()
                )

                // 3. Cấu hình Form Login
                .formLogin(form -> form
                        .loginPage("/authen/login")
                        .loginProcessingUrl("/authen/login") // URL để Spring xử lý login
                        .failureUrl("/authen/login?error=true")
                        // Xử lý điều hướng sau khi login thành công tùy theo Role
                        .successHandler(this::onAuthenticationSuccess)
                        .permitAll()
                )

                // 4. Cấu hình Logout
                .logout(logout -> logout
                        .logoutUrl("/authen/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * Helper xử lý điều hướng sau khi đăng nhập thành công
     */
    private void onAuthenticationSuccess(jakarta.servlet.http.HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        response.sendRedirect(isAdmin ? "/admin/articles" : "/");
    }
}