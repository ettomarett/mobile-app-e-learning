package com.projet.skilllearn.utils;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.projet.skilllearn.model.Achievement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestionnaire des progrès utilisateur
 * Utilise le pattern Singleton pour un accès global
 */
public class UserProgressManager {

    private static UserProgressManager instance;
    private final FirebaseDatabase database;
    private final FirebaseAuth auth;
    private ProgressUpdateListener progressUpdateListener;
    private BadgeAwardedListener badgeAwardedListener;

    // Constants for achievement types
    public static final String ACHIEVEMENT_TYPE_MILESTONE = "milestone";
    public static final String ACHIEVEMENT_TYPE_COURSE_COMPLETION = "course_completion";
    public static final String ACHIEVEMENT_TYPE_STREAK = "streak";
    public static final String ACHIEVEMENT_TYPE_SPECIAL = "special";
    public static final String ACHIEVEMENT_TYPE_CATEGORY_MASTER = "category_master";

    /**
     * Interface pour les notifications d'obtention de badge
     */
    public interface BadgeAwardedListener {
        void onBadgeAwarded(Achievement achievement);
    }

    /**
     * Interface pour les notifications de mise à jour de progrès
     */
    public interface ProgressUpdateListener {
        void onProgressUpdated(String courseId, int percentage);
    }

    /**
     * Interface pour récupérer le progrès d'un cours
     */
    public interface CourseProgressCallback {
        void onProgressLoaded(int percentage);
        void onError(String errorMessage);
    }

    /**
     * Interface pour récupérer les cours en cours
     */
    public interface UserCoursesCallback {
        void onCoursesLoaded(List<String> courseIds);
        void onError(String errorMessage);
    }

    /**
     * Interface pour récupérer les succès
     */
    public interface AchievementsCallback {
        void onAchievementsLoaded(List<Achievement> achievements);
        void onError(String errorMessage);
    }

    /**
     * Constructeur privé (pattern Singleton)
     */
    private UserProgressManager() {
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /**
     * Obtient l'instance unique
     * @return l'instance de UserProgressManager
     */
    public static synchronized UserProgressManager getInstance() {
        if (instance == null) {
            instance = new UserProgressManager();
        }
        return instance;
    }

    /**
     * Définit un écouteur pour les mises à jour de progrès
     * @param listener l'écouteur
     */
    public void setProgressUpdateListener(ProgressUpdateListener listener) {
        this.progressUpdateListener = listener;
    }

    /**
     * Définit un écouteur pour les badges obtenus
     * @param listener l'écouteur
     */
    public void setBadgeAwardedListener(BadgeAwardedListener listener) {
        this.badgeAwardedListener = listener;
    }

    /**
     * Initialise les données de progression pour un nouvel utilisateur
     * @param userId ID de l'utilisateur
     */
    public void initializeUserProgress(String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        DatabaseReference userProgressRef = database.getReference("user_progress").child(userId);

        // Vérifier si l'utilisateur a déjà des données de progression
        userProgressRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Créer un nœud vide pour la progression de l'utilisateur
                    userProgressRef.setValue(new HashMap<>());

                    // Créer un badge pour le premier jour
                    Achievement firstDayAchievement = new Achievement(
                            "first_day",
                            "Premier jour",
                            "Bienvenue sur SkillLearn !",
                            ACHIEVEMENT_TYPE_MILESTONE,
                            System.currentTimeMillis()
                    );
                    firstDayAchievement.setIconUrl("https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Ffirst_day.png");

                    // Ajouter le badge
                    addAchievement(firstDayAchievement);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Gérer l'erreur si nécessaire
                Log.e(TAG, "Error initializing user progress", error.toException());
            }
        });
    }

    /**
     * Met à jour la progression d'un cours
     * @param courseId ID du cours
     * @param percentage pourcentage de progression (0-100)
     */
    public void updateCourseProgress(String courseId, int percentage) {
        // Vérifier si l'utilisateur est connecté
        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DatabaseReference progressRef = database.getReference("user_progress")
                .child(userId).child(courseId);

        Map<String, Object> progressData = new HashMap<>();
        progressData.put("percentage", percentage);
        progressData.put("lastUpdated", System.currentTimeMillis());

        progressRef.updateChildren(progressData).addOnSuccessListener(unused -> {
            // Notifier les écouteurs
            if (progressUpdateListener != null) {
                progressUpdateListener.onProgressUpdated(courseId, percentage);
            }

            // Vérifier si le cours est complété
            if (percentage >= 100) {
                checkCourseCompletion(courseId);
            }
        });
    }

    /**
     * Marque une section d'un cours comme complétée
     * @param courseId ID du cours
     * @param sectionId ID de la section
     * @param totalSections nombre total de sections dans le cours
     */
    public void markSectionCompleted(String courseId, String sectionId, int totalSections) {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "Utilisateur non connecté");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Ajouter des logs pour déboguer
        Log.d(TAG, "Marking section completed: userId=" + userId + ", courseId=" + courseId + ", sectionId=" + sectionId);

        DatabaseReference sectionRef = database.getReference("user_progress")
                .child(userId).child(courseId).child("sections").child(sectionId);

        Map<String, Object> sectionData = new HashMap<>();
        sectionData.put("completed", true);
        sectionData.put("completedAt", System.currentTimeMillis());

        // Assurez-vous que la référence est correcte
        sectionRef.setValue(sectionData)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Section marquée comme terminée avec succès");
                    // Mettre à jour la progression globale
                    updateSectionProgress(courseId, totalSections);
                    
                    // Check achievements for section completions
                    checkSectionAchievements(courseId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erreur lors du marquage de la section comme terminée", e);
                });
    }

    /**
     * Récupère le progrès d'un utilisateur pour un cours spécifique
     * @param courseId ID du cours
     * @param callback callback pour le résultat
     */
    public void getCourseProgress(String courseId, CourseProgressCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("Utilisateur non connecté");
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DatabaseReference progressRef = database.getReference("user_progress")
                .child(userId).child(courseId);

        progressRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("percentage")) {
                    Integer percentage = snapshot.child("percentage").getValue(Integer.class);
                    if (percentage != null) {
                        callback.onProgressLoaded(percentage);
                    } else {
                        callback.onProgressLoaded(0);
                    }
                } else {
                    callback.onProgressLoaded(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Récupère la liste des IDs de cours auxquels l'utilisateur est inscrit
     * @param callback callback pour le résultat
     */
    public void getUserCourses(UserCoursesCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("Utilisateur non connecté");
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DatabaseReference progressRef = database.getReference("user_progress").child(userId);

        progressRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> courseIds = new ArrayList<>();
                for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                    courseIds.add(courseSnapshot.getKey());
                }
                callback.onCoursesLoaded(courseIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Récupère les succès de l'utilisateur
     * @param callback callback pour le résultat
     */
    public void getUserAchievements(AchievementsCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("Utilisateur non connecté");
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DatabaseReference achievementsRef = database.getReference("user_achievements").child(userId);

        achievementsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Achievement> achievements = new ArrayList<>();
                for (DataSnapshot achievementSnapshot : snapshot.getChildren()) {
                    Achievement achievement = achievementSnapshot.getValue(Achievement.class);
                    if (achievement != null) {
                        achievement.setId(achievementSnapshot.getKey());
                        achievements.add(achievement);
                    }
                }
                callback.onAchievementsLoaded(achievements);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Inscrit un utilisateur à un cours
     * @param courseId ID du cours
     */
    public Task<Void> enrollInCourse(String courseId) {
        if (auth.getCurrentUser() == null) {
            throw new IllegalStateException("Utilisateur non connecté");
        }

        String userId = auth.getCurrentUser().getUid();

        // Mettre à jour le progrès utilisateur
        DatabaseReference userProgressRef = database.getReference("user_progress")
                .child(userId).child(courseId);

        Map<String, Object> initialData = new HashMap<>();
        initialData.put("percentage", 0);
        initialData.put("enrolledAt", System.currentTimeMillis());

        // Mettre à jour le nombre d'inscrits au cours
        DatabaseReference courseRef = database.getReference("courses")
                .child(courseId).child("enrolledCount");

        courseRef.get().addOnSuccessListener(dataSnapshot -> {
            Long currentCount = dataSnapshot.getValue(Long.class);
            if (currentCount == null) {
                currentCount = 0L;
            }
            courseRef.setValue(currentCount + 1);
        });

        // Vérifier si c'est le premier cours
        checkFirstCourseAchievement();

        return userProgressRef.setValue(initialData);
    }

    /**
     * Met à jour la progression après avoir complété une section
     * @param courseId ID du cours
     * @param totalSections nombre total de sections
     */
    private void updateSectionProgress(String courseId, int totalSections) {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        Log.d(TAG, "Updating section progress: userId=" + userId + ", courseId=" + courseId);

        DatabaseReference sectionsRef = database.getReference("user_progress")
                .child(userId).child(courseId).child("sections");

        sectionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int completedSections = 0;
                int totalChildren = (int) snapshot.getChildrenCount();

                Log.d(TAG, "Found " + totalChildren + " sections in progress data");

                for (DataSnapshot sectionSnapshot : snapshot.getChildren()) {
                    Boolean completed = sectionSnapshot.child("completed").getValue(Boolean.class);
                    if (completed != null && completed) {
                        completedSections++;
                    }
                }

                Log.d(TAG, "Completed sections: " + completedSections + "/" + totalSections);

                // Calculer le pourcentage de progression
                int percentage = (totalSections > 0) ?
                        (int) ((float) completedSections / totalSections * 100) : 0;

                // Mettre à jour la progression globale du cours
                updateCourseProgress(courseId, percentage);

                // Vérifier pour les accomplissements basés sur la progression
                checkProgressAchievements(courseId, percentage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Erreur lors de la récupération des sections:" +error.getMessage());
            }
        });
    }

    /**
     * Vérifie si un cours est complété et décerne un badge si nécessaire
     * @param courseId ID du cours
     */
    private void checkCourseCompletion(String courseId) {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DatabaseReference courseRef = database.getReference("courses").child(courseId);

        courseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String courseTitle = snapshot.child("title").getValue(String.class);
                String category = snapshot.child("category").getValue(String.class);
                
                if (courseTitle != null) {
                    // Créer un succès pour le cours complété
                    Achievement achievement = new Achievement(
                            "course_completed_" + courseId,
                            "Cours terminé : " + courseTitle,
                            "Vous avez terminé le cours avec succès",
                            ACHIEVEMENT_TYPE_COURSE_COMPLETION,
                            System.currentTimeMillis()
                    );
                    
                    // Set an icon based on the course category
                    achievement.setIconUrl(getIconUrlForCategory(category));

                    // Ajouter le succès
                    addAchievement(achievement);
                    
                    // Check for category mastery
                    if (category != null && !category.isEmpty()) {
                        checkCategoryMastery(category);
                    }
                    
                    // Check for multiple course completions
                    checkMultipleCourseAchievements();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Gérer l'erreur
                Log.e(TAG, "Error checking course completion", error.toException());
            }
        });
    }

    /**
     * Vérifie si c'est le premier cours de l'utilisateur et décerne un badge
     */
    private void checkFirstCourseAchievement() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DatabaseReference progressRef = database.getReference("user_progress").child(userId);

        progressRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() == 1) {
                    // C'est le premier cours
                    Achievement achievement = new Achievement(
                            "first_course",
                            "Premier pas",
                            "Vous avez commencé votre premier cours",
                            ACHIEVEMENT_TYPE_MILESTONE,
                            System.currentTimeMillis()
                    );

                    // Ajouter le succès
                    addAchievement(achievement);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Gérer l'erreur
            }
        });
    }

    /**
     * Vérifie les réalisations liées au nombre de sections complétées
     */
    private void checkSectionAchievements(String courseId) {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DatabaseReference progressRef = database.getReference("user_progress").child(userId);
        
        progressRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalCompletedSections = 0;
                
                // Count all completed sections across all courses
                for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                    if (courseSnapshot.hasChild("sections")) {
                        for (DataSnapshot sectionSnapshot : courseSnapshot.child("sections").getChildren()) {
                            Boolean completed = sectionSnapshot.child("completed").getValue(Boolean.class);
                            if (completed != null && completed) {
                                totalCompletedSections++;
                            }
                        }
                    }
                }
                
                // Check for section milestone achievements
                checkSectionMilestones(totalCompletedSections);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking section achievements", error.toException());
            }
        });
    }
    
    /**
     * Vérifie si l'utilisateur a atteint des jalons de sections complétées
     */
    private void checkSectionMilestones(int totalCompletedSections) {
        // First section completed
        if (totalCompletedSections == 1) {
            Achievement achievement = new Achievement(
                    "first_section",
                    "Premier pas",
                    "Vous avez terminé votre première section de cours",
                    ACHIEVEMENT_TYPE_MILESTONE,
                    System.currentTimeMillis()
            );
            achievement.setIconUrl("https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Ffirst_section.png");
            addAchievement(achievement);
        }
        
        // 5 sections completed
        if (totalCompletedSections == 5) {
            Achievement achievement = new Achievement(
                    "five_sections",
                    "Apprenti débutant",
                    "Vous avez terminé 5 sections de cours",
                    ACHIEVEMENT_TYPE_MILESTONE,
                    System.currentTimeMillis()
            );
            achievement.setIconUrl("https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Ffive_sections.png");
            addAchievement(achievement);
        }
        
        // 25 sections completed
        if (totalCompletedSections == 25) {
            Achievement achievement = new Achievement(
                    "twenty_five_sections",
                    "Apprenant confirmé",
                    "Vous avez terminé 25 sections de cours",
                    ACHIEVEMENT_TYPE_MILESTONE,
                    System.currentTimeMillis()
            );
            achievement.setIconUrl("https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Ftwenty_five_sections.png");
            addAchievement(achievement);
        }
        
        // 50 sections completed
        if (totalCompletedSections == 50) {
            Achievement achievement = new Achievement(
                    "fifty_sections",
                    "Expert en apprentissage",
                    "Vous avez terminé 50 sections de cours",
                    ACHIEVEMENT_TYPE_MILESTONE,
                    System.currentTimeMillis()
            );
            achievement.setIconUrl("https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Ffifty_sections.png");
            addAchievement(achievement);
        }
        
        // 100 sections completed
        if (totalCompletedSections == 100) {
            Achievement achievement = new Achievement(
                    "hundred_sections",
                    "Maître étudiant",
                    "Vous avez terminé 100 sections de cours",
                    ACHIEVEMENT_TYPE_MILESTONE,
                    System.currentTimeMillis()
            );
            achievement.setIconUrl("https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fhundred_sections.png");
            addAchievement(achievement);
        }
    }
    
    /**
     * Vérifie si l'utilisateur a terminé plusieurs cours dans une catégorie
     */
    private void checkCategoryMastery(String category) {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DatabaseReference progressRef = database.getReference("user_progress").child(userId);
        DatabaseReference coursesRef = database.getReference("courses");
        
        progressRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot progressSnapshot) {
                // Get all completed courses
                List<String> completedCourseIds = new ArrayList<>();
                for (DataSnapshot courseSnapshot : progressSnapshot.getChildren()) {
                    Integer percentage = courseSnapshot.child("percentage").getValue(Integer.class);
                    if (percentage != null && percentage >= 100) {
                        completedCourseIds.add(courseSnapshot.getKey());
                    }
                }
                
                if (completedCourseIds.isEmpty()) {
                    return;
                }
                
                // Now check which of these courses belong to the specified category
                coursesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot coursesSnapshot) {
                        int coursesInCategory = 0;
                        
                        for (String courseId : completedCourseIds) {
                            DataSnapshot courseSnapshot = coursesSnapshot.child(courseId);
                            String courseCategory = courseSnapshot.child("category").getValue(String.class);
                            
                            if (category.equals(courseCategory)) {
                                coursesInCategory++;
                            }
                        }
                        
                        // Award badges based on number of courses completed in the category
                        if (coursesInCategory == 3) {
                            Achievement achievement = new Achievement(
                                    "category_master_" + category.toLowerCase().replace(" ", "_"),
                                    "Expert en " + category,
                                    "Vous avez terminé 3 cours dans la catégorie " + category,
                                    ACHIEVEMENT_TYPE_CATEGORY_MASTER,
                                    System.currentTimeMillis()
                            );
                            achievement.setIconUrl(getCategoryMasteryIconUrl(category));
                            addAchievement(achievement);
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking category mastery", error.toException());
                    }
                });
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking category mastery", error.toException());
            }
        });
    }
    
    /**
     * Vérifie les réalisations liées au nombre de cours complétés
     */
    private void checkMultipleCourseAchievements() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DatabaseReference progressRef = database.getReference("user_progress").child(userId);
        
        progressRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int completedCourses = 0;
                
                // Count all completed courses
                for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                    Integer percentage = courseSnapshot.child("percentage").getValue(Integer.class);
                    if (percentage != null && percentage >= 100) {
                        completedCourses++;
                    }
                }
                
                // Award badges based on number of completed courses
                if (completedCourses == 1) {
                    Achievement achievement = new Achievement(
                            "first_course_complete",
                            "Premier cours terminé",
                            "Vous avez terminé votre premier cours avec succès",
                            ACHIEVEMENT_TYPE_MILESTONE,
                            System.currentTimeMillis()
                    );
                    achievement.setIconUrl("https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Ffirst_course_complete.png");
                    addAchievement(achievement);
                }
                
                if (completedCourses == 5) {
                    Achievement achievement = new Achievement(
                            "five_courses_complete",
                            "Apprenant assidu",
                            "Vous avez terminé 5 cours avec succès",
                            ACHIEVEMENT_TYPE_MILESTONE,
                            System.currentTimeMillis()
                    );
                    achievement.setIconUrl("https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Ffive_courses_complete.png");
                    addAchievement(achievement);
                }
                
                if (completedCourses == 10) {
                    Achievement achievement = new Achievement(
                            "ten_courses_complete",
                            "Chercheur de savoir",
                            "Vous avez terminé 10 cours avec succès",
                            ACHIEVEMENT_TYPE_MILESTONE,
                            System.currentTimeMillis()
                    );
                    achievement.setIconUrl("https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Ften_courses_complete.png");
                    addAchievement(achievement);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking multiple course achievements", error.toException());
            }
        });
    }

    /**
     * Ajoute un badge de réussite
     * @param achievement le succès à ajouter
     */
    public void addAchievement(Achievement achievement) {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DatabaseReference achievementRef = database.getReference("user_achievements")
                .child(userId).child(achievement.getId());

        // Vérifier si le succès existe déjà
        achievementRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Ajouter le succès s'il n'existe pas
                    achievementRef.setValue(achievement)
                        .addOnSuccessListener(aVoid -> {
                            // Notify listeners about the new badge
                            if (badgeAwardedListener != null) {
                                badgeAwardedListener.onBadgeAwarded(achievement);
                            }
                            // Log the achievement
                            Log.d(TAG, "New achievement earned: " + achievement.getTitle());
                        });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Gérer l'erreur
                Log.e(TAG, "Error adding achievement", error.toException());
            }
        });
    }

    /**
     * Récupère l'URL d'icône pour une catégorie
     * @param category la catégorie
     * @return l'URL de l'icône
     */
    private String getIconUrlForCategory(String category) {
        if (category == null) {
            return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fdefault_badge.png";
        }
        
        switch (category.toLowerCase()) {
            case "programmation":
                return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fprogrammation.png";
            case "design":
                return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fdesign.png";
            case "marketing":
                return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fmarketing.png";
            case "langue":
                return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Flangue.png";
            case "business":
                return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fbusiness.png";
            case "développement personnel":
                return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fdeveloppement_personnel.png";
            default:
                return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fdefault_badge.png";
        }
    }
    
    /**
     * Récupère l'URL d'icône pour une maîtrise de catégorie
     * @param category la catégorie
     * @return l'URL de l'icône
     */
    private String getCategoryMasteryIconUrl(String category) {
        if (category == null) {
            return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fmastery_default.png";
        }
        
        switch (category.toLowerCase()) {
            case "programmation":
                return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fmastery_programmation.png";
            case "design":
                return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fmastery_design.png";
            case "marketing":
                return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fmastery_marketing.png";
            case "langue":
                return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fmastery_langue.png";
            case "business":
                return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fmastery_business.png";
            case "développement personnel":
                return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fmastery_developpement_personnel.png";
            default:
                return "https://firebasestorage.googleapis.com/v0/b/skilllearn-app.appspot.com/o/badges%2Fmastery_default.png";
        }
    }

    private void checkProgressAchievements(String courseId, int percentage) {
        if (percentage >= 100) {
            // Cours terminé - ajouter un badge d'accomplissement
            DatabaseReference courseRef = database.getReference("courses").child(courseId);
            courseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String courseTitle = snapshot.child("title").getValue(String.class);
                    if (courseTitle != null) {
                        // Créer un succès pour le cours terminé
                        Achievement achievement = new Achievement(
                                "course_completed_" + courseId,
                                "Cours terminé : " + courseTitle,
                                "Vous avez terminé le cours avec succès",
                                ACHIEVEMENT_TYPE_COURSE_COMPLETION,
                                System.currentTimeMillis()
                        );

                        // Ajouter le succès
                        addAchievement(achievement);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Erreur lors de la récupération du titre du cours:" +error.getMessage());
                }
            });
        }
    }
}