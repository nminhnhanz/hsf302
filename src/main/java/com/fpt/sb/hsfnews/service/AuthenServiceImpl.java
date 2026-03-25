package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.User;
import com.fpt.sb.hsfnews.repository.AuthenRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenServiceImpl implements AuthenService {
    private final AuthenRepo authenRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User login(String username, String password) {
        User user = authenRepo.findByUsernameIgnoreCase(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }

    @Override
    public User register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return authenRepo.save(user);
    }



    @Override
    public boolean isUserExist(String username) {
        return authenRepo.existsByUsername(username);
    }

    @Override
    public boolean isEmailExist(String email) {
        return authenRepo.existsByEmail(email);
    }
}
