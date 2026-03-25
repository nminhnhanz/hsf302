package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.entity.Role;
import com.fpt.sb.hsfnews.entity.User;
import com.fpt.sb.hsfnews.service.AuthenService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/authen")
@RequiredArgsConstructor
public class AuthenController {
    private final AuthenService authenService;

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "logout", required = false) String logout,
                       Model model){
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        return "login";
    }



    @GetMapping("/register")
    public String register(Model box){
        if (!box.containsAttribute("uObj")) {
            User user = new User();
            user.setRole(Role.EDITOR);
            user.setCreatedAt(LocalDateTime.now());
            box.addAttribute("uObj", user);
        }
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute("uObj") User user, BindingResult result, RedirectAttributes redirectAttributes){
        if (authenService.isUserExist(user.getUsername())) {
            result.rejectValue("username", "error.user", "Username already exists");
        }
        if (authenService.isEmailExist(user.getEmail())) {
            result.rejectValue("email", "e rror.user", "Email already exists");
        }

        if(result.hasErrors()){
            // Pass the model object and its BindingResult validation states over to the redirect
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.uObj", result);
            redirectAttributes.addFlashAttribute("uObj", user);
            redirectAttributes.addFlashAttribute("errMsg", "Please fix the errors below");
            return "redirect:/authen/register";
        }
        
        authenService.register(user);
        return "redirect:/authen/login";
    }
}
