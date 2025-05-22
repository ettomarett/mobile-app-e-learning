package com.projet.skilllearn.view;

import android.content.Intent;
import android.os.Bundle;
import android.transition.Fade;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.projet.skilllearn.R;
import com.projet.skilllearn.viewmodel.AuthViewModel;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister, btnOfflineMode;
    private ProgressBar progressBar;
    private com.projet.skilllearn.viewmodel.AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setAllowEnterTransitionOverlap(true);
        getWindow().setEnterTransition(new Fade());
        Window window = getWindow();
        window.setStatusBarColor(getResources().getColor(R.color.colorPrimary, null));
        setContentView(R.layout.activity_login);
        try {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            Log.d("LoginActivity", "Firebase Auth initialisé directement: " + mAuth);
        } catch (Exception e) {
            Log.e("LoginActivity", "Erreur lors de l'initialisation directe de Firebase Auth", e);
        }
        // Initialiser les vues
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        btnOfflineMode = findViewById(R.id.btn_offline_mode);
        progressBar = findViewById(R.id.progress_bar);

        // Initialiser le ViewModel
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupObservers();
        setupClickListeners();
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            // Animation du bouton lorsqu'il est cliqué
            btnLogin.setAlpha(0.7f);
            btnLogin.animate().alpha(1.0f).setDuration(300).start();

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (validateInput(email, password)) {
                try {
                    viewModel.loginUser(email, password);
                } catch (Exception e) {
                    Log.e("LoginActivity", "Erreur lors de la connexion", e);
                    Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        btnRegister.setOnClickListener(v -> {
            Log.d("LoginActivity", "Clic sur le bouton Register");
            showRegisterDialog();
        });

        btnOfflineMode.setOnClickListener(v -> {
            Log.d("LoginActivity", "Clic sur le bouton Mode hors ligne");
            // Start MainActivity directly with offline mode
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("offline_mode", true);
            startActivity(intent);
            finish();
        });
    }

    // Dans setupObservers(), ajoutez des logs
    private void setupObservers() {
        viewModel.getCurrentUser().observe(this, user -> {
            Log.d("LoginActivity", "Observer getCurrentUser déclenché, user: " + (user != null ? "non null" : "null"));
            if (user != null) {
                Log.d("LoginActivity", "Utilisateur connecté, redirection vers MainActivity");
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            Log.d("LoginActivity", "Observer isLoading déclenché: " + isLoading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnLogin.setEnabled(!isLoading);
            btnRegister.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(this, message -> {
            Log.d("LoginActivity", "Observer errorMessage déclenché: " + message);
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateInput(String email, String password) {
        boolean isValid = true;

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            isValid = false;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            isValid = false;
        }

        return isValid;
    }

    private void showRegisterDialog() {
        // Créer une vue pour le dialogue
        View view = getLayoutInflater().inflate(R.layout.dialog_register, null);
        EditText etRegName = view.findViewById(R.id.et_reg_name);
        EditText etRegEmail = view.findViewById(R.id.et_reg_email);
        EditText etRegPassword = view.findViewById(R.id.et_reg_password);

        // Créer le dialogue
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Inscription")
                .setView(view)
                .setPositiveButton("S'inscrire", (dialog, which) -> {
                    String name = etRegName.getText().toString().trim();
                    String email = etRegEmail.getText().toString().trim();
                    String password = etRegPassword.getText().toString().trim();

                    if (validateRegistrationInput(name, email, password)) {
                        viewModel.registerUser(name, email, password);
                    }
                })
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private boolean validateRegistrationInput(String name, String email, String password) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Tous les champs sont obligatoires", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }



        // Vous pouvez implémenter un dialogue d'inscription plus complet ici plus tard

}