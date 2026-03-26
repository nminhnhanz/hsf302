package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.User;


public interface AuthenService {
        public User login(String username, String password);
        public User register(User user);
        public boolean isUserExist(String username);
        public boolean isEmailExist(String email);

        default boolean existsByUsername(String username) {
                return isUserExist(username);
        }

        default boolean existsByEmail(String email) {
                return isEmailExist(email);
        }



}
