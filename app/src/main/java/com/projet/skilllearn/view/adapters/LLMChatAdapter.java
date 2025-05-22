package com.projet.skilllearn.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.projet.skilllearn.R;
import com.projet.skilllearn.model.LLMMessage;

public class LLMChatAdapter extends ListAdapter<LLMMessage, LLMChatAdapter.MessageViewHolder> {

    public LLMChatAdapter() {
        super(new DiffUtil.ItemCallback<LLMMessage>() {
            @Override
            public boolean areItemsTheSame(@NonNull LLMMessage oldItem, @NonNull LLMMessage newItem) {
                return oldItem.getTimestamp() == newItem.getTimestamp();
            }

            @Override
            public boolean areContentsTheSame(@NonNull LLMMessage oldItem, @NonNull LLMMessage newItem) {
                return oldItem.getContent().equals(newItem.getContent());
            }
        });
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        LLMMessage message = getItem(position);
        holder.bind(message);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final View messageContainer;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            messageContainer = itemView.findViewById(R.id.messageContainer);
        }

        void bind(LLMMessage message) {
            messageText.setText(message.getContent());
            
            // Apply different styles for user and assistant messages
            if (message.isUser()) {
                messageContainer.setBackgroundResource(R.drawable.bg_user_message);
                ((ViewGroup.MarginLayoutParams) messageContainer.getLayoutParams())
                    .setMarginStart(100);
                ((ViewGroup.MarginLayoutParams) messageContainer.getLayoutParams())
                    .setMarginEnd(16);
            } else {
                messageContainer.setBackgroundResource(R.drawable.bg_assistant_message);
                ((ViewGroup.MarginLayoutParams) messageContainer.getLayoutParams())
                    .setMarginStart(16);
                ((ViewGroup.MarginLayoutParams) messageContainer.getLayoutParams())
                    .setMarginEnd(100);
            }
        }
    }
} 