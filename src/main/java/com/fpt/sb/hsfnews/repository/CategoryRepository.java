package com.fpt.sb.hsfnews.repository;

import com.fpt.sb.hsfnews.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Tìm kiếm chính xác theo tên (không phân biệt hoa thường)
    Optional<Category> findByNameIgnoreCase(String name);

    // Tìm kiếm theo từ khóa trong tên (không phân biệt hoa thường) có phân trang
    // Lưu ý: Nên trả về Page<Category> thay vì List<Category> khi dùng Pageable
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Nếu bạn muốn trả về List đơn thuần không dùng phân trang
    List<Category> findByNameContainingIgnoreCase(String name);

    @Query("""
            SELECT c
            FROM Category c
            LEFT JOIN c.articles a
            GROUP BY c
            ORDER BY COUNT(a) DESC, c.name ASC
            """)
    List<Category> findTopByArticleCount(Pageable pageable);
}