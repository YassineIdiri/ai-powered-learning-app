package com.yassine.learningapp.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table( name = "quiz_results")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QuizResult extends BaseEntity {
    private Integer score;
    private Integer questionCount;
    private Double timeTakenSeconds;
    private Double percentageScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @PrePersist
    @PreUpdate
    public void calculatePercentageScore() {
        if(questionCount!=null && score!=null && questionCount > 0){
            this.percentageScore = (score * 100.0) / questionCount ;
        }
    }

}
