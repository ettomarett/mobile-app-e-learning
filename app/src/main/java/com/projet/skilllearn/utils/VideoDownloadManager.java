package com.projet.skilllearn.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Manager for downloading videos for offline viewing
 */
public class VideoDownloadManager {
    private static final String TAG = "VideoDownloadManager";
    private static VideoDownloadManager instance;
    
    private final Context context;
    private final DownloadManager downloadManager;
    private final Map<Long, VideoDownloadInfo> activeDownloads = new ConcurrentHashMap<>();
    private final Map<String, VideoDownloadInfo> completedDownloads = new ConcurrentHashMap<>();
    private final MutableLiveData<Map<String, VideoDownloadInfo>> downloadStatusLiveData = new MutableLiveData<>(new HashMap<>());
    
    private BroadcastReceiver downloadCompleteReceiver;
    
    /**
     * Get singleton instance
     * @param context Application context
     * @return VideoDownloadManager instance
     */
    public static synchronized VideoDownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new VideoDownloadManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor
     * @param context Application context
     */
    private VideoDownloadManager(Context context) {
        this.context = context;
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        
        // Register download complete receiver
        downloadCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId != -1) {
                    handleDownloadComplete(downloadId);
                }
            }
        };
        
        // Use ContextCompat to register receiver with RECEIVER_NOT_EXPORTED flag
        ContextCompat.registerReceiver(
                context, 
                downloadCompleteReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
        
        // Load completed downloads from storage
        loadCompletedDownloads();
    }
    
    /**
     * Download a video for offline viewing
     * @param videoUrl URL of the video to download
     * @param courseId Course ID
     * @param sectionId Section ID
     * @param title Video title
     * @return Download ID
     */
    public long downloadVideo(String videoUrl, String courseId, String sectionId, String title) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            Log.e(TAG, "Cannot download video: URL is empty");
            return -1;
        }
        
        // Check if video is from YouTube
        if (isYouTubeUrl(videoUrl)) {
            Log.e(TAG, "YouTube videos cannot be downloaded directly");
            return -1;
        }
        
        // Check if already downloaded
        String fileKey = generateFileKey(courseId, sectionId);
        if (isVideoDownloaded(courseId, sectionId)) {
            Log.d(TAG, "Video already downloaded: " + fileKey);
            return -1;
        }
        
        // Create filename using courseId and sectionId only
        String fileName = courseId + "_" + sectionId + ".mp4";
        
        // Set up download request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(videoUrl))
                .setTitle("Téléchargement du cours: " + title)
                .setDescription("Téléchargement en cours...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false);
        
        // Start download
        long downloadId = downloadManager.enqueue(request);
        
        // Save download info
        VideoDownloadInfo downloadInfo = new VideoDownloadInfo(
                downloadId, courseId, sectionId, title, videoUrl, fileName, 0
        );
        activeDownloads.put(downloadId, downloadInfo);
        
        // Update live data
        updateDownloadStatus(downloadInfo);
        
        return downloadId;
    }
    
    /**
     * Check if a video is already downloaded
     * @param courseId Course ID
     * @param sectionId Section ID
     * @return true if downloaded, false otherwise
     */
    public boolean isVideoDownloaded(String courseId, String sectionId) {
        String fileKey = generateFileKey(courseId, sectionId);
        
        // Check if in completed downloads
        if (completedDownloads.containsKey(fileKey)) {
            // Verify file still exists
            VideoDownloadInfo info = completedDownloads.get(fileKey);
            if (info != null) {
                File file = new File(info.getLocalPath());
                if (file.exists() && file.length() > 0) {
                    return true;
                } else {
                    // File is missing, remove from completed downloads
                    completedDownloads.remove(fileKey);
                    updateLiveData();
                }
            }
        }
        
        return false;
    }
    
    /**
     * Get local path for a downloaded video
     * @param courseId Course ID
     * @param sectionId Section ID
     * @return Local file path or null if not downloaded
     */
    public String getLocalVideoPath(String courseId, String sectionId) {
        String fileKey = generateFileKey(courseId, sectionId);
        VideoDownloadInfo info = completedDownloads.get(fileKey);
        
        if (info != null) {
            File file = new File(info.getLocalPath());
            if (file.exists() && file.length() > 0) {
                return info.getLocalPath();
            } else {
                // File is missing, remove from completed downloads
                completedDownloads.remove(fileKey);
                updateLiveData();
            }
        }
        
        return null;
    }
    
    /**
     * Save a downloaded video file to our storage location
     * @param downloadedFile The downloaded file
     * @param courseId Course ID
     * @param sectionId Section ID
     * @param title Video title
     * @return True if successful, false otherwise
     */
    public boolean saveDownloadedVideo(File downloadedFile, String courseId, String sectionId, String title) {
        if (downloadedFile == null || !downloadedFile.exists()) {
            Log.e(TAG, "Cannot save video: File does not exist");
            return false;
        }
        
        // Create filename using courseId and sectionId only
        String fileName = courseId + "_" + sectionId + ".mp4";
        
        // Get destination directory
        File destDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (destDir == null) {
            Log.e(TAG, "Cannot save video: External files directory not available");
            return false;
        }
        
        // Create destination file
        File destFile = new File(destDir, fileName);
        
        try {
            // Copy file using Java NIO for efficiency
            java.nio.file.Files.copy(
                downloadedFile.toPath(),
                destFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            
            // Create download info
            VideoDownloadInfo downloadInfo = new VideoDownloadInfo(
                    -1, // No download ID since it was manually downloaded
                    courseId,
                    sectionId,
                    title,
                    "manual_download", // Placeholder URL
                    fileName,
                    VideoDownloadInfo.STATUS_COMPLETED
            );
            downloadInfo.setLocalPath(destFile.getAbsolutePath());
            
            // Add to completed downloads
            String fileKey = generateFileKey(courseId, sectionId);
            completedDownloads.put(fileKey, downloadInfo);
            
            // Update Firebase record
            updateFirebaseDownloadStatus(courseId, sectionId, true);
            
            // Update live data
            updateLiveData();
            
            Log.d(TAG, "Manually downloaded video saved: " + fileKey);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving downloaded video", e);
            return false;
        }
    }
    
    /**
     * Delete a downloaded video
     * @param courseId Course ID
     * @param sectionId Section ID
     * @return true if deleted, false otherwise
     */
    public boolean deleteVideo(String courseId, String sectionId) {
        String fileKey = generateFileKey(courseId, sectionId);
        VideoDownloadInfo info = completedDownloads.get(fileKey);
        
        if (info != null) {
            File file = new File(info.getLocalPath());
            boolean deleted = file.delete();
            
            if (deleted) {
                completedDownloads.remove(fileKey);
                updateLiveData();
                
                // Update Firebase record
                updateFirebaseDownloadStatus(courseId, sectionId, false);
            }
            
            return deleted;
        }
        
        return false;
    }
    
    /**
     * Get download status LiveData
     * @return LiveData with download status
     */
    public LiveData<Map<String, VideoDownloadInfo>> getDownloadStatus() {
        return downloadStatusLiveData;
    }
    
    /**
     * Handle completed download
     * @param downloadId Download ID
     */
    private void handleDownloadComplete(long downloadId) {
        VideoDownloadInfo info = activeDownloads.get(downloadId);
        if (info == null) {
            return;
        }
        
        // Check download status
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);
        
        if (cursor.moveToFirst()) {
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
            int localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
            
            int status = cursor.getInt(statusIndex);
            int reason = cursor.getInt(reasonIndex);
            
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                // Get file path
                String localUri = cursor.getString(localUriIndex);
                String localPath = Uri.parse(localUri).getPath();
                
                // Update download info
                info.setStatus(VideoDownloadInfo.STATUS_COMPLETED);
                info.setLocalPath(localPath);
                
                // Add to completed downloads
                String fileKey = generateFileKey(info.getCourseId(), info.getSectionId());
                completedDownloads.put(fileKey, info);
                
                // Update Firebase record
                updateFirebaseDownloadStatus(info.getCourseId(), info.getSectionId(), true);
                
                Log.d(TAG, "Download completed: " + fileKey);
            } else if (status == DownloadManager.STATUS_FAILED) {
                // Update download info
                info.setStatus(VideoDownloadInfo.STATUS_FAILED);
                info.setErrorReason(reason);
                
                Log.e(TAG, "Download failed: " + info.getTitle() + ", reason: " + reason);
            }
        }
        
        cursor.close();
        
        // Remove from active downloads
        activeDownloads.remove(downloadId);
        
        // Update live data
        updateLiveData();
    }
    
    /**
     * Update download status in Firebase
     */
    private void updateFirebaseDownloadStatus(String courseId, String sectionId, boolean isDownloaded) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        
        DatabaseReference userDownloadsRef = FirebaseDatabase.getInstance()
                .getReference("user_downloads")
                .child(user.getUid())
                .child(courseId)
                .child(sectionId);
        
        if (isDownloaded) {
            Map<String, Object> downloadData = new HashMap<>();
            downloadData.put("downloaded", true);
            downloadData.put("timestamp", System.currentTimeMillis());
            
            userDownloadsRef.setValue(downloadData);
        } else {
            userDownloadsRef.removeValue();
        }
    }
    
    /**
     * Update live data with current status
     */
    private void updateLiveData() {
        Map<String, VideoDownloadInfo> allDownloads = new HashMap<>(completedDownloads);
        
        // Add active downloads
        for (VideoDownloadInfo info : activeDownloads.values()) {
            String fileKey = generateFileKey(info.getCourseId(), info.getSectionId());
            allDownloads.put(fileKey, info);
        }
        
        downloadStatusLiveData.postValue(allDownloads);
    }
    
    /**
     * Update download status for a specific download
     */
    private void updateDownloadStatus(VideoDownloadInfo info) {
        Map<String, VideoDownloadInfo> currentDownloads = downloadStatusLiveData.getValue();
        if (currentDownloads == null) {
            currentDownloads = new HashMap<>();
        }
        
        String fileKey = generateFileKey(info.getCourseId(), info.getSectionId());
        currentDownloads.put(fileKey, info);
        
        downloadStatusLiveData.postValue(currentDownloads);
    }
    
    /**
     * Load completed downloads from storage
     */
    private void loadCompletedDownloads() {
        // Clear existing data
        completedDownloads.clear();
        
        // Get the movies directory
        File moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (moviesDir == null || !moviesDir.exists()) {
            Log.d(TAG, "Movies directory does not exist, creating it");
            if (moviesDir != null) {
                moviesDir.mkdirs();
            }
            return;
        }
        
        // Look for downloaded files
        File[] files = moviesDir.listFiles();
        if (files == null) {
            Log.d(TAG, "No files found in movies directory");
            return;
        }

        // Scan for all video files
        for (File file : files) {
            if (file.isFile() && isVideoFile(file.getName())) {
                try {
                    // Generate a unique ID for this video
                    String uniqueId = file.getName().replace(".mp4", "");
                    
                    // Create a download info object with a friendly display name
                    VideoDownloadInfo info = new VideoDownloadInfo(
                        -1, // No download ID for existing files
                        uniqueId, // Use filename as courseId
                        uniqueId, // Use filename as sectionId
                        "Vidéo téléchargée " + formatFileSize(file.length()), // User-friendly title with file size
                        "local://video", // Local URL
                        file.getAbsolutePath(),
                        VideoDownloadInfo.STATUS_COMPLETED
                    );
                    info.setLocalPath(file.getAbsolutePath());
                    
                    // Add to completed downloads
                    String fileKey = generateFileKey(uniqueId, uniqueId);
                    completedDownloads.put(fileKey, info);
                    Log.d(TAG, "Found video file: " + file.getName() + " at " + file.getAbsolutePath());
                } catch (Exception e) {
                    Log.e(TAG, "Error processing video file: " + file.getName(), e);
                }
            }
        }
        
        // Update live data
        updateLiveData();
    }
    
    /**
     * Format file size in a human-readable format
     */
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    
    /**
     * Check if a file is a video file based on its extension
     */
    private boolean isVideoFile(String fileName) {
        String[] videoExtensions = {".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m4v"};
        fileName = fileName.toLowerCase();
        for (String extension : videoExtensions) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Load downloads from Firebase
     */
    private void loadDownloadsFromFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d(TAG, "No user logged in, skipping Firebase download check");
            return;
        }
        
        DatabaseReference userDownloadsRef = FirebaseDatabase.getInstance()
                .getReference("user_downloads")
                .child(user.getUid());
        
        userDownloadsRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                for (com.google.firebase.database.DataSnapshot courseSnapshot : snapshot.getChildren()) {
                    String courseId = courseSnapshot.getKey();
                    
                    for (com.google.firebase.database.DataSnapshot sectionSnapshot : courseSnapshot.getChildren()) {
                        String sectionId = sectionSnapshot.getKey();
                        String fileKey = generateFileKey(courseId, sectionId);
                        
                        // Check if file exists
                        String fileName = sectionId + ".mp4";
                        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), fileName);
                        
                        if (file.exists() && file.length() > 0) {
                            // Create download info
                            VideoDownloadInfo info = new VideoDownloadInfo(
                                    -1, courseId, sectionId, "Vidéo du cours", 
                                    "", file.getAbsolutePath(), VideoDownloadInfo.STATUS_COMPLETED
                            );
                            
                            // Add to completed downloads
                            completedDownloads.put(fileKey, info);
                            Log.d(TAG, "Found Firebase video record: " + fileKey);
                        } else {
                            // File is missing, remove from Firebase
                            sectionSnapshot.getRef().removeValue();
                            Log.d(TAG, "Removing missing video from Firebase: " + fileKey);
                        }
                    }
                }
                
                // Update live data
                updateLiveData();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading downloads from Firebase", e);
        });
    }
    
    /**
     * Generate a unique key for the file
     */
    private String generateFileKey(String courseId, String sectionId) {
        return courseId + "_" + sectionId;
    }
    
    /**
     * Check if URL is a YouTube URL
     */
    private boolean isYouTubeUrl(String url) {
        return url != null && (url.contains("youtube.com") || url.contains("youtu.be"));
    }
    
    /**
     * Sanitize filename by removing invalid characters
     */
    private String sanitizeFileName(String input) {
        return input.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        try {
            if (downloadCompleteReceiver != null) {
                context.unregisterReceiver(downloadCompleteReceiver);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
    }
    
    /**
     * Video download information class
     */
    public static class VideoDownloadInfo {
        public static final int STATUS_PENDING = 0;
        public static final int STATUS_DOWNLOADING = 1;
        public static final int STATUS_PAUSED = 2;
        public static final int STATUS_COMPLETED = 3;
        public static final int STATUS_FAILED = 4;
        
        private final long downloadId;
        private final String courseId;
        private final String sectionId;
        private final String title;
        private final String url;
        private final String fileName;
        private int status;
        private int progress;
        private String localPath;
        private int errorReason;
        
        public VideoDownloadInfo(long downloadId, String courseId, String sectionId, 
                               String title, String url, String fileName, int status) {
            this.downloadId = downloadId;
            this.courseId = courseId;
            this.sectionId = sectionId;
            this.title = title;
            this.url = url;
            this.fileName = fileName;
            this.status = status;
            this.progress = 0;
        }
        
        // Getters and setters
        public long getDownloadId() { return downloadId; }
        public String getCourseId() { return courseId; }
        public String getSectionId() { return sectionId; }
        public String getTitle() { return title; }
        public String getUrl() { return url; }
        public String getFileName() { return fileName; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public String getLocalPath() { return localPath; }
        public void setLocalPath(String localPath) { this.localPath = localPath; }
        public int getErrorReason() { return errorReason; }
        public void setErrorReason(int errorReason) { this.errorReason = errorReason; }
    }
} 