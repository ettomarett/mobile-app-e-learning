package com.projet.skilllearn.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
        
        // Get download manager instance
        downloadManager = VideoDownloadManager.getInstance(this);
        
        // Load downloaded videos
        loadDownloadedVideos();
    }
    
    private void loadDownloadedVideos() {
        // Check if user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vous devez être connecté pour voir vos téléchargements", Toast.LENGTH_SHORT).show();
            finish();
            return;
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
                        }
                    }
                }
            }
            
            // Update UI
            if (downloadedVideos.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                adapter.updateData(downloadedVideos);
            }
        });
    }
    
    @Override
    public void onPlayVideo(VideoDownloadManager.VideoDownloadInfo video) {
        // Open the video in course player or a standalone player
        if (video.getCourseId() != null && !video.getCourseId().isEmpty()) {
            // Open in course player
            Intent intent = new Intent(this, CoursePlayerActivity.class);
            intent.putExtra("courseId", video.getCourseId());
            intent.putExtra("sectionId", video.getSectionId());
            startActivity(intent);
        } else {
            // Open in default video player
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(video.getLocalPath()), "video/*");
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening video", e);
                Toast.makeText(this, "Impossible d'ouvrir la vidéo", Toast.LENGTH_SHORT).show();
            }
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
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 