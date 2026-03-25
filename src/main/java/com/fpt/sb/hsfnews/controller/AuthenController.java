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
    public String login(){
        return "login";
    }

    @PostMapping()
    public String doLogin(RedirectAttributes redirectAttributes, HttpSession session, 
                          @RequestParam(required = false) String username, 
                          @RequestParam(required = false) String password){
        boolean hasError = false;

        if (username == null || username.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("usernameError", "Username is required");
            hasError = true;
        } else if (username.length() < 5 || username.length() > 50) {
            redirectAttributes.addFlashAttribute("usernameError", "The name must be from 5 to 50 characters length");
            hasError = true;
        }

        if (password == null || password.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("passwordError", "Password is required");
            hasError = true;
        } else if (password.length() < 6 || password.length() > 100) {
            redirectAttributes.addFlashAttribute("passwordError", "Password must be from 6 to 100 characters length");
            hasError = true;
        }

        if (hasError) {
            redirectAttributes.addFlashAttribute("username", username);
            return "redirect:/authen/login";
        }

        User user = authenService.login(username, password);
        if(user == null){
            redirectAttributes.addFlashAttribute("errMsg", "Wrong username or password");
            redirectAttributes.addFlashAttribute("username", username);
            return "redirect:/authen/login";
        }
        session.setAttribute("loggedInUser", user);
        return "redirect:/";
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

    @GetMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();
        return "redirect:/authen/login";
    }
}
