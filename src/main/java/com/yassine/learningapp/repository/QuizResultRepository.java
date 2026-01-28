package com.yassine.learningapp.repository;

import com.yassine.learningapp.entity.QuizResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizResultRepository extends JpaRepository<QuizResult,Long> {
    Page<QuizResult> findByUserId(Long quizId, Pageable pageable);
}
