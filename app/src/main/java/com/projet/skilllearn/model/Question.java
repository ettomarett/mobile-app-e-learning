package com.projet.skilllearn.model;

import java.util.List;

/**
 * Model class for quiz questions
 */
public class Question {
    private String questionId;
    private String question;
    private List<String> options;
    private int correctOptionIndex;
    private String explanation;

    // Default constructor for Firebase
    public Question() {
    }

    public Question(String questionId, String question, List<String> options, int correctOptionIndex, String explanation) {
        this.questionId = questionId;
        this.question = question;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.explanation = explanation;
    }

    // Getters and setters
    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public void setCorrectOptionIndex(int correctOptionIndex) {
        this.correctOptionIndex = correctOptionIndex;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
} 