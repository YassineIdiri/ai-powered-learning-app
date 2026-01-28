package com.yassine.learningapp.repository;

import com.yassine.learningapp.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion,Long> {
    List<QuizQuestion> findByQuizIdOrderByQuestionOrderAsc(Long quizId);
    long countByQuizId(Long quizId);
}
