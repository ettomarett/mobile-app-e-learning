package com.projet.skilllearn.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.projet.skilllearn.R;
import com.projet.skilllearn.view.adapters.AvatarAdapter;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    public static final String EXTRA_AVATAR_UPDATED = "avatar_updated";
    public static final String EXTRA_SELECTED_AVATAR_INDEX = "selected_avatar_index";

    private Toolbar toolbar;
    private CircleImageView ivProfilePic;
    private EditText etName, etEmail;
    private TextView tvChangeAvatar;
    private CardView cardProfilePic;
    private MaterialButton btnSaveProfile;
    private ProgressBar progressBar;

    private FirebaseUser currentUser;
    private DatabaseReference userRef;

    // Avatars disponibles (ajoutez vos propres ressources d'avatars)
    private final int[] avatarResources = {
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5
            // Ajoutez vos avatars supplémentaires ici
    };

    private int selectedAvatarIndex = 0;
    private boolean avatarUpdated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialiser les vues
        initializeViews();

        // Configurer la toolbar
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Obtenir l'utilisateur actuel
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Référence à l'utilisateur dans la base de données
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        // Récupérer l'avatar actuel de l'utilisateur
        loadUserAvatar();

        // Charger les informations du profil
        loadUserProfile();

        // Configurer les écouteurs de clic
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        ivProfilePic = findViewById(R.id.iv_profile_pic);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        tvChangeAvatar = findViewById(R.id.tv_change_avatar);
        cardProfilePic = findViewById(R.id.card_profile_pic);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void loadUserAvatar() {
        userRef.child("avatarIndex").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Integer avatarIndex = dataSnapshot.getValue(Integer.class);
                    if (avatarIndex != null && avatarIndex >= 0 && avatarIndex < avatarResources.length) {
                        selectedAvatarIndex = avatarIndex;
                        ivProfilePic.setImageResource(avatarResources[selectedAvatarIndex]);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(EditProfileActivity.this,
                        "Erreur: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserProfile() {
        // Afficher le nom de l'utilisateur
        etName.setText(currentUser.getDisplayName());

        // Afficher l'email (non modifiable)
        etEmail.setText(currentUser.getEmail());
        etEmail.setEnabled(false);
    }

    private void setupClickListeners() {
        // Clic sur l'avatar ou le texte "Changer d'avatar"
        View.OnClickListener avatarClickListener = v -> showAvatarSelectionDialog();
        cardProfilePic.setOnClickListener(avatarClickListener);
        tvChangeAvatar.setOnClickListener(avatarClickListener);

        // Clic sur le bouton Enregistrer
        btnSaveProfile.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Le nom ne peut pas être vide");
                return;
            }

            updateProfile(name);
        });
    }

    private void showAvatarSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_avatar_selection, null);
        builder.setView(dialogView);

        RecyclerView rvAvatars = dialogView.findViewById(R.id.rv_avatars);
        rvAvatars.setLayoutManager(new GridLayoutManager(this, 3));

        AvatarAdapter adapter = new AvatarAdapter(this, avatarResources, selectedAvatarIndex,
                position -> {
                    selectedAvatarIndex = position;
                    ivProfilePic.setImageResource(avatarResources[selectedAvatarIndex]);
                    avatarUpdated = true;
                });

        rvAvatars.setAdapter(adapter);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateProfile(String name) {
        showProgress(true);

        // Mettre à jour le profil dans Firebase Authentication
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        currentUser.updateProfile(profileUpdates)
                .addOnSuccessListener(aVoid -> {
                    // Mettre à jour la base de données
                    updateDatabaseProfile(name);
                })
                .addOnFailureListener(e -> {
                    showProgress(false);
                    Snackbar.make(btnSaveProfile, "Erreur: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private void updateDatabaseProfile(String name) {
        userRef.child("name").setValue(name);

        if (avatarUpdated) {
            userRef.child("avatarIndex").setValue(selectedAvatarIndex)
                    .addOnSuccessListener(aVoid -> {
                        showProgress(false);

                        // Préparer les données à renvoyer
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(EXTRA_AVATAR_UPDATED, true);
                        resultIntent.putExtra(EXTRA_SELECTED_AVATAR_INDEX, selectedAvatarIndex);
                        setResult(Activity.RESULT_OK, resultIntent);

                        // Afficher un message et terminer l'activité
                        Snackbar.make(btnSaveProfile, "Profil mis à jour avec succès", Snackbar.LENGTH_SHORT)
                                .addCallback(new Snackbar.Callback() {
                                    @Override
                                    public void onDismissed(Snackbar snackbar, int event) {
                                        finish();
                                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                                    }
                                }).show();
                    })
                    .addOnFailureListener(e -> {
                        showProgress(false);
                        Snackbar.make(btnSaveProfile, "Erreur: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    });
        } else {
            showProgress(false);

            // Préparer les données à renvoyer
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_AVATAR_UPDATED, false);
            setResult(Activity.RESULT_OK, resultIntent);

            // Afficher un message et terminer l'activité
            Snackbar.make(btnSaveProfile, "Profil mis à jour avec succès", Snackbar.LENGTH_SHORT)
                    .addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            finish();
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                        }
                    }).show();
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!show);
    }

    @Override
    public void onBackPressed() {
        if (avatarUpdated) {
            new AlertDialog.Builder(this)
                    .setTitle("Modifications non enregistrées")
                    .setMessage("Vous avez modifié votre avatar. Voulez-vous enregistrer vos modifications?")
                    .setPositiveButton("Enregistrer", (dialog, which) -> {
                        String name = etName.getText().toString().trim();
                        if (!name.isEmpty()) {
                            updateProfile(name);
                        } else {
                            etName.setError("Le nom ne peut pas être vide");
                        }
                    })
                    .setNegativeButton("Annuler", (dialog, which) -> {
                        setResult(Activity.RESULT_CANCELED);
                        finish();
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    })
                    .show();
        } else {
            super.onBackPressed();
            setResult(Activity.RESULT_CANCELED);
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }
}