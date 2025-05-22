package com.projet.skilllearn.view.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.projet.skilllearn.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    private final Context context;
    private final int[] avatars;
    private final OnAvatarClickListener listener;
    private int selectedPosition = 0;

    public interface OnAvatarClickListener {
        void onAvatarClick(int position);
    }

    public AvatarAdapter(Context context, int[] avatars, int selectedPosition, OnAvatarClickListener listener) {
        this.context = context;
        this.avatars = avatars;
        this.selectedPosition = selectedPosition;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_avatar, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, final int position) {
        holder.ivAvatar.setImageResource(avatars[position]);

        // Mettre en évidence l'avatar sélectionné
        if (position == selectedPosition) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary));
            holder.ivAvatar.setBorderColor(ContextCompat.getColor(context, R.color.white));
            holder.ivAvatar.setBorderWidth(4);
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
            holder.ivAvatar.setBorderColor(ContextCompat.getColor(context, android.R.color.transparent));
            holder.ivAvatar.setBorderWidth(0);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                selectedPosition = adapterPosition;
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);
                listener.onAvatarClick(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return avatars.length;
    }

    static class AvatarViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final CircleImageView ivAvatar;

        AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
        }
    }
}