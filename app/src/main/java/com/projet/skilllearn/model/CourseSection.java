package com.projet.skilllearn.model;

public class CourseSection {
    private String sectionId;
    private String courseId;  // Ajout de cette propriété importante
    private String title;
    private String description;
    private String content;
    private String videoUrl;
    private Quiz quiz;
    private String quizId;
    private int durationMinutes;
    private int orderIndex;

    // Constructeur par défaut pour Firebase
    public CourseSection() {
    }

    public CourseSection(String sectionId, String courseId, String title, String description) {
        this.sectionId = sectionId;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
    }

    // Getters et setters
    public String getSectionId() {
        return sectionId;
    }

    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }

    // Ajout des getter et setter pour courseId
    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }
    
    public String getQuizId() {
        return quizId;
    }
    
    public void setQuizId(String quizId) {
        this.quizId = quizId;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}