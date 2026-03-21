package com.fpt.sb.hsfnews.repository;

import com.fpt.sb.hsfnews.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}

