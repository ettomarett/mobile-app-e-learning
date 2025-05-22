package com.projet.skilllearn.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.projet.skilllearn.model.LLMMessage;
import com.projet.skilllearn.repository.LLMRepository;

import java.util.List;

/**
 * ViewModel for LLM chat functionality
 */
public class LLMViewModel extends ViewModel {
    private final LLMRepository repository;
    private final MutableLiveData<Boolean> isCreatingCourse = new MutableLiveData<>(false);
    private final MutableLiveData<String> createdCourseId = new MutableLiveData<>();
    private final MutableLiveData<String> courseCreationError = new MutableLiveData<>();

    /**
     * Constructor
     */
    public LLMViewModel() {
        repository = new LLMRepository();
        
        // Set up course creation callback
        repository.setCourseCreationCallback(new LLMRepository.CourseCreationCallback() {
            @Override
            public void onCourseCreationStarted() {
                isCreatingCourse.postValue(true);
            }
            
            @Override
            public void onCourseCreated(String courseId) {
                isCreatingCourse.postValue(false);
                createdCourseId.postValue(courseId);
            }
            
            @Override
            public void onCourseCreationFailed(String error) {
                isCreatingCourse.postValue(false);
                courseCreationError.postValue(error);
            }
        });
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
     * Get whether a course is being created
     * @return true if a course is being created, false otherwise
     */
    public LiveData<Boolean> getIsCreatingCourse() {
        return isCreatingCourse;
    }
    
    /**
     * Get the ID of the most recently created course
     * @return the course ID
     */
    public LiveData<String> getCreatedCourseId() {
        return createdCourseId;
    }
    
    /**
     * Get the error message from course creation
     * @return the error message
     */
    public LiveData<String> getCourseCreationError() {
        return courseCreationError;
    }
    
    /**
     * Reset the course creation state
     */
    public void resetCourseCreationState() {
        isCreatingCourse.setValue(false);
        createdCourseId.setValue(null);
        courseCreationError.setValue(null);
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