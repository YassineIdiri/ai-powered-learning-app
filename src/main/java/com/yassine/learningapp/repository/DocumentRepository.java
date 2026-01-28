package com.yassine.learningapp.repository;

import com.yassine.learningapp.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Page<Document> findByUserId(Long userId, Pageable pageable);

    Optional<Document> findById(Long id);

    Page<Document> findByUserIdTitleContainingIgnoreCase(Long id, String title, Pageable pageable);

    @Query("SELECT SUM(d.fileSize) FROM Document d WHERE d.user.id = :userId")
    Double countTotalSize(Long userId);

    List<Document> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
}
