package com.fpt.sb.hsfnews.controller;

import com.fpt.sb.hsfnews.service.AdminDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@org.springframework.web.bind.annotation.RequestParam(name = "mode", required = false) String mode,
                            @org.springframework.web.bind.annotation.RequestParam(name = "year", required = false) Integer year,
                            @org.springframework.web.bind.annotation.RequestParam(name = "compareYearA", required = false) Integer compareYearA,
                            @org.springframework.web.bind.annotation.RequestParam(name = "compareYearB", required = false) Integer compareYearB,
                            Model model) {
        model.addAttribute("dashboard", adminDashboardService.buildDashboardData(mode, year, compareYearA, compareYearB));
        return "admin/dashboard";
    }
}




