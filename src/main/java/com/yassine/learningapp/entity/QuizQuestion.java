package com.yassine.learningapp.entity;


import jakarta.persistence.*;

@Entity
@Table( name = "quiz_questions")
public class QuizQuestion extends BaseEntity {
    private String question;
    private String answer;

    @Column( name = "option_a", nullable = false)
    private String optionA;

    @Column( name = "option_b", nullable = false)
    private String optionB;

    @Column( name = "option_c", nullable = false)
    private String optionC;

    @Column( name = "option_d", nullable = false)
    private String optionD;

    private String explanation;

    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer; // "A", "B", "C", ou "D"

    private Integer questionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;
}