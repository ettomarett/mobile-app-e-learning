package com.projet.skilllearn.view.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.projet.skilllearn.R;

public class DownloadsFragment extends Fragment {
    private static DownloadsFragment instance;
    private DownloadActionListener downloadActionListener;
    private TextView tvDownloadInfo;
    private Button btnDownloadAction;
    
    private boolean isYoutubeVideo = false;
    private boolean isDownloaded = false;

    public interface DownloadActionListener {
        void onDownloadRequest();
    }

    public static DownloadsFragment getInstance() {
        if (instance == null) {
            instance = new DownloadsFragment();
        }
        return instance;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            downloadActionListener = (DownloadActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement DownloadActionListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_downloads, container, false);
        
        tvDownloadInfo = view.findViewById(R.id.tv_download_info);
        btnDownloadAction = view.findViewById(R.id.btn_download_action);
        
        btnDownloadAction.setOnClickListener(v -> {
            if (downloadActionListener != null) {
                downloadActionListener.onDownloadRequest();
            }
        });
        
        return view;
    }
    
    public void updateDownloadStatus(boolean isDownloaded, boolean isYoutubeVideo) {
        this.isDownloaded = isDownloaded;
        this.isYoutubeVideo = isYoutubeVideo;
        
        if (tvDownloadInfo != null && btnDownloadAction != null) {
            if (isYoutubeVideo) {
                if (isDownloaded) {
                    tvDownloadInfo.setText(R.string.youtube_video_downloaded);
                    btnDownloadAction.setText(R.string.delete_offline_video);
                    btnDownloadAction.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
                } else {
                    tvDownloadInfo.setText(R.string.youtube_video_info);
                    btnDownloadAction.setText(R.string.download_youtube_video);
                    btnDownloadAction.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_download, 0, 0, 0);
                }
            } else {
                if (isDownloaded) {
                    tvDownloadInfo.setText(R.string.video_downloaded);
                    btnDownloadAction.setText(R.string.delete_offline_video);
                    btnDownloadAction.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
                } else {
                    tvDownloadInfo.setText(R.string.download_video_info);
                    btnDownloadAction.setText(R.string.download_for_offline);
                    btnDownloadAction.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_download, 0, 0, 0);
                }
            }
        }
    }
    
    public void setDownloadInProgress(boolean inProgress) {
        if (btnDownloadAction != null) {
            btnDownloadAction.setEnabled(!inProgress);
            btnDownloadAction.setText(inProgress ? R.string.downloading : 
                isDownloaded ? R.string.delete_offline_video : 
                isYoutubeVideo ? R.string.download_youtube_video : R.string.download_for_offline);
        }
    }
} 