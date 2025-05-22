package com.projet.skilllearn.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.projet.skilllearn.R;
import com.projet.skilllearn.model.Achievement;
import com.projet.skilllearn.model.Course;
import com.projet.skilllearn.model.CourseSection;
import com.projet.skilllearn.utils.UserProgressManager;
import com.projet.skilllearn.view.adapters.CoursePagerAdapter;
import com.projet.skilllearn.view.adapters.CourseSectionAdapter;
import com.projet.skilllearn.view.fragments.ContentFragment;
import com.projet.skilllearn.view.fragments.DownloadsFragment;
import com.projet.skilllearn.view.fragments.QuizFragment;
import com.projet.skilllearn.viewmodel.CourseViewModel;
import com.projet.skilllearn.utils.VideoDownloadManager;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Button;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoursePlayerActivity extends AppCompatActivity implements
        CourseSectionAdapter.OnSectionClickListener,
        Player.Listener,
        UserProgressManager.BadgeAwardedListener,
        YouTubeDownloadDialog.DownloadCompleteListener,
        DownloadsFragment.DownloadActionListener {

    private static final String TAG = "CoursePlayerActivity";
    private static final int STORAGE_PERMISSION_CODE = 1001;

    private FrameLayout videoContainer;
    private PlayerView playerView;
    private FrameLayout youtubePlayerContainer;
    private YouTubePlayerView youtubePlayerView;
    private ExoPlayer player;
    private TextView tvTitle;
    private TextView tvDescription;
    private ProgressBar progressBar;
    private Button btnPrevious;
    private Button btnNext;
    private MaterialButton btnMarkComplete;
    private Button btnDownloadVideo;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private RecyclerView rvSections;

    private CourseViewModel viewModel;
    private UserProgressManager progressManager;
    private VideoDownloadManager downloadManager;
    private String courseId;
    private String sectionId;
    private List<CourseSection> sections;
    private int currentSectionIndex = 0;
    private boolean videoCompleted = false;
    private YouTubePlayer youTubePlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_player);

        // Récupérer les identifiants depuis l'intent
        courseId = getIntent().getStringExtra("courseId");
        sectionId = getIntent().getStringExtra("sectionId");

        if (courseId == null) {
            Toast.makeText(this, "Erreur: ID de cours manquant", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialiser les vues
        initViews();

        // Initialiser le ViewModel
        viewModel = new ViewModelProvider(this).get(CourseViewModel.class);

        // Initialiser le gestionnaire de progrès
        progressManager = UserProgressManager.getInstance();
        progressManager.setBadgeAwardedListener(this);

        // Initialiser le gestionnaire de téléchargement
        downloadManager = VideoDownloadManager.getInstance(this);

        // Initialiser les lecteurs vidéo
        initializePlayer();

        // Configurer les onglets et ViewPager
        setupViewPager();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Activer le bouton de navigation vers le haut
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        // Observer les données du ViewModel
        observeViewModel();

        // Observer les statuts de téléchargement
        observeDownloadStatus();

        // Charger le cours
        viewModel.selectCourse(courseId);
    }

    private void initViews() {
        try {
            videoContainer = findViewById(R.id.video_container);
            playerView = findViewById(R.id.player_view);
            youtubePlayerContainer = findViewById(R.id.youtube_player_container);
            progressBar = findViewById(R.id.progress_bar);
            btnPrevious = findViewById(R.id.btn_previous);
            btnNext = findViewById(R.id.btn_next);
            btnMarkComplete = findViewById(R.id.btn_mark_complete);
            viewPager = findViewById(R.id.view_pager);
            tabLayout = findViewById(R.id.tab_layout);
            rvSections = findViewById(R.id.rv_sections);

            // Configurer RecyclerView avec orientation horizontale comme spécifié dans le XML
            rvSections.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

            // Configurer les listeners
            btnPrevious.setOnClickListener(v -> navigateToPreviousSection());
            btnNext.setOnClickListener(v -> navigateToNextSection());
            btnMarkComplete.setOnClickListener(v -> markSectionAsCompleted());
        } catch (Exception e) {
            Toast.makeText(this, "Erreur d'initialisation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializePlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.addListener(this);
    }

    private void setupViewPager() {
        Log.d("CoursePlayerActivity", "Configuration du ViewPager");

        // Configurer l'adaptateur
        CoursePagerAdapter pagerAdapter = new CoursePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Conserver les fragments en mémoire
        viewPager.setOffscreenPageLimit(4);

        // Configurer les onglets
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            String tabText;
            switch (position) {
                case 0:
                    tabText = getString(R.string.content);
                    break;
                case 1:
                    tabText = getString(R.string.notes);
                    break;
                case 2:
                    tabText = getString(R.string.quiz);
                    break;
                case 3:
                    tabText = getString(R.string.assistant_ia);
                    break;
                case 4:
                    tabText = getString(R.string.downloads);
                    break;
                default:
                    tabText = "Tab " + (position + 1);
                    break;
            }
            tab.setText(tabText);
        }).attach();

        // Écouter les changements d'onglet
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Log.d("CoursePlayerActivity", "Page sélectionnée: " + position);
            }
        });
    }

    private String getTabContentDescription(int tabPosition) {
        switch (tabPosition) {
            case 0:
                return getString(R.string.content_tab_description);
            case 1:
                return getString(R.string.notes_tab_description);
            case 2:
                return getString(R.string.quiz_tab_description);
            case 3:
                return getString(R.string.assistant_ia_tab_description);
            case 4:
                return getString(R.string.downloads);
            default:
                return "Tab " + (tabPosition + 1);
        }
    }

    private void observeViewModel() {
        // Observer le cours sélectionné
        viewModel.getSelectedCourse().observe(this, course -> {
            if (course != null) {
                loadSections(course);
            }
        });

        // Observer l'état de chargement
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observer les erreurs
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });
    }

    private void loadSections(Course course) {
        sections = course.getSections();
        if (sections == null || sections.isEmpty()) {
            Toast.makeText(this, "Ce cours ne contient aucune section", Toast.LENGTH_SHORT).show();
            return;
        }

        // Configurer l'adaptateur des sections
        CourseSectionAdapter adapter = new CourseSectionAdapter(this, sections, this);
        rvSections.setAdapter(adapter);

        // Déterminer l'index de la section initiale
        if (sectionId != null) {
            for (int i = 0; i < sections.size(); i++) {
                if (sections.get(i).getSectionId().equals(sectionId)) {
                    currentSectionIndex = i;
                    break;
                }
            }
        }

        // Charger la section initiale
        loadSection(currentSectionIndex);
    }

    private void loadSection(int index) {
        if (index < 0 || index >= sections.size()) {
            return;
        }

        CourseSection section = sections.get(currentSectionIndex);
        currentSectionIndex = index;

        if (rvSections.getAdapter() instanceof CourseSectionAdapter) {
            CourseSectionAdapter adapter = (CourseSectionAdapter) rvSections.getAdapter();
            adapter.setSelectedPosition(index);

            // Faire défiler jusqu'à la position sélectionnée
            rvSections.smoothScrollToPosition(index);
        }
        
        // Mettre à jour le contenu des fragments
        refreshFragments(section);

        // Charger la vidéo si disponible
        String videoUrl = section.getVideoUrl();
        if (videoUrl != null && !videoUrl.isEmpty()) {
            videoContainer.setVisibility(View.VISIBLE);
            if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
                setupYouTubePlayer(videoUrl);
            } else {
                setupExoPlayer(videoUrl);
            }
        } else {
            videoContainer.setVisibility(View.GONE);
            stopAllPlayers();
        }

        // Mettre à jour les boutons de navigation
        updateNavigationButtons();
        updateCompleteButtonState();
    }

    private void setupYouTubePlayer(String videoUrl) {
        // Arrêter d'abord tous les lecteurs
        stopAllPlayers();

        playerView.setVisibility(View.GONE);
        youtubePlayerContainer.setVisibility(View.VISIBLE);

        String videoId = getYouTubeVideoId(videoUrl);
        if (videoId == null) {
            Toast.makeText(this, "ID de vidéo YouTube invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        // Créer et initialiser le lecteur YouTube
        if (youtubePlayerView == null) {
            youtubePlayerView = new YouTubePlayerView(this);
            youtubePlayerContainer.removeAllViews(); // Assurez-vous que le conteneur est vide
            youtubePlayerContainer.addView(youtubePlayerView);
            getLifecycle().addObserver(youtubePlayerView);

            youtubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onReady(@NonNull YouTubePlayer player) {
                    youTubePlayer = player;
                    player.loadVideo(videoId, 0);
                }

                @Override
                public void onStateChange(@NonNull YouTubePlayer player, @NonNull PlayerConstants.PlayerState state) {
                    if (state == PlayerConstants.PlayerState.ENDED) {
                        videoCompleted = true;
                        updateCompleteButtonState();
                        showVideoCompletedDialog();
                    }
                }

                @Override
                public void onError(@NonNull YouTubePlayer player, @NonNull PlayerConstants.PlayerError error) {
                    Log.e(TAG, "YouTube player error: " + error.name());
                    Toast.makeText(CoursePlayerActivity.this, 
                        "Erreur lors de la lecture de la vidéo: " + error.name(), 
                        Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            if (youTubePlayer != null) {
                youTubePlayer.loadVideo(videoId, 0);
            }
        }
    }

    private void setupExoPlayer(String videoUrl) {
        // Arrêter d'abord tous les lecteurs
        stopAllPlayers();

        playerView.setVisibility(View.VISIBLE);
        youtubePlayerContainer.setVisibility(View.GONE);

        // Vérifier si la vidéo est disponible hors ligne
        String localPath = downloadManager.getLocalVideoPath(courseId, sections.get(currentSectionIndex).getSectionId());
        if (localPath != null && new File(localPath).exists()) {
            // Charger la vidéo locale
            MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(new File(localPath)));
            player.setMediaItem(mediaItem);
        } else {
            // Charger la vidéo depuis l'URL
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoUrl));
            player.setMediaItem(mediaItem);
        }
        
        player.prepare();
        player.play();
    }

    private void loadStandardVideo(String videoUrl) {
        // Arrêter d'abord tous les lecteurs
        stopAllPlayers();

        playerView.setVisibility(View.VISIBLE);
        youtubePlayerContainer.setVisibility(View.GONE);

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoUrl));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void loadLocalVideo(String localPath) {
        // Arrêter d'abord tous les lecteurs
        stopAllPlayers();

        playerView.setVisibility(View.VISIBLE);
        youtubePlayerContainer.setVisibility(View.GONE);

        File localFile = new File(localPath);
        if (!localFile.exists()) {
            Toast.makeText(this, "Erreur: Fichier vidéo local introuvable", Toast.LENGTH_SHORT).show();
            return;
        }

        MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(localFile));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void stopAllPlayers() {
        // Arrêter ExoPlayer
        if (player != null) {
            player.stop();
            player.clearMediaItems();
        }

        // Arrêter YouTube Player
        if (youTubePlayer != null) {
            youTubePlayer.pause();
        }
    }

    private void refreshFragments(CourseSection section) {
        // Mettre à jour le fragment de contenu
        ContentFragment contentFragment = ContentFragment.getInstance();
        if (contentFragment != null) {
            contentFragment.updateContent(section.getContent());
        }

        // Mettre à jour le fragment de quiz
        QuizFragment quizFragment = QuizFragment.getInstance();
        if (quizFragment != null && section.getQuiz() != null) {
            quizFragment.updateQuiz(section.getQuiz());
        }
    }

    private void showVideoCompletedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Vidéo terminée")
                .setMessage("Voulez-vous marquer cette section comme terminée ?")
                .setPositiveButton("Oui", (dialog, which) -> markSectionAsCompleted())
                .setNegativeButton("Non", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private boolean isYouTubeUrl(String url) {
        String pattern = "(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);
        return matcher.find();
    }

    private String getYouTubeVideoId(String url) {
        String pattern = "(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void updateNavigationButtons() {
        btnPrevious.setEnabled(currentSectionIndex > 0);
        btnNext.setEnabled(currentSectionIndex < sections.size() - 1);
    }

    private void updateCompleteButtonState() {
        // Enable the button regardless of video completion status
        btnMarkComplete.setEnabled(true);
    }

    private void navigateToPreviousSection() {
        if (currentSectionIndex > 0) {
            loadSection(currentSectionIndex - 1);
        }
    }

    private void navigateToNextSection() {
        if (currentSectionIndex < sections.size() - 1) {
            loadSection(currentSectionIndex + 1);
        }
    }

    private void markSectionAsCompleted() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Veuillez vous connecter pour enregistrer votre progression", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String sectionId = sections.get(currentSectionIndex).getSectionId();

        // Change button state to indicate it's processing
        btnMarkComplete.setEnabled(false);
        btnMarkComplete.setText(R.string.marking_complete);

        progressManager.markSectionCompleted(courseId, sectionId, sections.size());

        // Re-enable button with success message
        btnMarkComplete.setEnabled(true);
        btnMarkComplete.setText(R.string.section_completed);

        // Disable the button after completion
        btnMarkComplete.setEnabled(false);

        // Vérifier s'il y a une autre section
        if (currentSectionIndex < sections.size() - 1) {
            showNextSectionDialog();
        } else {
            showCourseCompletedDialog();
        }
    }

    private void showNextSectionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Section terminée")
                .setMessage("Félicitations ! Voulez-vous passer à la section suivante ?")
                .setPositiveButton("Oui", (dialog, which) -> navigateToNextSection())
                .setNegativeButton("Non", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showCourseCompletedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cours terminé")
                .setMessage("Félicitations ! Vous avez terminé toutes les sections de ce cours.")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }

    @Override
    public void onSectionClick(int position) {
        loadSection(position);
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        if (playbackState == Player.STATE_ENDED) {
            videoCompleted = true;
            updateCompleteButtonState();
            showVideoCompletedDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reprendre la lecture vidéo si nécessaire
        if (playerView.getVisibility() == View.VISIBLE && !player.isPlaying()) {
            player.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Mettre en pause la lecture vidéo
        if (player.isPlaying()) {
            player.pause();
        }

        // Mettre en pause YouTube si actif
        if (youTubePlayer != null) {
            youTubePlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Libérer les ressources du lecteur
        if (player != null) {
            player.release();
            player = null;
        }

        // Libérer le lecteur YouTube
        if (youtubePlayerView != null) {
            youtubePlayerView.release();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Gestion du bouton retour dans la barre d'action
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when a new badge is earned
     */
    @Override
    public void onBadgeAwarded(Achievement achievement) {
        runOnUiThread(() -> {
            showBadgeEarnedDialog(achievement);
        });
    }

    /**
     * Shows a dialog when a badge is earned
     */
    private void showBadgeEarnedDialog(Achievement achievement) {
        View badgeView = getLayoutInflater().inflate(R.layout.dialog_badge_earned, null);
        
        TextView tvBadgeTitle = badgeView.findViewById(R.id.badge_title);
        TextView tvBadgeDescription = badgeView.findViewById(R.id.badge_description);
        ImageView ivBadgeIcon = badgeView.findViewById(R.id.badge_icon);
        
        tvBadgeTitle.setText(achievement.getTitle());
        tvBadgeDescription.setText(achievement.getDescription());
        
        // Load badge icon if available
        if (achievement.getIconUrl() != null && !achievement.getIconUrl().isEmpty()) {
            // Using Glide to load the image
            try {
                Glide.with(this)
                    .load(achievement.getIconUrl())
                    .placeholder(R.drawable.placeholder_badge)
                    .error(R.drawable.placeholder_badge)
                    .into(ivBadgeIcon);
            } catch (Exception e) {
                Log.e(TAG, "Error loading badge icon", e);
                ivBadgeIcon.setImageResource(R.drawable.placeholder_badge);
            }
        } else {
            ivBadgeIcon.setImageResource(R.drawable.placeholder_badge);
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle("Badge obtenu !")
            .setView(badgeView)
            .setPositiveButton("Super !", null);
            
        builder.create().show();
    }

    /**
     * Observer le statut des téléchargements
     */
    private void observeDownloadStatus() {
        downloadManager.getDownloadStatus().observe(this, downloads -> {
            if (sections == null || sections.isEmpty() || currentSectionIndex >= sections.size()) {
                return;
            }
            
            // Vérifier le statut de la section actuelle
            CourseSection currentSection = sections.get(currentSectionIndex);
            String fileKey = courseId + "_" + currentSection.getSectionId();
            
            if (downloads.containsKey(fileKey)) {
                VideoDownloadManager.VideoDownloadInfo info = downloads.get(fileKey);
                
                if (info != null) {
                    switch (info.getStatus()) {
                        case VideoDownloadManager.VideoDownloadInfo.STATUS_COMPLETED:
                            updateDownloadButton(true);
                            break;
                        case VideoDownloadManager.VideoDownloadInfo.STATUS_DOWNLOADING:
                        case VideoDownloadManager.VideoDownloadInfo.STATUS_PENDING:
                            btnDownloadVideo.setText("Téléchargement...");
                            btnDownloadVideo.setEnabled(false);
                            break;
                        case VideoDownloadManager.VideoDownloadInfo.STATUS_FAILED:
                            btnDownloadVideo.setText("Télécharger");
                            btnDownloadVideo.setEnabled(true);
                            Toast.makeText(this, "Échec du téléchargement", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }
        });
    }
    
    /**
     * Mettre à jour l'apparence du bouton de téléchargement
     */
    private void updateDownloadButton(boolean isDownloaded) {
        updateDownloadButton(isDownloaded, false);
    }
    
    /**
     * Mettre à jour l'apparence du bouton de téléchargement
     * @param isDownloaded si la vidéo est téléchargée
     * @param isYouTube si c'est une vidéo YouTube
     */
    private void updateDownloadButton(boolean isDownloaded, boolean isYouTube) {
        if (isYouTube) {
            // YouTube peut maintenant être téléchargé via ssyoutube.com
            btnDownloadVideo.setVisibility(View.VISIBLE);
            btnDownloadVideo.setText("Télécharger vidéo YouTube");
            btnDownloadVideo.setEnabled(true);
            btnDownloadVideo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_download, 0, 0, 0);
        } else if (isDownloaded) {
            // Vidéo déjà téléchargée
            btnDownloadVideo.setVisibility(View.VISIBLE);
            btnDownloadVideo.setText("Supprimer la vidéo hors ligne");
            btnDownloadVideo.setEnabled(true);
            btnDownloadVideo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
        } else {
            // Vidéo non téléchargée
            btnDownloadVideo.setVisibility(View.VISIBLE);
            btnDownloadVideo.setText("Télécharger pour regarder hors ligne");
            btnDownloadVideo.setEnabled(true);
            btnDownloadVideo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_download, 0, 0, 0);
        }
    }
    
    /**
     * Gérer le téléchargement ou la suppression de la vidéo
     */
    private void handleVideoDownload() {
        if (sections == null || sections.isEmpty() || currentSectionIndex >= sections.size()) {
            return;
        }
        
        CourseSection section = sections.get(currentSectionIndex);
        String videoUrl = section.getVideoUrl();
        
        if (videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(this, "Aucune vidéo disponible à télécharger", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Vérifier les permissions de stockage
        if (!checkStoragePermission()) {
            requestStoragePermission();
            return;
        }
        
        // Vérifier si la vidéo est déjà téléchargée
        if (downloadManager.isVideoDownloaded(courseId, section.getSectionId())) {
            // Demander confirmation avant de supprimer
            new AlertDialog.Builder(this)
                    .setTitle("Supprimer la vidéo")
                    .setMessage("Voulez-vous vraiment supprimer cette vidéo téléchargée ?")
                    .setPositiveButton("Oui", (dialog, which) -> {
                        if (downloadManager.deleteVideo(courseId, section.getSectionId())) {
                            Toast.makeText(this, "Vidéo supprimée", Toast.LENGTH_SHORT).show();
                            updateDownloadButton(false);
                            
                            // Recharger la vidéo depuis l'URL
                            if (!isYouTubeUrl(videoUrl)) {
                                loadStandardVideo(videoUrl);
                            }
                        } else {
                            Toast.makeText(this, "Échec de la suppression", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Non", null)
                    .show();
        } else {
            // Pour les vidéos YouTube, utiliser notre nouveau dialogue de téléchargement
            if (isYouTubeUrl(videoUrl)) {
                openYouTubeDownloadDialog(videoUrl, section);
                return;
            }
            
            // Pour les vidéos standard, utiliser le gestionnaire de téléchargement existant
            long downloadId = downloadManager.downloadVideo(
                    videoUrl, 
                    courseId, 
                    section.getSectionId(), 
                    section.getTitle()
            );
            
            if (downloadId != -1) {
                Toast.makeText(this, "Téléchargement démarré", Toast.LENGTH_SHORT).show();
                btnDownloadVideo.setText("Téléchargement...");
                btnDownloadVideo.setEnabled(false);
            } else {
                Toast.makeText(this, "Impossible de démarrer le téléchargement", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Ouvre le service de téléchargement en arrière-plan pour YouTube
     */
    private void openYouTubeDownloadDialog(String youtubeUrl, CourseSection section) {
        // Use dialog-based approach only
        YouTubeDownloadDialog dialog = YouTubeDownloadDialog.newInstance(
            youtubeUrl,
            courseId,
            section.getSectionId(),
            section.getTitle()
        );
        dialog.show(getSupportFragmentManager(), "youtube_download_dialog");
    }
    
    /**
     * Vérifier si la permission de stockage est accordée
     */
    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Demander la permission de stockage
     */
    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_CODE
        );
    }
    
    /**
     * Gérer la réponse à la demande de permission
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée, réessayer le téléchargement
                handleVideoDownload();
            } else {
                Toast.makeText(this, "Permission de stockage nécessaire pour télécharger des vidéos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Appelé lorsqu'un fichier vidéo a été téléchargé via le dialogue YouTube
     */
    @Override
    public void onDownloadComplete(File downloadedFile) {
        if (sections == null || sections.isEmpty() || currentSectionIndex >= sections.size()) {
            return;
        }
        
        CourseSection section = sections.get(currentSectionIndex);
        
        // Copier le fichier téléchargé vers notre emplacement de stockage interne
        boolean success = downloadManager.saveDownloadedVideo(
                downloadedFile,
                courseId,
                section.getSectionId(),
                section.getTitle()
        );
        
        if (success) {
            Toast.makeText(this, "Vidéo sauvegardée pour visionnage hors ligne", Toast.LENGTH_SHORT).show();
            
            // Mettre à jour le bouton de téléchargement
            updateDownloadButton(true);
            
            // Charger la vidéo locale
            String localPath = downloadManager.getLocalVideoPath(courseId, section.getSectionId());
            if (localPath != null) {
                loadLocalVideo(localPath);
            }
        } else {
            Toast.makeText(this, "Erreur lors de la sauvegarde de la vidéo", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDownloadsFragment(boolean isDownloaded, boolean isYouTube) {
        DownloadsFragment downloadsFragment = DownloadsFragment.getInstance();
        if (downloadsFragment != null) {
            downloadsFragment.updateDownloadStatus(isDownloaded, isYouTube);
        }
    }

    @Override
    public void onDownloadRequest() {
        handleVideoDownload();
    }
}