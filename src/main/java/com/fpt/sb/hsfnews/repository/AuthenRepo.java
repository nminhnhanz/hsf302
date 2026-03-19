package com.fpt.sb.hsfnews.repository;

import com.fpt.sb.hsfnews.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthenRepo extends JpaRepository<User,Long> {
     User findByUsernameContainingIgnoreCaseAndPassword(String username, String password);
     User findByUsername(String username);
     boolean existsByUsername(String username);
     boolean existsByEmail(String email);

}
