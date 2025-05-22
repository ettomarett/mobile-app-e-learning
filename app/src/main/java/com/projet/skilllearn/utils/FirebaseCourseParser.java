package com.projet.skilllearn.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.projet.skilllearn.model.Course;
import com.projet.skilllearn.model.CourseSection;
import com.projet.skilllearn.model.Question;
import com.projet.skilllearn.model.Quiz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing LLM-generated course content and uploading it to Firebase
 */
public class FirebaseCourseParser {
    private static final String TAG = "FirebaseCourseParser";

    /**
     * Parse LLM output containing course content and upload it to Firebase
     * @param llmOutput LLM response containing course content
     * @param callback Callback to notify about success or failure
     */
    public static void parseAndUploadCourse(String llmOutput, FirebaseCallback callback) {
        try {
            // Extract course content between <FirebaseCourse> tags
            Pattern coursePattern = Pattern.compile("<FirebaseCourse>(.*?)</FirebaseCourse>", Pattern.DOTALL);
            Matcher courseMatcher = coursePattern.matcher(llmOutput);
            
            if (!courseMatcher.find()) {
                callback.onError("No valid course content found");
                return;
            }
            
            String courseContent = courseMatcher.group(1);
            
            // Parse course details
            Course course = parseCourseDetails(courseContent);
            if (course == null) {
                callback.onError("Failed to parse course details");
                return;
            }
            
            // Parse sections
            List<CourseSection> sections = parseSections(courseContent);
            if (sections.isEmpty()) {
                callback.onError("No sections found in course content");
                return;
            }
            
            // Parse quizzes
            Map<String, Quiz> quizzes = parseQuizzes(courseContent);
            
            // Validate that we have quizzes
            if (quizzes.isEmpty()) {
                callback.onError("No quizzes found in course content - quizzes are mandatory");
                return;
            }
            
            // Check if all quizzes have questions
            boolean allQuizzesHaveQuestions = true;
            for (Map.Entry<String, Quiz> entry : quizzes.entrySet()) {
                Quiz quiz = entry.getValue();
                if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
                    allQuizzesHaveQuestions = false;
                    Log.w(TAG, "Quiz " + quiz.getTitle() + " has no questions");
                }
            }
            
            if (!allQuizzesHaveQuestions) {
                callback.onError("One or more quizzes don't have any questions - all quizzes must have questions");
                return;
            }
            
            // Ensure each section has a quiz by matching titles
            boolean allSectionsHaveQuiz = associateQuizzesWithSections(sections, quizzes);
            
            if (!allSectionsHaveQuiz) {
                callback.onError("One or more sections don't have an associated quiz - each section must have a quiz");
                return;
            }
            
            // Upload to Firebase
            uploadToFirebase(course, sections, quizzes, callback);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing course", e);
            callback.onError("Error parsing course: " + e.getMessage());
        }
    }
    
    /**
     * Parse course details from the content
     */
    private static Course parseCourseDetails(String content) {
        Course course = new Course();
        
        Pattern detailsPattern = Pattern.compile("<CourseDetails>(.*?)</CourseDetails>", Pattern.DOTALL);
        Matcher detailsMatcher = detailsPattern.matcher(content);
        
        if (detailsMatcher.find()) {
            String details = detailsMatcher.group(1);
            
            // Extract course properties
            course.setTitle(extractProperty(details, "title"));
            course.setDescription(extractProperty(details, "description"));
            course.setCategory(extractProperty(details, "category"));
            course.setLevel(extractProperty(details, "level"));
            
            String durationStr = extractProperty(details, "durationMinutes");
            if (durationStr != null && !durationStr.isEmpty()) {
                try {
                    course.setDurationMinutes(Integer.parseInt(durationStr.trim()));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid duration format: " + durationStr, e);
                }
            }
            
            String tagsStr = extractProperty(details, "tags");
            if (tagsStr != null && !tagsStr.isEmpty()) {
                List<String> tags = Arrays.asList(tagsStr.split("\\s*,\\s*"));
                course.setTags(tags);
            }
            
            course.setImageUrl(extractProperty(details, "imageUrl"));
            
            // Set timestamps
            long now = System.currentTimeMillis();
            course.setCreatedAt(now);
            course.setUpdatedAt(now);
            
            // Set author details from current user
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                course.setAuthor(currentUser.getUid());
                course.setAuthorName(currentUser.getDisplayName() != null ? 
                        currentUser.getDisplayName() : "SkillLearn User");
            } else {
                // Default author if no one is logged in
                course.setAuthor("system");
                course.setAuthorName("SkillLearn AI");
            }
            
            // Initialize other fields
            course.setEnrolledCount(0);
        }
        
        return course;
    }
    
    /**
     * Parse sections from the content
     */
    private static List<CourseSection> parseSections(String content) {
        List<CourseSection> sections = new ArrayList<>();
        
        Pattern sectionPattern = Pattern.compile("<Section>(.*?)</Section>", Pattern.DOTALL);
        Matcher sectionMatcher = sectionPattern.matcher(content);
        
        while (sectionMatcher.find()) {
            String sectionContent = sectionMatcher.group(1);
            CourseSection section = new CourseSection();
            
            section.setTitle(extractProperty(sectionContent, "title"));
            section.setDescription(extractProperty(sectionContent, "description"));
            
            String durationStr = extractProperty(sectionContent, "durationMinutes");
            if (durationStr != null && !durationStr.isEmpty()) {
                try {
                    section.setDurationMinutes(Integer.parseInt(durationStr.trim()));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid section duration format: " + durationStr, e);
                }
            }
            
            String orderStr = extractProperty(sectionContent, "orderIndex");
            if (orderStr != null && !orderStr.isEmpty()) {
                try {
                    section.setOrderIndex(Integer.parseInt(orderStr.trim()));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid order index format: " + orderStr, e);
                    section.setOrderIndex(sections.size()); // Fallback to current size
                }
            } else {
                section.setOrderIndex(sections.size()); // Default to current size
            }
            
            section.setContent(extractProperty(sectionContent, "content"));
            section.setVideoUrl(extractProperty(sectionContent, "videoUrl"));
            
            // Generate a temporary section ID that will be replaced during upload
            section.setSectionId("temp_section_" + UUID.randomUUID().toString().substring(0, 8));
            
            sections.add(section);
        }
        
        return sections;
    }
    
    /**
     * Parse quizzes from the content
     */
    private static Map<String, Quiz> parseQuizzes(String content) {
        Map<String, Quiz> quizzes = new HashMap<>();
        
        Pattern quizPattern = Pattern.compile("<Quiz>(.*?)</Quiz>", Pattern.DOTALL);
        Matcher quizMatcher = quizPattern.matcher(content);
        
        while (quizMatcher.find()) {
            String quizContent = quizMatcher.group(1);
            Quiz quiz = new Quiz();
            
            quiz.setTitle(extractProperty(quizContent, "title"));
            
            String passingScoreStr = extractProperty(quizContent, "passingScore");
            if (passingScoreStr != null && !passingScoreStr.isEmpty()) {
                try {
                    quiz.setPassingScore(Integer.parseInt(passingScoreStr.trim()));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid passing score format: " + passingScoreStr, e);
                    quiz.setPassingScore(70); // Default passing score
                }
            } else {
                quiz.setPassingScore(70); // Default passing score
            }
            
            // Parse questions
            List<Question> questions = parseQuestions(quizContent);
            
            // If no questions were found, add default questions
            if (questions.isEmpty()) {
                questions = createDefaultQuestions(quiz.getTitle());
                Log.w(TAG, "No questions found for quiz: " + quiz.getTitle() + ". Added default questions.");
            }
            
            quiz.setQuestions(questions);
            
            String quizId = "quiz_" + UUID.randomUUID().toString().substring(0, 8);
            quiz.setQuizId(quizId);
            quiz.setRequired(true); // Default to required
            
            quizzes.put(quizId, quiz);
        }
        
        return quizzes;
    }
    
    /**
     * Create default questions for a quiz when none are provided
     */
    private static List<Question> createDefaultQuestions(String quizTitle) {
        List<Question> defaultQuestions = new ArrayList<>();
        
        // Create 3 generic questions
        Question q1 = new Question();
        q1.setQuestionId("q1");
        q1.setQuestion("Quel est l'objectif principal de cette section?");
        q1.setOptions(Arrays.asList(
            "Comprendre les concepts de base",
            "Apprendre les techniques avancées",
            "Mémoriser des informations spécifiques",
            "Résoudre des problèmes complexes"
        ));
        q1.setCorrectOptionIndex(0);
        q1.setExplanation("L'objectif principal de cette section est de comprendre les concepts de base avant de passer aux sujets plus avancés.");
        defaultQuestions.add(q1);
        
        Question q2 = new Question();
        q2.setQuestionId("q2");
        q2.setQuestion("Quelle est la meilleure façon d'appliquer les connaissances de cette section?");
        q2.setOptions(Arrays.asList(
            "Mémoriser toutes les définitions",
            "Pratiquer avec des exercices",
            "Lire des articles supplémentaires",
            "Discuter avec d'autres étudiants"
        ));
        q2.setCorrectOptionIndex(1);
        q2.setExplanation("La pratique régulière à travers des exercices est la meilleure façon d'intégrer les connaissances acquises dans cette section.");
        defaultQuestions.add(q2);
        
        Question q3 = new Question();
        q3.setQuestionId("q3");
        q3.setQuestion("Pourquoi est-il important de maîtriser le contenu de cette section?");
        q3.setOptions(Arrays.asList(
            "Pour réussir l'examen final",
            "Pour impressionner les autres",
            "Pour construire une base solide pour les sections suivantes",
            "Ce n'est pas important"
        ));
        q3.setCorrectOptionIndex(2);
        q3.setExplanation("Maîtriser cette section est essentiel pour construire une base solide qui facilitera la compréhension des sections suivantes du cours.");
        defaultQuestions.add(q3);
        
        return defaultQuestions;
    }
    
    /**
     * Parse questions from quiz content
     */
    private static List<Question> parseQuestions(String content) {
        List<Question> questions = new ArrayList<>();
        
        Pattern questionPattern = Pattern.compile("<Question>(.*?)</Question>", Pattern.DOTALL);
        Matcher questionMatcher = questionPattern.matcher(content);
        
        while (questionMatcher.find()) {
            String questionContent = questionMatcher.group(1);
            Question question = new Question();
            
            question.setQuestion(extractProperty(questionContent, "question"));
            question.setExplanation(extractProperty(questionContent, "explanation"));
            
            String correctIndex = extractProperty(questionContent, "correctOptionIndex");
            if (correctIndex != null && !correctIndex.isEmpty()) {
                try {
                    question.setCorrectOptionIndex(Integer.parseInt(correctIndex.trim()));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid correct option index: " + correctIndex, e);
                    question.setCorrectOptionIndex(0); // Default to first option
                }
            }
            
            String optionsStr = extractProperty(questionContent, "options");
            if (optionsStr != null && !optionsStr.isEmpty()) {
                List<String> options = Arrays.asList(optionsStr.split("\\|"));
                question.setOptions(options);
            }
            
            question.setQuestionId("q" + (questions.size() + 1));
            questions.add(question);
        }
        
        return questions;
    }
    
    /**
     * Extract a property value from the content
     */
    private static String extractProperty(String content, String propertyName) {
        Pattern pattern = Pattern.compile(propertyName + ":\\s*(.*?)(?=\\n\\s*\\w+:|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return null;
    }
    
    /**
     * Upload course, sections, and quizzes to Firebase
     */
    private static void uploadToFirebase(Course course, List<CourseSection> sections, 
                                         Map<String, Quiz> quizzes, FirebaseCallback callback) {
        // Get database reference
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        
        // Generate a unique course ID
        String courseId = database.getReference("courses").push().getKey();
        if (courseId == null) {
            callback.onError("Failed to generate course ID");
            return;
        }
        
        course.setCourseId(courseId);
        
        // Upload course
        database.getReference("courses").child(courseId).setValue(course)
            .addOnSuccessListener(aVoid -> {
                // Now upload sections and quizzes
                uploadSectionsAndQuizzes(database, courseId, sections, quizzes, callback);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to upload course", e);
                callback.onError("Failed to upload course: " + e.getMessage());
            });
    }
    
    /**
     * Associate quizzes with sections based on matching titles
     * @return true if all sections have an associated quiz
     */
    private static boolean associateQuizzesWithSections(List<CourseSection> sections, Map<String, Quiz> quizzes) {
        boolean allSectionsHaveQuiz = true;
        
        // First pass - associate quizzes with sections based on matching titles
        for (CourseSection section : sections) {
            boolean foundQuiz = false;
            
            for (Map.Entry<String, Quiz> entry : quizzes.entrySet()) {
                String quizTitle = entry.getValue().getTitle().toLowerCase();
                String sectionTitle = section.getTitle().toLowerCase();
                
                // Associate quiz with section if titles match or contain each other
                if (quizTitle.contains(sectionTitle) || sectionTitle.contains(quizTitle)) {
                    section.setQuizId(entry.getKey());
                    Quiz quiz = entry.getValue();
                    quiz.setSectionId(section.getSectionId());
                    foundQuiz = true;
                    break;
                }
            }
            
            if (!foundQuiz) {
                // If we couldn't find a match by title, just assign the first available quiz
                // This is a fallback to ensure each section has a quiz
                if (!quizzes.isEmpty()) {
                    String firstQuizId = quizzes.keySet().iterator().next();
                    section.setQuizId(firstQuizId);
                    Quiz quiz = quizzes.get(firstQuizId);
                    quiz.setSectionId(section.getSectionId());
                    
                    // Update the quiz title to match the section to make it clear they're related
                    quiz.setTitle("Quiz: " + section.getTitle());
                    
                    // Remove this quiz from the map so it's not assigned to multiple sections
                    quizzes.remove(firstQuizId);
                    
                    Log.w(TAG, "No matching quiz found for section: " + section.getTitle() + ". Assigned a generic quiz.");
                } else {
                    allSectionsHaveQuiz = false;
                    Log.e(TAG, "No quiz available for section: " + section.getTitle());
                }
            }
        }
        
        return allSectionsHaveQuiz;
    }
    
    /**
     * Upload sections and quizzes to Firebase
     */
    private static void uploadSectionsAndQuizzes(FirebaseDatabase database, String courseId, 
                                              List<CourseSection> sections, Map<String, Quiz> quizzes,
                                              FirebaseCallback callback) {
        AtomicInteger completedUploads = new AtomicInteger(0);
        int totalUploads = sections.size() + quizzes.size();
        
        // If there are no sections or quizzes, report success immediately
        if (totalUploads == 0) {
            callback.onSuccess(courseId);
            return;
        }
        
        // Upload sections
        for (CourseSection section : sections) {
            // Generate section ID
            String sectionId = database.getReference("sections").push().getKey();
            if (sectionId == null) {
                Log.e(TAG, "Failed to generate section ID");
                continue;
            }
            
            section.setSectionId(sectionId);
            section.setCourseId(courseId);
            
            // Update quiz reference if this section has a quiz
            if (section.getQuizId() != null) {
                Quiz quiz = quizzes.get(section.getQuizId());
                if (quiz != null) {
                    quiz.setSectionId(sectionId);
                }
            }
            
            // Upload section
            database.getReference("sections").child(sectionId).setValue(section)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Section uploaded successfully: " + section.getTitle());
                    
                    // Check if all uploads are complete
                    if (completedUploads.incrementAndGet() == totalUploads) {
                        callback.onSuccess(courseId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload section: " + section.getTitle(), e);
                    // Still increment counter to avoid deadlock
                    if (completedUploads.incrementAndGet() == totalUploads) {
                        callback.onSuccess(courseId);
                    }
                });
        }
        
        // Upload quizzes
        for (Map.Entry<String, Quiz> entry : quizzes.entrySet()) {
            String quizId = entry.getKey();
            Quiz quiz = entry.getValue();
            
            database.getReference("quizzes").child(quizId).setValue(quiz)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Quiz uploaded successfully: " + quiz.getTitle());
                    
                    // Check if all uploads are complete
                    if (completedUploads.incrementAndGet() == totalUploads) {
                        callback.onSuccess(courseId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload quiz: " + quiz.getTitle(), e);
                    // Still increment counter to avoid deadlock
                    if (completedUploads.incrementAndGet() == totalUploads) {
                        callback.onSuccess(courseId);
                    }
                });
        }
    }
    
    /**
     * Callback interface for Firebase operations
     */
    public interface FirebaseCallback {
        void onSuccess(String courseId);
        void onError(String errorMessage);
    }
} 