package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.User;
import com.fpt.sb.hsfnews.repository.AuthenRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenServiceImpl implements AuthenService {
    private final AuthenRepo authenRepo;

    @Override
    public User login(String username, String password) {
        return authenRepo.findByUsernameContainingIgnoreCaseAndPassword(username, password);
    }

    @Override
    public User register(User user) {
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
