package com.projet.skilllearn.view.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.projet.skilllearn.R;
import com.projet.skilllearn.utils.VideoDownloadManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DownloadedVideoAdapter extends RecyclerView.Adapter<DownloadedVideoAdapter.ViewHolder> {
    
    private final Context context;
    private List<VideoDownloadManager.VideoDownloadInfo> videos;
    private final OnVideoActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    
    public interface OnVideoActionListener {
        void onPlayVideo(VideoDownloadManager.VideoDownloadInfo video);
        void onDeleteVideo(VideoDownloadManager.VideoDownloadInfo video);
    }
    
    public DownloadedVideoAdapter(Context context, List<VideoDownloadManager.VideoDownloadInfo> videos, OnVideoActionListener listener) {
        this.context = context;
        this.videos = videos;
        this.listener = listener;
    }
    
    public void updateData(List<VideoDownloadManager.VideoDownloadInfo> newVideos) {
        this.videos = newVideos;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_downloaded_video, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoDownloadManager.VideoDownloadInfo video = videos.get(position);
        
        holder.tvTitle.setText(video.getTitle());
        
        // Set course and section info
        String courseInfo = "Cours: " + video.getCourseId();
        holder.tvCourseInfo.setText(courseInfo);
        
        // Get file info
        if (video.getLocalPath() != null) {
            File videoFile = new File(video.getLocalPath());
            if (videoFile.exists()) {
                // Format file size
                long fileSizeKB = videoFile.length() / 1024;
                String fileSize;
                if (fileSizeKB > 1024) {
                    fileSize = String.format(Locale.getDefault(), "%.1f MB", fileSizeKB / 1024f);
                } else {
                    fileSize = String.format(Locale.getDefault(), "%d KB", fileSizeKB);
                }
                
                // Format date
                Date lastModified = new Date(videoFile.lastModified());
                String dateStr = dateFormat.format(lastModified);
                
                String fileInfo = fileSize + " • " + dateStr;
                holder.tvFileInfo.setText(fileInfo);
                
                // Set video thumbnail (use a placeholder for now)
                holder.ivThumbnail.setImageResource(R.drawable.placeholder_video);
            }
        }
        
        // Set click listeners
        holder.itemView.setOnClickListener(v -> listener.onPlayVideo(video));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteVideo(video));
        holder.btnPlay.setOnClickListener(v -> listener.onPlayVideo(video));
    }
    
    @Override
    public int getItemCount() {
        return videos.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvTitle;
        TextView tvCourseInfo;
        TextView tvFileInfo;
        ImageButton btnPlay;
        ImageButton btnDelete;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvCourseInfo = itemView.findViewById(R.id.tv_course_info);
            tvFileInfo = itemView.findViewById(R.id.tv_file_info);
            btnPlay = itemView.findViewById(R.id.btn_play);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
} 