package com.projet.skilllearn.view;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
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
        
        // Add these additional settings to help with downloads
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        
        // Enable downloads
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            Log.d(TAG, "Download triggered: " + url);
            Log.d(TAG, "MimeType: " + mimeType + ", Content-Disposition: " + contentDisposition);
            
            try {
                // Create a direct download intent
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
                
                // Show a message
                Toast.makeText(getContext(), "Téléchargement démarré via navigateur externe", Toast.LENGTH_LONG).show();
                
                // Start checking for new downloads
                startCheckingDownloads();
            } catch (Exception e) {
                Log.e(TAG, "Error starting download", e);
                Toast.makeText(getContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                
                Log.d(TAG, "Page loaded: " + url);
                
                // If we're on the ytmp3.la page, inject the YouTube URL and select MP4
                if (url.contains("ytmp3.la")) {
                    // Inject the YouTube URL into the input field, select MP4 format, and submit
                    String js = "javascript:(function() {" +
                                "console.log('Injecting URL: " + youtubeUrl + "');" +
                                "document.getElementById('v').value = '" + youtubeUrl + "';" +
                                "var formatBtn = document.getElementById('f');" +
                                "if(formatBtn && formatBtn.innerText === 'MP3') {" +
                                "  console.log('Clicking format button');" +
                                "  formatBtn.click();" + // This should toggle to MP4
                                "}" +
                                "})()";
                    
                    // Execute after a short delay to ensure the page is fully loaded
                    new Handler().postDelayed(() -> {
                        webView.evaluateJavascript(js, value -> {
                            Log.d(TAG, "JavaScript evaluation result: " + value);
                        });
                        
                        // Add a floating action button to submit the form
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                View submitButtonContainer = getLayoutInflater().inflate(R.layout.auto_submit_button, null);
                                Button autoSubmitButton = submitButtonContainer.findViewById(R.id.auto_submit_button);
                                
                                // Position the button on the screen
                                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT, 
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                );
                                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                                params.bottomMargin = 48;
                                submitButtonContainer.setLayoutParams(params);
                                
                                // Add click listener to submit the form
                                autoSubmitButton.setOnClickListener(v -> {
                                    String submitJs = "javascript:(function() {" +
                                                     "console.log('Submitting form');" +
                                                     "var submitBtn = document.querySelector('button[type=\"submit\"]');" +
                                                     "if(submitBtn) {" +
                                                     "  console.log('Submit button found');" +
                                                     "  submitBtn.click();" +
                                                     "} else {" +
                                                     "  console.log('Submit button not found');" +
                                                     "}" +
                                                     "})()";
                                    webView.evaluateJavascript(submitJs, result -> {
                                        Log.d(TAG, "Form submission result: " + result);
                                    });
                                    
                                    // Keep the button visible for additional attempts
                                    Toast.makeText(getContext(), "Conversion démarrée, attendez le lien de téléchargement", Toast.LENGTH_LONG).show();
                                });
                                
                                // Add a manual download instruction
                                TextView instructionText = new TextView(getContext());
                                instructionText.setText("Si le téléchargement ne démarre pas automatiquement, cliquez sur le lien de téléchargement qui apparaîtra.");
                                instructionText.setTextColor(Color.BLACK);
                                instructionText.setPadding(32, 16, 32, 16);
                                instructionText.setGravity(Gravity.CENTER);
                                
                                // Add to a layout
                                LinearLayout container = new LinearLayout(getContext());
                                container.setOrientation(LinearLayout.VERTICAL);
                                container.addView(instructionText);
                                container.addView(submitButtonContainer);
                                
                                // Add the layout to the dialog
                                FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT, 
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                );
                                containerParams.gravity = Gravity.BOTTOM;
                                container.setLayoutParams(containerParams);
                                
                                // Add to the webview parent
                                ((ViewGroup) webView.getParent()).addView(container);
                            });
                        }
                    }, 1000);
                }
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "URL loading: " + url);
                
                // If it's a download link, handle it specially
                if (url.contains(".mp4") || url.contains("download") || url.contains("api/convert")) {
                    Log.d(TAG, "Detected potential download URL: " + url);
                    // Let the download listener handle it
                    return false;
                }
                
                // For all other URLs, load in the WebView
                return false;
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "WebView error: " + description + " for URL: " + failingUrl);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }
        });
        
        // Set a WebChromeClient to handle JavaScript console messages
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d(TAG, "Console: " + consoleMessage.message() + " at " + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber());
                return true;
            }
        });
        
        // Load the ytmp3.la site
        webView.loadUrl("https://ytmp3.la/");
    }
    
    private void startCheckingDownloads() {
        // Cancel any existing timer
        if (checkDownloadTimer != null) {
            checkDownloadTimer.cancel();
        }
        
        Log.d(TAG, "Starting to check for downloads");
        
        // Create a new timer to check for downloads
        checkDownloadTimer = new Timer();
        checkDownloadTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForNewDownloads();
            }
        }, 3000, 3000); // Check every 3 seconds (reduced from 5)
    }
    
    private void checkForNewDownloads() {
        if (getContext() == null) return;
        
        Log.d(TAG, "Checking for new downloads...");
        
        // Get the Downloads directory
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        Log.d(TAG, "Downloads directory: " + downloadsDir.getAbsolutePath());
        
        // Get all files in the Downloads directory
        File[] files = downloadsDir.listFiles();
        if (files == null) {
            Log.d(TAG, "No files found in downloads directory");
            return;
        }
        
        Log.d(TAG, "Found " + files.length + " files in downloads directory");
        
        // Look for MP4 files that might be our download (more lenient timeframe - last 5 minutes)
        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(".mp4") && 
                file.lastModified() > System.currentTimeMillis() - 300000) { // Files modified in the last 5 minutes
                
                Log.d(TAG, "Found potential download: " + file.getName() + ", size: " + file.length() + " bytes");
                
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
        
        Log.d(TAG, "Handling downloaded file: " + file.getAbsolutePath());
        
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(), "Téléchargement terminé: " + file.getName(), Toast.LENGTH_LONG).show();
            
            // Show confirmation dialog
            new AlertDialog.Builder(getContext())
                .setTitle("Téléchargement terminé")
                .setMessage("Voulez-vous utiliser ce fichier pour le visionnage hors ligne?\n\nFichier: " + file.getName() + "\nTaille: " + (file.length() / 1024 / 1024) + " MB")
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