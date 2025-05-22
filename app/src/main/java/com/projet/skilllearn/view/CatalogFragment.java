package com.projet.skilllearn.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.projet.skilllearn.R;
import com.projet.skilllearn.model.Course;
import com.projet.skilllearn.view.adapters.CourseAdapter;
import com.projet.skilllearn.viewmodel.CourseViewModel;

import java.util.ArrayList;

public class CatalogFragment extends Fragment implements CourseAdapter.OnCourseClickListener {

    private RecyclerView rvCourses;
    private ProgressBar progressBar;
    private TextView tvEmptyView;
    private CourseViewModel viewModel;
    private CourseAdapter adapter;
    private SearchView searchView;
    private Spinner spinnerCategory;
    private Spinner spinnerLevel;
    private Spinner spinnerSort;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_catalog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialiser les vues
        rvCourses = view.findViewById(R.id.rv_courses);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmptyView = view.findViewById(R.id.tv_empty_view);
        searchView = view.findViewById(R.id.search_view_courses);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        spinnerLevel = view.findViewById(R.id.spinner_level);
        spinnerSort = view.findViewById(R.id.spinner_sort);

        // Initialize generate course view
        TextView tvGenerateCourse = view.findViewById(R.id.tv_generate_course);
        tvGenerateCourse.setOnClickListener(v -> navigateToAIAssistant());

        // Initialiser le ViewModel
        viewModel = new ViewModelProvider(this).get(CourseViewModel.class);

        // Initialiser l'adaptateur
        adapter = new CourseAdapter(requireContext(), new ArrayList<>(), this);
        rvCourses.setAdapter(adapter);
        rvCourses.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Configurer la barre de recherche
        setupSearchView();

        // Configurer les spinners de filtrage et de tri
        setupSpinners();

        // Observer les données du ViewModel
        observeViewModel();

        // Charger les cours
        viewModel.loadAllCourses();
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.searchCourses(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Recherche au fur et à mesure de la saisie, mais seulement si le texte
                // est vide ou assez long pour éviter des recherches inutiles
                if (newText.isEmpty() || newText.length() >= 3) {
                    viewModel.searchCourses(newText);
                }
                return true;
            }
        });
    }

    private void setupSpinners() {
        // Spinner Catégorie
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.course_categories,
                android.R.layout.simple_spinner_item
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String category = parent.getItemAtPosition(position).toString();
                if (position == 0) {
                    // "Toutes les catégories" est sélectionné
                    viewModel.loadAllCourses();
                } else {
                    viewModel.loadCoursesByCategory(category);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ne rien faire
            }
        });

        // Spinner Niveau
        ArrayAdapter<CharSequence> levelAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.course_levels,
                android.R.layout.simple_spinner_item
        );
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(levelAdapter);
        spinnerLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String level = parent.getItemAtPosition(position).toString();
                if (position == 0) {
                    // "Tous les niveaux" est sélectionné
                    viewModel.loadAllCourses();
                } else {
                    viewModel.filterCoursesByLevel(level);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ne rien faire
            }
        });

        // Spinner Tri
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.course_sort_options,
                android.R.layout.simple_spinner_item
        );
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1: // "Durée (croissant)"
                        viewModel.sortCoursesByDuration(true);
                        break;
                    case 2: // "Durée (décroissant)"
                        viewModel.sortCoursesByDuration(false);
                        break;
                    case 3: // "Popularité"
                        viewModel.sortCoursesByPopularity();
                        break;
                    default:
                        // Ne rien faire, garder l'ordre actuel
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ne rien faire
            }
        });
    }

    private void observeViewModel() {
        viewModel.getCourses().observe(getViewLifecycleOwner(), courses -> {
            if (courses != null && !courses.isEmpty()) {
                adapter = new CourseAdapter(requireContext(), courses, this);
                rvCourses.setAdapter(adapter);
                rvCourses.setVisibility(View.VISIBLE);
                tvEmptyView.setVisibility(View.GONE);
                view.findViewById(R.id.tv_generate_course).setVisibility(View.GONE);
            } else {
                rvCourses.setVisibility(View.GONE);
                tvEmptyView.setVisibility(View.VISIBLE);
                view.findViewById(R.id.tv_generate_course).setVisibility(View.VISIBLE);
                tvEmptyView.setText("Aucun cours trouvé pour votre recherche");
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });
    }

    private void navigateToAIAssistant() {
        // Navigate to AI Assistant with pre-filled message
        Bundle args = new Bundle();
        args.putString("initial_message", 
            "Je souhaite que vous génériez un cours sur [votre sujet]. " +
            "Par exemple: \"Je souhaite que vous génériez un cours sur la photographie pour débutants, " +
            "couvrant les bases de la composition, l'exposition et l'utilisation de l'appareil photo.\"");
        
        Navigation.findNavController(requireView())
                .navigate(R.id.nav_assistant, args);
    }

    @Override
    public void onCourseClick(Course course) {
        // Naviguer vers l'écran de détail du cours
        Intent intent = new Intent(requireActivity(), CourseDetailActivity.class);
        intent.putExtra("courseId", course.getCourseId());
        startActivity(intent);
    }

    public static class UserAchievementsFragment {
    }
}