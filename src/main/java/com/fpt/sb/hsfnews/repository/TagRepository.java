package com.fpt.sb.hsfnews.repository;

import com.fpt.sb.hsfnews.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // Bổ sung import bị thiếu
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    // Tìm kiếm chính xác theo tên, không phân biệt hoa thường
    Optional<Tag> findByNameIgnoreCase(String name);

    // Tìm kiếm gần đúng theo tên kèm phân trang
    Page<Tag> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Tìm kiếm gần đúng theo tên trả về List (nếu cần)
    List<Tag> findByNameContainingIgnoreCase(String name);
}