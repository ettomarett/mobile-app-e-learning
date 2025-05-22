package com.projet.skilllearn.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.projet.skilllearn.R;
import com.projet.skilllearn.view.DownloadsActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Background service for downloading YouTube videos without UI
 */
public class HeadlessDownloadService extends Service {
    private static final String TAG = "HeadlessDownloadService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "download_channel";
    
    public static final String ACTION_START_DOWNLOAD = "com.projet.skilllearn.action.START_DOWNLOAD";
    public static final String ACTION_CANCEL_DOWNLOAD = "com.projet.skilllearn.action.CANCEL_DOWNLOAD";
    public static final String EXTRA_YOUTUBE_URL = "youtube_url";
    public static final String EXTRA_COURSE_ID = "course_id";
    public static final String EXTRA_SECTION_ID = "section_id";
    public static final String EXTRA_TITLE = "title";
    
    private WebView headlessWebView;
    private VideoDownloadManager downloadManager;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private String youtubeUrl;
    private String courseId;
    private String sectionId;
    private String title;
    private Timer downloadCheckTimer;
    private boolean conversionStarted = false;
    private int progressStage = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        // Initialize download manager
        downloadManager = VideoDownloadManager.getInstance(this);
        
        // Get notification manager
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Create notification channel for Android O and above
        createNotificationChannel();
        
        // Initialize WebView
        initHeadlessWebView();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        
        String action = intent.getAction();
        if (action == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        
        switch (action) {
            case ACTION_START_DOWNLOAD:
                youtubeUrl = intent.getStringExtra(EXTRA_YOUTUBE_URL);
                courseId = intent.getStringExtra(EXTRA_COURSE_ID);
                sectionId = intent.getStringExtra(EXTRA_SECTION_ID);
                title = intent.getStringExtra(EXTRA_TITLE);
                
                if (youtubeUrl == null || courseId == null || sectionId == null || title == null) {
                    Log.e(TAG, "Missing required parameters");
                    stopSelf();
                    return START_NOT_STICKY;
                }
                
                // Start foreground service with notification
                startForeground(NOTIFICATION_ID, createNotification());
                
                // Start the download process
                startDownloadProcess();
                break;
                
            case ACTION_CANCEL_DOWNLOAD:
                // Cancel the download and stop the service
                stopSelf();
                break;
        }
        
        return START_REDELIVER_INTENT;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        
        // Clean up resources
        if (headlessWebView != null) {
            headlessWebView.destroy();
            headlessWebView = null;
        }
        
        if (downloadCheckTimer != null) {
            downloadCheckTimer.cancel();
            downloadCheckTimer = null;
        }
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void initHeadlessWebView() {
        try {
            headlessWebView = new WebView(this);
            WebSettings settings = headlessWebView.getSettings();
            
            // Enable JavaScript and DOM storage
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setSupportMultipleWindows(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            settings.setAllowUniversalAccessFromFileURLs(true);
            
            // JavaScript interface for communication
            headlessWebView.addJavascriptInterface(new WebViewJavaScriptInterface(), "Android");
            
            // Set up WebView client
            headlessWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    Log.d(TAG, "Page loaded: " + url);
                    
                    // If we're on the ytmp3.la page, inject the YouTube URL and select MP4
                    if (url.contains("ytmp3.la")) {
                        if (!conversionStarted) {
                            injectUrlAndStartConversion();
                        }
                    }
                }
                
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Log.d(TAG, "URL loading: " + url);
                    return false;
                }
                
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Log.e(TAG, "WebView error: " + description + " for URL: " + failingUrl);
                    updateNotification("Erreur: " + description);
                    super.onReceivedError(view, errorCode, description, failingUrl);
                }
                
                @Nullable
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    String url = request.getUrl().toString();
                    
                    // Look for download URLs
                    if ((url.contains(".mp4") || url.contains("convert") || url.contains("download")) && !url.contains("api/")) {
                        Log.d(TAG, "Intercepted potential download URL: " + url);
                        try {
                            handleDownloadUrl(url);
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling download URL", e);
                        }
                    }
                    
                    return super.shouldInterceptRequest(view, request);
                }
            });
            
            headlessWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                    Log.d(TAG, "Console: " + consoleMessage.message());
                    return true;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing WebView", e);
        }
    }
    
    private void startDownloadProcess() {
        try {
            updateNotification("Initialisation de la conversion...");
            progressStage = 1;
            
            // Load the converter website
            headlessWebView.loadUrl("https://ytmp3.la/");
        } catch (Exception e) {
            Log.e(TAG, "Error starting download process", e);
            updateNotification("Erreur: Impossible de démarrer le processus");
            stopSelf();
        }
    }
    
    private void injectUrlAndStartConversion() {
        try {
            progressStage = 2;
            updateNotification("Préparation de la conversion...");
            
            // First check if the page is fully loaded
            String checkJs = "javascript:(function() {" +
                       "var inputElement = document.getElementById('v');" +
                       "var formatBtn = document.getElementById('f');" +
                       "if (!inputElement || !formatBtn) {" +
                       "  console.log('Page not fully loaded yet, input or format button missing');" +
                       "  return false;" +
                       "}" +
                       "return true;" +
                       "})()";
            
            headlessWebView.evaluateJavascript(checkJs, result -> {
                Log.d(TAG, "Check if page elements loaded: " + result);
                if ("true".equals(result)) {
                    // Page is loaded, proceed with URL injection
                    injectUrlAndProceed();
                } else {
                    // Page not loaded, wait a bit and try again
                    Log.d(TAG, "Page not fully loaded, waiting and trying again");
                    new Handler().postDelayed(this::injectUrlAndStartConversion, 2000);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in injectUrlAndStartConversion", e);
            updateNotification("Erreur: Échec de la conversion");
        }
    }
    
    private void injectUrlAndProceed() {
        // Inject the YouTube URL and select MP4 format
        String js = "javascript:(function() {" +
                   "console.log('Injecting URL: " + youtubeUrl + "');" +
                   "var inputElement = document.getElementById('v');" +
                   "if (!inputElement) {" +
                   "  console.log('Input element not found');" +
                   "  Android.onError('Input element not found');" +
                   "  return false;" +
                   "}" +
                   "inputElement.value = '" + youtubeUrl + "';" +
                   "var formatBtn = document.getElementById('f');" +
                   "if(formatBtn && formatBtn.innerText === 'MP3') {" +
                   "  console.log('Clicking format button');" +
                   "  formatBtn.click();" + // This should toggle to MP4
                   "}" +
                   "console.log('URL injected and format set, ready for conversion');" +
                   "return true;" +
                   "})()";
        
        headlessWebView.evaluateJavascript(js, result -> {
            Log.d(TAG, "JavaScript URL injection result: " + result);
            
            // After URL injection, click the submit button with a small delay
            if ("true".equals(result)) {
                new Handler().postDelayed(() -> {
                    clickConvertButton();
                }, 1000);
            } else {
                // Retry with a page reload if injection failed
                Log.d(TAG, "URL injection failed, reloading page");
                headlessWebView.loadUrl("https://ytmp3.la/");
                new Handler().postDelayed(this::injectUrlAndStartConversion, 3000);
            }
        });
    }
    
    private void clickConvertButton() {
        try {
            String submitJs = "javascript:(function() {" +
                        "console.log('Looking for submit button');" +
                        "var submitBtn = document.querySelector('button[type=\"submit\"]');" +
                        "if(submitBtn) {" +
                        "  console.log('Submit button found: ' + submitBtn.innerText);" +
                        "  submitBtn.click();" +
                        "  console.log('Submit button clicked');" +
                        "  Android.onConversionStarted();" +
                        "  return true;" +
                        "} else {" +
                        "  console.log('Submit button not found!');" +
                        "  var allButtons = document.querySelectorAll('button');" +
                        "  console.log('All buttons found: ' + allButtons.length);" +
                        "  for(var i=0; i < allButtons.length; i++) {" +
                        "    console.log('Button ' + i + ': ' + allButtons[i].innerText + ', type: ' + allButtons[i].getAttribute('type'));" +
                        "  }" +
                        "  Android.onError('Submit button not found');" +
                        "  return false;" +
                        "}" +
                        "})()";
            
            headlessWebView.evaluateJavascript(submitJs, result -> {
                Log.d(TAG, "Convert button click result: " + result);
                if (!"true".equals(result)) {
                    // If the convert button wasn't found or clicked, try again after a delay
                    new Handler().postDelayed(this::clickConvertButton, 2000);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error clicking convert button", e);
        }
    }
    
    private void checkForDownloadButton() {
        try {
            if (headlessWebView == null) return;
            
            String js = "javascript:(function() {" +
                       "console.log('Checking for download button');" +
                       "var downloadButtons = document.querySelectorAll('button');" +
                       "console.log('Found ' + downloadButtons.length + ' buttons total');" +
                       "if (downloadButtons.length === 0) {" +
                       "  console.log('No buttons found in main document, checking iframes');" +
                       "  var iframes = document.querySelectorAll('iframe');" +
                       "  console.log('Found ' + iframes.length + ' iframes');" +
                       "  for (var i = 0; i < iframes.length; i++) {" +
                       "    try {" +
                       "      var iframeDoc = iframes[i].contentDocument || iframes[i].contentWindow.document;" +
                       "      var iframeButtons = iframeDoc.querySelectorAll('button');" +
                       "      console.log('Found ' + iframeButtons.length + ' buttons in iframe ' + i);" +
                       "      for (var j = 0; j < iframeButtons.length; j++) {" +
                       "        var btn = iframeButtons[j];" +
                       "        console.log('Iframe ' + i + ' button ' + j + ' text: \"' + btn.innerText + '\"');" +
                       "        if (btn.innerText.trim().toLowerCase() === 'download' || " +
                       "            btn.innerText.toLowerCase().includes('download') || " +
                       "            btn.innerText.toLowerCase().includes('télécharger')) {" +
                       "          console.log('Found download button in iframe!');" +
                       "          btn.click();" +
                       "          console.log('Download button clicked');" +
                       "          Android.onDownloadButtonClicked();" +
                       "          return true;" +
                       "        }" +
                       "      }" +
                       "    } catch(e) { console.log('Error accessing iframe: ' + e.message); }" +
                       "  }" +
                       "  // Also check for anchor elements that might be download links" +
                       "  var anchors = document.querySelectorAll('a');" +
                       "  console.log('Found ' + anchors.length + ' anchor elements');" +
                       "  for (var i = 0; i < anchors.length; i++) {" +
                       "    var a = anchors[i];" +
                       "    console.log('Anchor ' + i + ' text: \"' + a.innerText + '\", href: ' + a.href);" +
                       "    if ((a.innerText.trim().toLowerCase() === 'download' || " +
                       "         a.innerText.toLowerCase().includes('download') || " +
                       "         a.innerText.toLowerCase().includes('télécharger')) && " +
                       "        (a.href.includes('.mp4') || a.href.includes('download'))) {" +
                       "      console.log('Found download link!');" +
                       "      a.click();" +
                       "      console.log('Download link clicked');" +
                       "      Android.onDownloadButtonClicked();" +
                       "      return true;" +
                       "    }" +
                       "  }" +
                       "  // Check page URL for any redirects" +
                       "  console.log('Current page URL: ' + window.location.href);" +
                       "  if (window.location.href.includes('download') || window.location.href.includes('convert')) {" +
                       "    console.log('On a download/convert page, looking for download elements');" +
                       "  }" +
                       "  return false;" +
                       "} else {" +
                       "  // Original button checking logic" +
                       "  for (var i = 0; i < downloadButtons.length; i++) {" +
                       "    var btn = downloadButtons[i];" +
                       "    console.log('Button ' + i + ' text: \"' + btn.innerText + '\"');" +
                       "    if (btn.innerText.trim().toLowerCase() === 'download' || " +
                       "        btn.innerText.toLowerCase().includes('download') || " +
                       "        btn.innerText.toLowerCase().includes('télécharger')) {" +
                       "      console.log('Found download button!');" +
                       "      btn.click();" +
                       "      console.log('Download button clicked');" +
                       "      Android.onDownloadButtonClicked();" +
                       "      return true;" +
                       "    }" +
                       "  }" +
                       "}" +
                       "return false;" +
                       "})()";
            
            headlessWebView.evaluateJavascript(js, result -> {
                Log.d(TAG, "Check for download button result: " + result);
                if ("true".equals(result)) {
                    progressStage = 4;
                    updateNotification("Démarrage du téléchargement...");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error checking for download button", e);
        }
    }
    
    private void handleDownloadUrl(String url) {
        progressStage = 5;
        updateNotification("Téléchargement en cours...");
        
        // Start a background thread to download the file
        new Thread(() -> {
            HttpURLConnection connection = null;
            InputStream input = null;
            FileOutputStream output = null;
            
            try {
                // Create a connection to the URL
                connection = (HttpURLConnection) new URL(url).openConnection();
                
                // Set cookies from WebView
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) {
                    connection.setRequestProperty("Cookie", cookies);
                }
                
                // Set up the connection
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.connect();
                
                // Check if the connection was successful
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "HTTP error code: " + responseCode);
                    mainHandler.post(() -> {
                        updateNotification("Erreur de téléchargement: " + responseCode);
                    });
                    return;
                }
                
                // Create a temporary file
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File tempFile = new File(downloadDir, "temp_" + System.currentTimeMillis() + ".mp4");
                
                // Download the file
                input = connection.getInputStream();
                output = new FileOutputStream(tempFile);
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                long fileSize = connection.getContentLength();
                
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    // Update progress
                    if (fileSize > 0) {
                        final int progress = (int) (totalBytesRead * 100 / fileSize);
                        mainHandler.post(() -> {
                            updateNotification("Téléchargement: " + progress + "%");
                        });
                    }
                }
                
                // Close streams
                output.close();
                input.close();
                
                // Handle the downloaded file
                final File downloadedFile = tempFile;
                mainHandler.post(() -> {
                    handleDownloadComplete(downloadedFile);
                });
                
            } catch (IOException e) {
                Log.e(TAG, "Error downloading file", e);
                mainHandler.post(() -> {
                    updateNotification("Erreur de téléchargement: " + e.getMessage());
                });
            } finally {
                // Clean up resources
                try {
                    if (output != null) {
                        output.close();
                    }
                    if (input != null) {
                        input.close();
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error closing streams", e);
                }
            }
        }).start();
    }
    
    private void handleDownloadComplete(File file) {
        try {
            progressStage = 6;
            updateNotification("Finalisation du téléchargement...");
            
            // Save the downloaded video to the app's storage
            boolean success = downloadManager.saveDownloadedVideo(
                    file,
                    courseId,
                    sectionId,
                    title
            );
            
            if (success) {
                // Show success notification
                updateNotification("Téléchargement terminé!", true);
                
                // Clean up temp file
                if (file.exists()) {
                    file.delete();
                }
                
                // Show toast
                mainHandler.post(() -> {
                    Toast.makeText(this, "Vidéo téléchargée avec succès", Toast.LENGTH_SHORT).show();
                });
                
                // Stop the service after a delay
                mainHandler.postDelayed(this::stopSelf, 5000);
            } else {
                updateNotification("Erreur lors de l'enregistrement de la vidéo");
                stopSelf();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling download complete", e);
            updateNotification("Erreur lors de la finalisation");
            stopSelf();
        }
    }
    
    private Notification createNotification() {
        try {
            // Create intent for notification click
            Intent notificationIntent = new Intent(this, DownloadsActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
            
            // Create cancel intent
            Intent cancelIntent = new Intent(this, HeadlessDownloadService.class);
            cancelIntent.setAction(ACTION_CANCEL_DOWNLOAD);
            PendingIntent cancelPendingIntent = PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE);
            
            // Build notification
            notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Téléchargement YouTube")
                    .setContentText("Préparation...")
                    .setSmallIcon(R.drawable.ic_download)
                    .setContentIntent(pendingIntent)
                    .addAction(R.drawable.ic_delete, "Annuler", cancelPendingIntent)
                    .setOngoing(true)
                    .setProgress(100, 0, true);
            
            return notificationBuilder.build();
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification", e);
            // Return a basic notification to avoid crashing
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Téléchargement YouTube")
                    .setContentText("En cours...")
                    .setSmallIcon(R.drawable.ic_download)
                    .build();
        }
    }
    
    private void updateNotification(String status) {
        updateNotification(status, false);
    }
    
    private void updateNotification(String status, boolean isComplete) {
        try {
            if (notificationBuilder == null || notificationManager == null) return;
            
            notificationBuilder.setContentText(status);
            
            // Update progress
            if (isComplete) {
                notificationBuilder.setProgress(0, 0, false)
                        .setOngoing(false)
                        .clearActions();
            } else {
                notificationBuilder.setProgress(6, progressStage, progressStage == 0);
            }
            
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification", e);
        }
    }
    
    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Téléchargements",
                        NotificationManager.IMPORTANCE_LOW);
                
                channel.setDescription("Téléchargements de vidéos YouTube");
                channel.enableLights(true);
                channel.setLightColor(Color.BLUE);
                
                notificationManager.createNotificationChannel(channel);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channel", e);
        }
    }
    
    /**
     * JavaScript interface for communication between WebView and Java
     */
    private class WebViewJavaScriptInterface {
        @JavascriptInterface
        public void onConversionStarted() {
            Log.d(TAG, "Conversion started");
            conversionStarted = true;
            progressStage = 3;
            
            mainHandler.post(() -> {
                updateNotification("Conversion en cours...");
                
                // Start a timer to check for the download button
                if (downloadCheckTimer != null) {
                    downloadCheckTimer.cancel();
                }
                
                // Use a dynamic approach: start with frequent checks and gradually reduce frequency
                downloadCheckTimer = new Timer();
                downloadCheckTimer.scheduleAtFixedRate(new TimerTask() {
                    // Start with a short interval of 1 second for the first 10 checks
                    private int checkCount = 0;
                    private final int INITIAL_CHECKS = 10;
                    
                    @Override
                    public void run() {
                        mainHandler.post(() -> {
                            checkForDownloadButton();
                            
                            // Log the current check count
                            Log.d(TAG, "Download button check #" + checkCount);
                            
                            checkCount++;
                        });
                    }
                }, 1000, 1000); // Check every 1 second initially
                
                // Add a watchdog timer that will restart the conversion if it takes too long
                new Handler().postDelayed(() -> {
                    if (progressStage < 4) { // If we haven't reached the download stage yet
                        Log.d(TAG, "Conversion taking too long, restarting the process");
                        if (downloadCheckTimer != null) {
                            downloadCheckTimer.cancel();
                            downloadCheckTimer = null;
                        }
                        conversionStarted = false;
                        
                        // Reload the page first before trying again
                        headlessWebView.loadUrl("https://ytmp3.la/");
                        
                        // Wait for the page to load before reinjecting
                        new Handler().postDelayed(() -> {
                            injectUrlAndStartConversion();
                        }, 3000);
                    }
                }, 60000); // Give it 1 minute before restarting
            });
        }
        
        @JavascriptInterface
        public void onDownloadButtonClicked() {
            Log.d(TAG, "Download button clicked");
            
            // Cancel the timer as we don't need to check for the button anymore
            if (downloadCheckTimer != null) {
                downloadCheckTimer.cancel();
                downloadCheckTimer = null;
            }
        }
        
        @JavascriptInterface
        public void onError(String error) {
            Log.e(TAG, "JavaScript error: " + error);
            
            mainHandler.post(() -> {
                updateNotification("Erreur: " + error);
                
                // If there's an error finding the submit button, try refreshing the page
                if (error.contains("Submit button not found") || error.contains("Cannot set properties of null")) {
                    new Handler().postDelayed(() -> {
                        Log.d(TAG, "Refreshing page after error: " + error);
                        headlessWebView.loadUrl("https://ytmp3.la/");
                    }, 3000);
                }
            });
        }
    }
} 