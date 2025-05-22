package com.projet.skilllearn.model;

import java.util.List;

public class Quiz {
    private String quizId;
    private String title;
    private List<Question> questions;
    private int passingScore;
    private boolean required;
    private String sectionId;

    // Default constructor for Firebase
    public Quiz() {
    }

    public Quiz(String quizId, String title, List<Question> questions, int passingScore, boolean required) {
        this.quizId = quizId;
        this.title = title;
        this.questions = questions;
        this.passingScore = passingScore;
        this.required = required;
    }

    // Getters and setters
    public String getQuizId() {
        return quizId;
    }

    public void setQuizId(String quizId) {
        this.quizId = quizId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public int getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(int passingScore) {
        this.passingScore = passingScore;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
    
    public String getSectionId() {
        return sectionId;
    }
    
    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }
}