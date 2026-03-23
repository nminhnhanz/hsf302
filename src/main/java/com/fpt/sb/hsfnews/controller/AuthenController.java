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
    public String doLogin(HttpSession session, RedirectAttributes redirectAttributes, @RequestParam String username, @RequestParam String password){
        User user = authenService.login(username, password);
        if(user == null){
            redirectAttributes.addFlashAttribute("errMsg", "Wrong username or password");
            return "redirect:/authen/login";
        }
        session.setAttribute("loggedInUser", user);
        return "redirect:/";
    }



    @GetMapping("/register")
    public String register(Model box){
        User user = new  User();
        user.setRole(Role.EDITOR);
        user.setCreatedAt(LocalDateTime.now());
        box.addAttribute("uObj", user);
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute("uObj") User user, Model box , BindingResult result){
        if (authenService.isUserExist(user.getUsername())) {
            box.addAttribute("error", "Username already exists");
            return "register";
        }else if (authenService.isEmailExist(user.getEmail())) {
            box.addAttribute("error", "Email already exists");
            return "register";
        }

        if(result.hasErrors()){
            box.addAttribute("uObj", user);
            box.addAttribute("errMsg", "Please fix the errors below");
            return "register";
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
