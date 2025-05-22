package com.projet.skilllearn.view.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.projet.skilllearn.R;
import com.projet.skilllearn.model.Achievement;
import com.projet.skilllearn.model.Question;
import com.projet.skilllearn.model.Quiz;
import com.projet.skilllearn.utils.UserProgressManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizFragment extends Fragment {
    private static final String TAG = "QuizFragment";
    private static QuizFragment instance;
    private TextView tvQuestion;
    private RadioGroup rgOptions;
    private Button btnSubmit;
    private Button btnNext;
    private TextView tvFeedback;
    private TextView tvScore;

    private Quiz quiz;
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private boolean answered = false;

    public static QuizFragment getInstance() {
        if (instance == null) {
            instance = new QuizFragment();
        }
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView appelé");
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated appelé");

        // Initialiser les vues
        tvQuestion = view.findViewById(R.id.tv_question);
        rgOptions = view.findViewById(R.id.rg_options);
        btnSubmit = view.findViewById(R.id.btn_submit);
        btnNext = view.findViewById(R.id.btn_next);
        tvFeedback = view.findViewById(R.id.tv_feedback);
        tvScore = view.findViewById(R.id.tv_score);

        // Afficher un message par défaut si aucun quiz n'est disponible
        if (tvQuestion.getText().toString().isEmpty()) {
            tvQuestion.setText("Chargement des questions...");
        }

        // Configurer les listeners
        btnSubmit.setOnClickListener(v -> checkAnswer());
        btnNext.setOnClickListener(v -> showNextQuestion());

        // Afficher le quiz s'il est disponible
        if (quiz != null) {
            updateQuiz(quiz);
        }
    }

    public void updateQuiz(Quiz quiz) {
        Log.d(TAG, "updateQuiz appelé avec quiz: " + (quiz != null ? quiz.getTitle() : "null"));

        if (quiz == null) {
            tvQuestion.setText("Aucun quiz disponible pour cette section");
            rgOptions.setVisibility(View.GONE);
            btnSubmit.setVisibility(View.GONE);
            btnNext.setVisibility(View.GONE);
            return;
        }

        this.quiz = quiz;
        this.questions = quiz.getQuestions();

        if (questions == null || questions.isEmpty()) {
            Log.e(TAG, "Questions nulles ou vides");
            tvQuestion.setText("Aucune question disponible pour ce quiz");
            rgOptions.setVisibility(View.GONE);
            btnSubmit.setVisibility(View.GONE);
            btnNext.setVisibility(View.GONE);
            return;
        }

        // Réinitialiser l'état
        rgOptions.setVisibility(View.VISIBLE);
        btnSubmit.setVisibility(View.VISIBLE);
        btnNext.setVisibility(View.VISIBLE);

        this.currentQuestionIndex = 0;
        this.score = 0;
        this.answered = false;

        updateScoreDisplay();
        showQuestion(currentQuestionIndex);
    }

    private void showQuestion(int index) {
        Log.d(TAG, "showQuestion appelé avec index: " + index);

        if (questions == null) {
            Log.e(TAG, "questions est null");
            return;
        }

        if (index >= questions.size()) {
            Log.e(TAG, "Index hors limites: " + index + " (total: " + questions.size() + ")");
            return;
        }

        Question question = questions.get(index);
        Log.d(TAG, "Question: " + question.getQuestion());

        tvQuestion.setText(question.getQuestion());

        // Réinitialiser les options
        rgOptions.removeAllViews();

        // Ajouter les options
        List<String> options = question.getOptions();
        if (options == null) {
            options = new ArrayList<>();
        }
        
        Log.d(TAG, "Nombre d'options: " + options.size());

        for (int i = 0; i < options.size(); i++) {
            RadioButton rb = new RadioButton(requireContext());
            rb.setId(View.generateViewId());
            rb.setText(options.get(i));
            rb.setTextSize(16);
            rb.setPadding(0, 8, 0, 8);
            rgOptions.addView(rb);
            Log.d(TAG, "Option ajoutée: " + options.get(i));
        }

        // Réinitialiser l'état
        rgOptions.clearCheck();
        btnSubmit.setEnabled(true);
        btnNext.setEnabled(false);
        tvFeedback.setText("");
        tvFeedback.setVisibility(View.GONE);

        answered = false;
    }

    private void checkAnswer() {
        Log.d(TAG, "checkAnswer appelé");

        if (answered || questions == null || currentQuestionIndex >= questions.size()) {
            return;
        }

        int selectedId = rgOptions.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(requireContext(), "Veuillez sélectionner une réponse", Toast.LENGTH_SHORT).show();
            return;
        }

        answered = true;

        // Trouver l'index de l'option sélectionnée
        int selectedIndex = -1;
        for (int i = 0; i < rgOptions.getChildCount(); i++) {
            RadioButton rb = (RadioButton) rgOptions.getChildAt(i);
            if (rb.getId() == selectedId) {
                selectedIndex = i;
                break;
            }
        }

        Question question = questions.get(currentQuestionIndex);
        int correctIndex = question.getCorrectOptionIndex();
        Log.d(TAG, "Option sélectionnée: " + selectedIndex + ", correcte: " + correctIndex);

        if (selectedIndex == correctIndex) {
            // Réponse correcte
            score++;
            tvFeedback.setText("Correct ! " + question.getExplanation());
            tvFeedback.setTextColor(getResources().getColor(R.color.colorCorrect, null));
        } else {
            // Réponse incorrecte
            tvFeedback.setText("Incorrect. La bonne réponse est : " +
                    question.getOptions().get(correctIndex) + "\n" + question.getExplanation());
            tvFeedback.setTextColor(getResources().getColor(R.color.colorIncorrect, null));
        }

        tvFeedback.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);
        btnNext.setEnabled(true);

        updateScoreDisplay();
    }

    private void updateScoreDisplay() {
        if (questions != null && !questions.isEmpty()) {
            tvScore.setText(String.format("Score: %d/%d", score, questions.size()));
        } else {
            tvScore.setText("Score: 0/0");
        }
    }

    private void showNextQuestion() {
        Log.d(TAG, "showNextQuestion appelé");

        currentQuestionIndex++;
        updateScoreDisplay();

        if (currentQuestionIndex < questions.size()) {
            showQuestion(currentQuestionIndex);
        } else {
            // Quiz terminé
            showQuizResult();
        }
    }

    private void showQuizResult() {
        Log.d(TAG, "showQuizResult appelé");

        double percentage = (double) score / questions.size() * 100;

        // Cacher les éléments du quiz
        tvQuestion.setVisibility(View.GONE);
        rgOptions.setVisibility(View.GONE);
        btnSubmit.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);

        // Afficher le résultat
        tvFeedback.setText(String.format("Quiz terminé !\nVotre score : %d/%d (%.1f%%)",
                score, questions.size(), percentage));
        tvFeedback.setVisibility(View.VISIBLE);

        // Sauvegarder le résultat si l'utilisateur est connecté
        if (FirebaseAuth.getInstance().getCurrentUser() != null && getActivity() != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String courseId = getActivity().getIntent().getStringExtra("courseId");

            if (courseId != null && quiz != null) {
                // Enregistrer le résultat du quiz
                saveQuizResult(userId, courseId, quiz.getQuizId(), percentage);

                // Vérifier si l'utilisateur mérite un badge
                if (percentage >= 80) {
                    UserProgressManager.getInstance().addAchievement(
                            new Achievement(
                                    "quiz_master_" + quiz.getQuizId(),
                                    "Quiz Master",
                                    "Vous avez obtenu plus de 80% au quiz",
                                    "quiz_master",
                                    System.currentTimeMillis()
                            )
                    );
                }
            }
        }
    }

    private void saveQuizResult(String userId, String courseId, String quizId, double percentage) {
        Log.d(TAG, "saveQuizResult appelé");

        DatabaseReference resultRef = FirebaseDatabase.getInstance().getReference("quiz_results")
                .child(userId).child(courseId).child(quizId);

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("score", score);
        resultData.put("totalQuestions", questions.size());
        resultData.put("percentage", percentage);
        resultData.put("completedAt", System.currentTimeMillis());

        resultRef.setValue(resultData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Résultat du quiz enregistré avec succès"))
                .addOnFailureListener(e -> Log.e(TAG, "Erreur lors de l'enregistrement du résultat", e));
    }
}