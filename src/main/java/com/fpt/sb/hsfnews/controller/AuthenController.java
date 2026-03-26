package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.entity.Role;
import com.fpt.sb.hsfnews.entity.User;
import com.fpt.sb.hsfnews.service.AuthenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Objects;

@Controller
@RequestMapping("/authen")
@RequiredArgsConstructor
public class AuthenController {

    private final AuthenService authenService;

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("uObj")) {
            User user = new User();
            // Thiết lập mặc định cho user mới
            user.setRole(Role.MEMBER);
            model.addAttribute("uObj", user);
        }
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute("uObj") User user,
                             BindingResult result,
                             @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        // 1. Kiểm tra lỗi validate cơ bản từ Entity (Annotation)
        if (result.hasErrors()) {
            return "register";
        }

        // 2. Kiểm tra khớp mật khẩu
        if (confirmPassword == null || confirmPassword.isBlank()) {
            result.rejectValue("password", "error.user", "Password confirmation is required");
        } else if (!Objects.equals(user.getPassword(), confirmPassword)) {
            result.rejectValue("password", "error.user", "Password and confirm password do not match");
        }

        // 3. Kiểm tra Email/Username tồn tại (gọi xuống Service)
        if (authenService.existsByEmail(user.getEmail())) {
            result.rejectValue("email", "error.user", "Email already exists");
        }

        if (authenService.existsByUsername(user.getUsername())) {
            result.rejectValue("username", "error.user", "Username already exists");
        }

        // 4. Nếu có bất kỳ lỗi nào ở trên, quay lại trang register
        if (result.hasErrors()) {
            return "register";
        }

        // 5. Thực hiện đăng ký
        user.setRole(Role.MEMBER);
        user.setCreatedAt(LocalDateTime.now());
        authenService.register(user);

        redirectAttributes.addFlashAttribute("message", "Registration successful! Please login.");
        return "redirect:/authen/login";
    }
}