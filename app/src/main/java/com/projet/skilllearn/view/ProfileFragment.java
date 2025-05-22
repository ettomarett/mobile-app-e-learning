package com.projet.skilllearn.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.os.Handler;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projet.skilllearn.R;
import com.projet.skilllearn.model.Achievement;
import com.projet.skilllearn.view.adapters.AchievementAdapter;
import com.projet.skilllearn.viewmodel.ProfileViewModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private final int[] avatarResources = {
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5
            // Assurez-vous que ce sont les mêmes avatars que dans EditProfileActivity
    };
    private ImageView ivProfile;
    private TextView tvName, tvEmail;
    private ProgressBar progressLearning;
    private RecyclerView rvAchievements;
    private LinearLayout btnEditProfile, btnLogout;

    private ProfileViewModel viewModel;
    private AchievementAdapter adapter;
    private static final int EDIT_PROFILE_REQUEST_CODE = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        try {
            return inflater.inflate(R.layout.fragment_profile, container, false);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'inflation du layout", e);
            // Fallback vers un layout simple en cas d'erreur
            return new View(requireContext());
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            // Initialiser les vues
            initializeViews(view);

            // Configurer les clics - AJOUTEZ CETTE LIGNE
            setupClickListeners();

            // Initialiser le ViewModel
            viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

            // Configurer le RecyclerView
            setupRecyclerView();

            // Observer les données du ViewModel
            setupObservers();

            // Charger les données du profil
            loadProfileData();
        } catch (Exception e) {
            Log.e(TAG, "Erreur dans onViewCreated", e);
            Toast.makeText(requireContext(), "Erreur lors du chargement du profil", Toast.LENGTH_SHORT).show();
        }
    }
    private void initializeViews(View view) {
        try {
            // Initialiser les vues
            ivProfile = view.findViewById(R.id.iv_profile);
            tvName = view.findViewById(R.id.tv_name);
            tvEmail = view.findViewById(R.id.tv_email);
            progressLearning = view.findViewById(R.id.progress_learning);
            rvAchievements = view.findViewById(R.id.rv_achievements);

            // Initialiser les "boutons" qui sont en fait des LinearLayout
            btnEditProfile = view.findViewById(R.id.btn_edit_profile);
            btnLogout = view.findViewById(R.id.btn_logout);

            // Valeurs par défaut
            tvName.setText("Nom d'utilisateur");
            tvEmail.setText("email@exemple.com");
            progressLearning.setProgress(0);

            // Debug
            Log.d(TAG, "Vues initialisées : btnEditProfile=" + (btnEditProfile != null) + ", btnLogout=" + (btnLogout != null));
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation des vues", e);
        }
    }


    private void setupRecyclerView() {
        try {
            adapter = new AchievementAdapter(requireContext(), new ArrayList<>());
            if (rvAchievements != null) {
                rvAchievements.setAdapter(adapter);
                rvAchievements.setLayoutManager(new GridLayoutManager(requireContext(), 3));
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la configuration du RecyclerView", e);
        }
    }

    private void setupObservers() {
        try {
            // Observer les changements dans les données de progression
            viewModel.getLearningProgress().observe(getViewLifecycleOwner(), progress -> {
                if (progress != null && progressLearning != null) {
                    progressLearning.setProgress(progress);
                }
            });

            // Observer les réalisations
            viewModel.getAchievements().observe(getViewLifecycleOwner(), achievements -> {
                if (achievements != null && adapter != null) {
                    adapter.updateAchievements(achievements);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la configuration des observateurs", e);
        }
    }

    private void setupClickListeners() {
        try {
            // Configurer le "bouton" Modifier le profil
            if (btnEditProfile != null) {
                btnEditProfile.setOnClickListener(v -> {
                    try {
                        Log.d(TAG, "Clic sur Modifier le profil");
                        Toast.makeText(requireContext(), "Ouverture de l'éditeur de profil", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(requireActivity(), EditProfileActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors du lancement de l'activité d'édition de profil", e);
                        Toast.makeText(requireContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "btnEditProfile est null");
            }

            // Configurer le "bouton" Déconnexion
            if (btnLogout != null) {
                btnLogout.setOnClickListener(v -> {
                    Log.d(TAG, "Clic sur Déconnexion");
                    // Afficher une confirmation avant de déconnecter l'utilisateur
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Déconnexion")
                            .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
                            .setPositiveButton("Oui", (dialog, which) -> {
                                // Déconnecter l'utilisateur de Firebase
                                FirebaseAuth.getInstance().signOut();
                                
                                // Rediriger vers la page de connexion
                                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            })
                            .setNegativeButton("Non", null)
                            .show();
                });
            } else {
                Log.e(TAG, "btnLogout est null");
            }
            
            // Configurer le bouton Téléchargements
            LinearLayout btnDownload = getView().findViewById(R.id.btn_download);
            if (btnDownload != null) {
                btnDownload.setOnClickListener(v -> {
                    Log.d(TAG, "Clic sur Téléchargements");
                    try {
                        Intent intent = new Intent(requireActivity(), DownloadsActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors du lancement de l'activité de téléchargements", e);
                        Toast.makeText(requireContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "btnDownload est null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur dans setupClickListeners", e);
        }
    }

    private void loadProfileData() {
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                // Définir le nom et l'email
                if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                    tvName.setText(currentUser.getDisplayName());
                }

                tvEmail.setText(currentUser.getEmail());

                // Charger la photo de profil
                if (currentUser.getPhotoUrl() != null) {
                    try {
                        Picasso.get()
                                .load(currentUser.getPhotoUrl())
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .error(R.drawable.ic_launcher_foreground)
                                .into(ivProfile);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors du chargement de la photo de profil", e);
                        ivProfile.setImageResource(R.drawable.ic_launcher_foreground);
                    }
                } else {
                    ivProfile.setImageResource(R.drawable.ic_launcher_foreground);
                }

                // Charger la progression et les réalisations
                viewModel.loadUserData(currentUser.getUid());
            } else {
                // L'utilisateur n'est pas connecté, rediriger vers la page de connexion
                try {
                    Intent intent = new Intent(requireActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors de la redirection vers LoginActivity", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du chargement des données du profil", e);
            Toast.makeText(requireContext(), "Erreur lors du chargement du profil", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        try {
            // Recharger les données à chaque retour sur le fragment
            loadProfileData();
        } catch (Exception e) {
            Log.e(TAG, "Erreur dans onResume", e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_PROFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Vérifier si l'avatar a été mis à jour
            if (data != null && data.getBooleanExtra(EditProfileActivity.EXTRA_AVATAR_UPDATED, false)) {
                int selectedAvatarIndex = data.getIntExtra(EditProfileActivity.EXTRA_SELECTED_AVATAR_INDEX, 0);

                // Mettre à jour l'avatar dans le profil
                if (ivProfile != null && selectedAvatarIndex >= 0 && selectedAvatarIndex < avatarResources.length) {
                    ivProfile.setImageResource(avatarResources[selectedAvatarIndex]);
                }
            }

            // Recharger le reste des données du profil
            loadProfileData();

            // Afficher un message de succès
            if (getView() != null) {
                Snackbar.make(getView(), "Profil mis à jour avec succès", Snackbar.LENGTH_SHORT).show();
            }
        }
    }    private void performLogout() {
        // Afficher un indicateur de progression
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Déconnexion en cours...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            // Effacer les données en cache si nécessaire
            // Exemple : clearLocalUserData();

            // Déconnexion de Firebase
            FirebaseAuth.getInstance().signOut();

            // Rediriger vers l'écran de connexion
            new Handler().postDelayed(() -> {
                progressDialog.dismiss();

                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();

                // Animation de transition
                requireActivity().overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }, 800); // Délai pour montrer le dialogue et donner une impression de travail
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Erreur lors de la déconnexion", e);
            Toast.makeText(requireContext(), "Erreur lors de la déconnexion: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}