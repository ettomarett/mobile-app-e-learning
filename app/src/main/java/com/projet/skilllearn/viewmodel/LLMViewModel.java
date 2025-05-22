package com.projet.skilllearn.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.projet.skilllearn.model.LLMMessage;
import com.projet.skilllearn.repository.LLMRepository;

import java.util.List;

/**
 * ViewModel for LLM chat functionality
 */
public class LLMViewModel extends ViewModel {
    private final LLMRepository repository;

    /**
     * Constructor
     */
    public LLMViewModel() {
        repository = new LLMRepository();
    }

    /**
     * Get chat history
     * @return LiveData of chat messages
     */
    public LiveData<List<LLMMessage>> getChatHistory() {
        return repository.getChatHistory();
    }

    /**
     * Send a message to the LLM
     * @param message the message to send
     */
    public void sendMessage(String message) {
        repository.sendMessage(message);
    }
    
    /**
     * Set whether to show the LLM's thinking process
     * @param show true to show thinking, false to hide
     */
    public void setShowThinking(boolean show) {
        repository.setShowThinking(show);
    }
    
    /**
     * Called when ViewModel is cleared
     * Clean up resources
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanup();
    }
} 