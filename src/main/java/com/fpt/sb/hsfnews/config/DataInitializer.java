package com.fpt.sb.hsfnews.config;

import com.fpt.sb.hsfnews.entity.Role;
import com.fpt.sb.hsfnews.entity.User;
import com.fpt.sb.hsfnews.repository.AuthenRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private AuthenRepo authenRepo;


    @Override
    public void run(String... args) throws Exception {
        //điều kiện có khi bên application.properties có update để tránh trùng key id
        //xài create thì xóa điều kiện
        if(!authenRepo.existsByUsername("nguyena")) {
            User ad = User.builder().
                    username("nguyena").
                    password("123456").
                    fullName("Nguyễn Ánh").
                    role(Role.ADMIN).
                    email("nguyena@gmail.com").
                    createdAt(LocalDateTime.now())
                    .build();
            authenRepo.save(ad);
        }
    }
}
