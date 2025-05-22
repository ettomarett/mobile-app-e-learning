package com.projet.skilllearn.view;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.projet.skilllearn.R;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Dialog fragment that opens a YouTube downloader site for manual video download
 */
public class YouTubeDownloadDialog extends DialogFragment {
    private static final String TAG = "YouTubeDownloadDialog";
    private static final String ARG_YOUTUBE_URL = "youtube_url";
    private static final String ARG_COURSE_ID = "course_id";
    private static final String ARG_SECTION_ID = "section_id";
    private static final String ARG_TITLE = "title";

    private String youtubeUrl;
    private String courseId;
    private String sectionId;
    private String title;
    private WebView webView;
    private ProgressBar progressBar;
    private Button closeButton;
    private DownloadCompleteListener listener;
    private Timer checkDownloadTimer;

    /**
     * Interface for download completion callback
     */
    public interface DownloadCompleteListener {
        void onDownloadComplete(File downloadedFile);
    }

    /**
     * Create a new instance of the dialog
     */
    public static YouTubeDownloadDialog newInstance(String youtubeUrl, String courseId, String sectionId, String title) {
        YouTubeDownloadDialog dialog = new YouTubeDownloadDialog();
        Bundle args = new Bundle();
        args.putString(ARG_YOUTUBE_URL, youtubeUrl);
        args.putString(ARG_COURSE_ID, courseId);
        args.putString(ARG_SECTION_ID, sectionId);
        args.putString(ARG_TITLE, title);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);

        if (getArguments() != null) {
            youtubeUrl = getArguments().getString(ARG_YOUTUBE_URL);
            courseId = getArguments().getString(ARG_COURSE_ID);
            sectionId = getArguments().getString(ARG_SECTION_ID);
            title = getArguments().getString(ARG_TITLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_youtube_download, container, false);
        
        webView = view.findViewById(R.id.webView);
        progressBar = view.findViewById(R.id.progressBar);
        closeButton = view.findViewById(R.id.closeButton);
        
        setupWebView();
        
        closeButton.setOnClickListener(v -> dismiss());
        
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (DownloadCompleteListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement DownloadCompleteListener");
        }
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        });
        
        // Listen for downloads
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            Log.d(TAG, "Download triggered: " + url);
            Toast.makeText(getContext(), "Téléchargement démarré. Veuillez attendre...", Toast.LENGTH_LONG).show();
            
            // Start checking for new downloads
            startCheckingDownloads();
        });
        
        // Convert YouTube URL to downloader URL by adding "ss" after "www."
        String modifiedUrl = youtubeUrl.replace("www.youtube.com", "www.ssyoutube.com");
        
        // Load the modified URL
        webView.loadUrl(modifiedUrl);
    }
    
    private void startCheckingDownloads() {
        // Cancel any existing timer
        if (checkDownloadTimer != null) {
            checkDownloadTimer.cancel();
        }
        
        // Create a new timer to check for downloads
        checkDownloadTimer = new Timer();
        checkDownloadTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForNewDownloads();
            }
        }, 5000, 5000); // Check every 5 seconds
    }
    
    private void checkForNewDownloads() {
        if (getContext() == null) return;
        
        // Get the Downloads directory
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        
        // Get all files in the Downloads directory
        File[] files = downloadsDir.listFiles();
        if (files == null) return;
        
        // Look for MP4 files that might be our download
        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(".mp4") && 
                file.lastModified() > System.currentTimeMillis() - 60000) { // Files modified in the last minute
                
                // Found a potential download
                handleDownloadedFile(file);
                
                // Cancel the timer
                if (checkDownloadTimer != null) {
                    checkDownloadTimer.cancel();
                    checkDownloadTimer = null;
                }
                
                break;
            }
        }
    }
    
    private void handleDownloadedFile(File file) {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(), "Téléchargement terminé: " + file.getName(), Toast.LENGTH_LONG).show();
            
            // Show confirmation dialog
            new AlertDialog.Builder(getContext())
                .setTitle("Téléchargement terminé")
                .setMessage("Voulez-vous utiliser ce fichier pour le visionnage hors ligne?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    // Notify the listener
                    listener.onDownloadComplete(file);
                    dismiss();
                })
                .setNegativeButton("Non", (dialog, which) -> dialog.dismiss())
                .show();
        });
    }
    
    @Override
    public void onDestroyView() {
        // Clean up the timer
        if (checkDownloadTimer != null) {
            checkDownloadTimer.cancel();
            checkDownloadTimer = null;
        }
        
        super.onDestroyView();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }
} 