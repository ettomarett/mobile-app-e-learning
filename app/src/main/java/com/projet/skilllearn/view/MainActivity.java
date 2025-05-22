package com.projet.skilllearn.view;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentContainerView;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.projet.skilllearn.R;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private NavController navController;
    private BottomNavigationView bottomNavigationView;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Set content view first
            setContentView(R.layout.activity_main);
            Log.d(TAG, "Content view set");

            // Find views
            bottomNavigationView = findViewById(R.id.bottom_navigation);
            if (bottomNavigationView == null) {
                Log.e(TAG, "BottomNavigationView not found in layout");
                Toast.makeText(this, "Error: Navigation view not found", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "BottomNavigationView found");

            // Setup navigation
            try {
                // Use NavHostFragment approach for more reliable fragment management
                FragmentContainerView navHostFragment = findViewById(R.id.nav_host_fragment);
                if (navHostFragment == null) {
                    Log.e(TAG, "NavHostFragment not found in layout");
                    Toast.makeText(this, "Error: Navigation host not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Get NavController from the NavHostFragment
                navController = ((NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment))
                        .getNavController();
                
                Log.d(TAG, "NavController initialized");
                
                // Define top level destinations
                appBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.nav_home,
                        R.id.nav_catalog,
                        R.id.nav_assistant,
                        R.id.nav_profile
                ).build();
                Log.d(TAG, "AppBarConfiguration created");
                
                // Connect bottom navigation with nav controller
                NavigationUI.setupWithNavController(bottomNavigationView, navController);
                Log.d(TAG, "BottomNavigationView setup completed");
                
                // Only setup ActionBar if it exists
                if (getSupportActionBar() != null) {
                    NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
                    Log.d(TAG, "ActionBar setup completed");
                } else {
                    Log.d(TAG, "No ActionBar present, skipping setup");
                }
                
                // Log navigation changes
                navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    try {
                        String label = destination.getLabel() != null ? destination.getLabel().toString() : "Unknown";
                        Log.d(TAG, "Navigation to: " + label + " (ID: " + destination.getId() + ")");
                    } catch (Exception e) {
                        Log.e(TAG, "Error logging navigation change", e);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error setting up navigation", e);
                Toast.makeText(this, "Error setting up navigation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        try {
            return NavigationUI.navigateUp(navController, appBarConfiguration) 
                || super.onSupportNavigateUp();
        } catch (Exception e) {
            Log.e(TAG, "Error in onSupportNavigateUp", e);
            return super.onSupportNavigateUp();
        }
    }
}