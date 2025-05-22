package com.projet.skilllearn.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projet.skilllearn.R;
import com.projet.skilllearn.model.Course;
import com.projet.skilllearn.repository.CourseRepository;
import com.projet.skilllearn.view.adapters.CourseAdapter;
import com.projet.skilllearn.view.adapters.CourseProgressAdapter;
import com.projet.skilllearn.viewmodel.CourseViewModel;
import com.projet.skilllearn.viewmodel.ProfileViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // UI components
    private TextView tvWelcome;
    private TextView tvWelcomeMessage;
    private TextView tvCoursesCount;
    private TextView tvUserAchievements;
    private RecyclerView rvRecommendedCourses;
    private RecyclerView rvContinueLearning;
    private TextView tvViewAllRecommended;
    private TextView tvViewAllLearning;
    private MaterialButton btnExplore;
    private ProgressBar progressBar;

    // ViewModels
    private CourseViewModel courseViewModel;
    private ProfileViewModel profileViewModel;

    // Adapters
    private CourseAdapter recommendedCoursesAdapter;
    private CourseProgressAdapter continueCoursesAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the ViewModels
        courseViewModel = new ViewModelProvider(this).get(CourseViewModel.class);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // Initialize views
        initViews(view);

        // Setup adapters for RecyclerViews
        setupAdapters();

        // Observe ViewModel data
        observeViewModels();

        // Load data
        loadData();

        // Setup click listeners
        setupClickListeners();
    }

    private void initViews(View view) {
        try {
            tvWelcome = view.findViewById(R.id.tv_welcome);
            tvWelcomeMessage = view.findViewById(R.id.tv_welcome_message);
            tvCoursesCount = view.findViewById(R.id.tv_courses_count);
            tvUserAchievements = view.findViewById(R.id.tv_user_achievements);
            rvRecommendedCourses = view.findViewById(R.id.rv_recommended_courses);
            rvContinueLearning = view.findViewById(R.id.rv_continue_learning);
            tvViewAllRecommended = view.findViewById(R.id.tv_view_all_recommended);
            tvViewAllLearning = view.findViewById(R.id.tv_view_all_learning);
            btnExplore = view.findViewById(R.id.btn_explore);
            // Add ProgressBar if it exists in your layout
            // progressBar = view.findViewById(R.id.progress_bar);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
        }
    }

    private void setupAdapters() {
        try {
            // Adapter for recommended courses (horizontal list)
            recommendedCoursesAdapter = new CourseAdapter(requireContext(), new ArrayList<>(),
                    course -> {
                        // Navigate to course detail
                        Intent intent = new Intent(requireActivity(), CourseDetailActivity.class);
                        intent.putExtra("courseId", course.getCourseId());
                        startActivity(intent);
                    });

            rvRecommendedCourses.setAdapter(recommendedCoursesAdapter);
            rvRecommendedCourses.setLayoutManager(
                    new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

            // Adapter for courses in progress
            continueCoursesAdapter = new CourseProgressAdapter(requireContext(), new ArrayList<>());
            rvContinueLearning.setAdapter(continueCoursesAdapter);
            rvContinueLearning.setLayoutManager(
                    new LinearLayoutManager(requireContext()));
        } catch (Exception e) {
            Log.e(TAG, "Error setting up adapters", e);
        }
    }

    private void observeViewModels() {
        // Observe recommended courses
        courseViewModel.getCourses().observe(getViewLifecycleOwner(), courses -> {
            try {
                if (courses != null) {
                    recommendedCoursesAdapter.updateCourses(courses);
                    tvCoursesCount.setText(String.valueOf(courses.size()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating recommended courses", e);
            }
        });

        // Observer error messages
        courseViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                courseViewModel.clearError();
            }
        });
        courseViewModel.getUserCourses().observe(getViewLifecycleOwner(), courses -> {
            if (courses != null) {
                continueCoursesAdapter.updateCourses(courses);
            }
        });

        // Observer loading state
        courseViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // Observe user achievements
        profileViewModel.getAchievements().observe(getViewLifecycleOwner(), achievements -> {
            try {
                if (achievements != null) {
                    tvUserAchievements.setText(String.valueOf(achievements.size()));
                } else {
                    tvUserAchievements.setText("0");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating achievements count", e);
            }
        });
    }

    private void loadData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Personalize welcome message with user's name
            String userName = currentUser.getDisplayName();
            if (userName != null && !userName.isEmpty()) {
                tvWelcome.setText(getString(R.string.welcome_name, userName));
            }

            // Get user ID
            String userId = currentUser.getUid();

            // Load popular courses as recommendations initially
            courseViewModel.sortCoursesByPopularity();

            // Load user's enrolled courses - this will update the LiveData<List<Course>>
            // that we're observing above
            courseViewModel.getUserCourses(userId);
            // Load user's achievements
            profileViewModel.loadUserData(userId);
        } else {
            // Default message if not logged in
            tvWelcome.setText(R.string.welcome_title);
            tvCoursesCount.setText("0");
            tvUserAchievements.setText("0");

            // Load popular courses for non-logged in users
            courseViewModel.sortCoursesByPopularity();
        }
    }

    private void setupClickListeners() {
        try {
            btnExplore.setOnClickListener(v -> navigateToCatalog());

            MaterialButton btnAiAssistant = requireView().findViewById(R.id.btn_ai_assistant);
            btnAiAssistant.setOnClickListener(v -> {
                // Navigate to LLM Chat Fragment
                Navigation.findNavController(requireView())
                        .navigate(R.id.nav_assistant);
            });

            tvViewAllRecommended.setOnClickListener(v -> navigateToCatalog());
            tvViewAllLearning.setOnClickListener(v -> navigateToUserProgress());
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners", e);
        }
    }

    private void navigateToCatalog() {
        try {
            Navigation.findNavController(requireView()).navigate(R.id.nav_catalog);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to catalog", e);
        }
    }

    private void navigateToUserProgress() {
        try {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                Navigation.findNavController(requireView()).navigate(R.id.nav_profile);
            } else {
                // Show login prompt if not logged in
                Toast.makeText(requireContext(),
                        "Veuillez vous connecter pour accéder à vos cours",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to user progress", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
        loadData();
    }
}