package com.projet.skilllearn.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.projet.skilllearn.R;
import com.projet.skilllearn.model.Course;
import com.projet.skilllearn.utils.VideoDownloadManager;
import com.projet.skilllearn.view.adapters.DownloadedVideoAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadsActivity extends AppCompatActivity implements DownloadedVideoAdapter.OnVideoActionListener {
    private static final String TAG = "DownloadsActivity";
    
    private RecyclerView recyclerView;
    private TextView emptyView;
    private DownloadedVideoAdapter adapter;
    private VideoDownloadManager downloadManager;
    
    // Store course data to display alongside downloaded videos
    private Map<String, Course> courseMap = new HashMap<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.downloads);
        }
        
        // Initialize views
        recyclerView = findViewById(R.id.recycler_downloads);
        emptyView = findViewById(R.id.empty_view);
        
        // Set up recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DownloadedVideoAdapter(this, new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
        
        // Get download manager instance and initialize it
        downloadManager = VideoDownloadManager.getInstance(this);
        
        // Load downloaded videos
        loadDownloadedVideos();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload videos when returning to the activity
        loadDownloadedVideos();
    }
    
    private void loadDownloadedVideos() {
        // In offline mode, don't check for user login
        boolean isOfflineMode = getIntent().getBooleanExtra("offline_mode", false);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        
        if (!isOfflineMode && user == null) {
            Toast.makeText(this, "Vous devez être connecté pour voir vos téléchargements", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Show loading state
        emptyView.setText(R.string.loading_downloads);
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        
        // Verify the movies directory exists
        File moviesDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (moviesDir == null || !moviesDir.exists()) {
            if (moviesDir != null) {
                moviesDir.mkdirs();
            }
            Log.d(TAG, "Created movies directory");
        }
        
        // Observe the download status
        downloadManager.getDownloadStatus().observe(this, downloadInfoMap -> {
            List<VideoDownloadManager.VideoDownloadInfo> downloadedVideos = new ArrayList<>();
            
            // Filter completed downloads
            for (VideoDownloadManager.VideoDownloadInfo info : downloadInfoMap.values()) {
                if (info.getStatus() == VideoDownloadManager.VideoDownloadInfo.STATUS_COMPLETED) {
                    // Verify the file still exists
                    if (info.getLocalPath() != null) {
                        File file = new File(info.getLocalPath());
                        if (file.exists() && file.length() > 0) {
                            downloadedVideos.add(info);
                            Log.d(TAG, "Found valid downloaded video: " + info.getTitle() + 
                                  " at " + info.getLocalPath());
                        } else {
                            Log.d(TAG, "Invalid or missing video file: " + info.getLocalPath());
                        }
                    }
                }
            }
            
            // Update UI
            if (downloadedVideos.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText(R.string.no_downloads_yet);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                adapter.updateData(downloadedVideos);
            }
        });
    }
    
    @Override
    public void onPlayVideo(VideoDownloadManager.VideoDownloadInfo video) {
        try {
            // Create content URI using FileProvider
            File videoFile = new File(video.getLocalPath());
            Uri videoUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".provider",
                videoFile
            );

            // Start CoursePlayerActivity with the local video URI
            Intent intent = new Intent(this, CoursePlayerActivity.class);
            intent.putExtra("videoUri", videoUri.toString());
            intent.putExtra("isLocalFile", true);
            intent.putExtra("title", video.getTitle());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error playing video: " + e.getMessage());
            Toast.makeText(this, "Impossible d'ouvrir la vidéo", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onDeleteVideo(VideoDownloadManager.VideoDownloadInfo video) {
        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Supprimer la vidéo")
                .setMessage("Êtes-vous sûr de vouloir supprimer cette vidéo téléchargée ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    // Delete the video
                    boolean deleted = downloadManager.deleteVideo(
                            video.getCourseId(), 
                            video.getSectionId()
                    );
                    
                    if (deleted) {
                        Toast.makeText(this, "Vidéo supprimée", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Échec de la suppression", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // If in offline mode, go back to login screen
            boolean isOfflineMode = getIntent().getBooleanExtra("offline_mode", false);
            if (isOfflineMode) {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            // Otherwise, normal back navigation
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 