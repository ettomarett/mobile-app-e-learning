package com.projet.skilllearn.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.projet.skilllearn.R;
import com.projet.skilllearn.repository.CourseRepository;
import com.projet.skilllearn.utils.CourseCreatedDialogHelper;
import com.projet.skilllearn.view.CourseDetailActivity;
import com.projet.skilllearn.view.adapters.LLMChatAdapter;
import com.projet.skilllearn.viewmodel.LLMViewModel;

/**
 * Fragment for LLM chat interface
 */
public class LLMChatFragment extends Fragment {
    private static final String TAG = "LLMChatFragment";
    private LLMViewModel viewModel;
    private LLMChatAdapter adapter;
    private EditText messageInput;
    private RecyclerView recyclerView;
    private MaterialButton sendButton;
    private LinearProgressIndicator progressIndicator;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LLMViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_llm_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.chatRecyclerView);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);
        progressIndicator = view.findViewById(R.id.progressIndicator);
        
        if (progressIndicator == null) {
            Log.w(TAG, "Progress indicator not found in layout, course creation progress will not be displayed");
        }
        
        // Set up adapter
        adapter = new LLMChatAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Observe chat history
        viewModel.getChatHistory().observe(getViewLifecycleOwner(), messages -> {
            adapter.submitList(messages);
            if (!messages.isEmpty()) {
                recyclerView.scrollToPosition(messages.size() - 1);
            }
        });
        
        // Observe course creation state
        viewModel.getIsCreatingCourse().observe(getViewLifecycleOwner(), isCreating -> {
            if (progressIndicator != null) {
                progressIndicator.setVisibility(isCreating ? View.VISIBLE : View.GONE);
            }
            sendButton.setEnabled(!isCreating);
            messageInput.setEnabled(!isCreating);
            
            if (isCreating) {
                showToast("Création de cours en cours...");
            }
        });
        
        // Observe course creation success
        viewModel.getCreatedCourseId().observe(getViewLifecycleOwner(), courseId -> {
            if (courseId != null && getContext() != null) {
                // Show the course created dialog
                CourseCreatedDialogHelper.showCourseCreatedDialog(getContext(), courseId);
                
                // Reset the state
                viewModel.resetCourseCreationState();
            }
        });
        
        // Observe course creation error
        viewModel.getCourseCreationError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showToast("Erreur: " + error);
                viewModel.resetCourseCreationState();
            }
        });
        
        // Set up send button
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                viewModel.sendMessage(message);
                messageInput.setText("");
            }
        });
    }
    
    /**
     * Show a toast message
     * @param message the message to show
     */
    private void showToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
} 