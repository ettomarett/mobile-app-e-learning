package com.projet.skilllearn.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.projet.skilllearn.model.LLMMessage;
import com.projet.skilllearn.utils.FirebaseCourseParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Repository for handling communication with Azure OpenAI API using OkHttp
 */
public class LLMRepository {
    private static final String TAG = "LLMRepository";
    
    // Azure OpenAI API Configuration - Exact working values
    private static final String AZURE_RESOURCE_NAME = "DeepSeek-R1-gADK";
    private static final String AZURE_ENDPOINT = "https://" + AZURE_RESOURCE_NAME + ".eastus.models.ai.azure.com";
    private static final String AZURE_API_KEY = "sczzACCarm4XtyfSQz5GQ3v5Hc2hSB2i";
    private static final String MODEL_NAME = "DeepSeek-R1-gADK";
    private static final String API_VERSION = "2024-06-01-preview";
    
    private final MutableLiveData<List<LLMMessage>> chatHistory;
    private final List<LLMMessage> messages;
    private final OkHttpClient httpClient;
    
    // Flag to control whether to show thinking process
    private boolean showThinking = false;
    
    // Callback for course creation
    private CourseCreationCallback courseCreationCallback = null;

    /**
     * Interface for course creation callbacks
     */
    public interface CourseCreationCallback {
        void onCourseCreationStarted();
        void onCourseCreated(String courseId);
        void onCourseCreationFailed(String error);
    }
    
    /**
     * Set the course creation callback
     * @param callback the callback
     */
    public void setCourseCreationCallback(CourseCreationCallback callback) {
        this.courseCreationCallback = callback;
    }

    /**
     * Constructor
     */
    public LLMRepository() {
        chatHistory = new MutableLiveData<>();
        messages = new ArrayList<>();
        
        // Set up HTTP client with logging
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> Log.d(TAG, message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        httpClient = new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
            
        // Add initial welcome message
        LLMMessage welcomeMessage = new LLMMessage(
            "assistant", 
            "Bonjour! Je suis votre assistant d'apprentissage IA. Comment puis-je vous aider aujourd'hui?", 
            false
        );
        messages.add(welcomeMessage);
        chatHistory.postValue(new ArrayList<>(messages));
    }

    /**
     * Send a message to the LLM
     * @param message the message to send
     */
    public void sendMessage(String message) {
        // Add user message to UI
        LLMMessage userMessage = new LLMMessage("user", message, true);
        messages.add(userMessage);
        chatHistory.postValue(new ArrayList<>(messages));
        
        // Make the API request with the working configuration
        makeApiRequest();
    }
    
    /**
     * Add a system message to the chat history
     * @param content the message content
     */
    public void addSystemMessage(String content) {
        LLMMessage systemMessage = new LLMMessage("assistant", content, false);
        messages.add(systemMessage);
        chatHistory.postValue(new ArrayList<>(messages));
    }
    
    /**
     * Process the LLM response for course creation
     * @param content the LLM response content
     */
    private void processForCourseCreation(String content) {
        // Check if the response contains a course creation request
        if (content.contains("<FirebaseCourse>") && content.contains("</FirebaseCourse>")) {
            Log.d(TAG, "Course creation detected in LLM response");
            
            // Notify callback about course creation starting
            if (courseCreationCallback != null) {
                courseCreationCallback.onCourseCreationStarted();
            }
            
            // Add a system message indicating course creation
            addSystemMessage("Création de cours en cours...");
            
            // Parse and upload the course
            FirebaseCourseParser.parseAndUploadCourse(content, new FirebaseCourseParser.FirebaseCallback() {
                @Override
                public void onSuccess(String courseId) {
                    Log.d(TAG, "Course created successfully: " + courseId);
                    
                    // Add a success message
                    addSystemMessage("✅ Cours créé avec succès ! \n\nID: " + courseId);
                    
                    // Notify callback
                    if (courseCreationCallback != null) {
                        courseCreationCallback.onCourseCreated(courseId);
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Course creation failed: " + errorMessage);
                    
                    // Add an error message
                    addSystemMessage("❌ Erreur lors de la création du cours: " + errorMessage);
                    
                    // Notify callback
                    if (courseCreationCallback != null) {
                        courseCreationCallback.onCourseCreationFailed(errorMessage);
                    }
                }
            });
            
            return;
        }
    }
    
    /**
     * Make an API request with the known working configuration
     */
    private void makeApiRequest() {
        // Using the exact working configuration
        Log.d(TAG, "Making API call with proven configuration");
        Log.d(TAG, "URL: " + AZURE_ENDPOINT + "/v1/chat/completions?api-version=" + API_VERSION);
        
        // Create API request body
        JsonObject requestBody = new JsonObject();
        JsonArray messagesArray = new JsonArray();
        
        // Add system message
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", 
            "You are a helpful learning assistant for an e-learning app called SkillLearn. " +
            "Provide concise, informative responses. When appropriate, include references to learning materials. " +
            "Be friendly and encouraging.\n\n" +
            "To create course content for the SkillLearn app, use the following syntax:\n\n" +
            "<FirebaseCourse>\n" +
            "  <CourseDetails>\n" +
            "    title: [Course Title]\n" +
            "    description: [Detailed course description]\n" +
            "    category: [Course Category]\n" +
            "    level: [Débutant|Intermédiaire|Expert]\n" +
            "    durationMinutes: [Total duration in minutes]\n" +
            "    tags: [Comma separated tags]\n" +
            "    imageUrl: [URL to course image]\n" +
            "  </CourseDetails>\n" +
            "  \n" +
            "  <Section>\n" +
            "    title: [Section title]\n" +
            "    description: [Brief section description]\n" +
            "    durationMinutes: [Section duration in minutes]\n" +
            "    orderIndex: [Order in course, starting from 0]\n" +
            "    content: [HTML content with <p> tags]\n" +
            "    videoUrl: [IMPORTANT: You must provide an actual, relevant YouTube video URL. Do not use placeholders. Search for and include real, educational YouTube videos that match the section's content. The URL must start with 'https://www.youtube.com/' or 'https://youtu.be/']\n" +
            "  </Section>\n" +
            "  \n" +
            "  [Additional sections...]\n" +
            "  \n" +
            "  [MANDATORY: Each section MUST have a corresponding quiz:]\n" +
            "  <Quiz>\n" +
            "    title: [Quiz title - should match related section title]\n" +
            "    passingScore: [Score needed to pass, e.g., 70]\n" +
            "    \n" +
            "    <Question>\n" +
            "      question: [Question text]\n" +
            "      options: [Option A|Option B|Option C|Option D]\n" +
            "      correctOptionIndex: [Index of correct option (0-3)]\n" +
            "      explanation: [Explanation for the answer]\n" +
            "    </Question>\n" +
            "    \n" +
            "    [IMPORTANT: Each quiz MUST have at least 3 questions]\n" +
            "  </Quiz>\n" +
            "</FirebaseCourse>\n\n" +
            "When you receive a request to create course content, generate the full course structure following this syntax. " +
            "IMPORTANT RULES:\n" +
            "1. Each section must have a corresponding quiz with at least 3 questions\n" +
            "2. All videoUrl fields MUST contain actual, relevant YouTube video URLs - NO PLACEHOLDERS ALLOWED\n" +
            "3. Before including a video URL, verify that it exists and is relevant to the section content\n" +
            "4. If you cannot find a relevant video for a section, you must search harder or modify the section to match available educational content"
        );
        messagesArray.add(systemMessage);
        
        // Add conversation history - but limit to just the latest few messages
        // to keep request simpler while testing
        List<LLMMessage> recentMessages = new ArrayList<>();
        if (messages.size() > 5) {
            // Get last 5 messages
            recentMessages = messages.subList(messages.size() - 5, messages.size());
        } else {
            recentMessages = messages;
        }
        
        for (LLMMessage msg : recentMessages) {
            if (msg.getRole().equals("user") || msg.getRole().equals("assistant")) {
                JsonObject msgObj = new JsonObject();
                msgObj.addProperty("role", msg.getRole());
                msgObj.addProperty("content", msg.getContent());
                messagesArray.add(msgObj);
            }
        }
        
        // Set request parameters
        requestBody.add("messages", messagesArray);
        requestBody.addProperty("max_tokens", 2500);
        requestBody.addProperty("temperature", 0.7);
        
        // Build URL with the exact format that works
        String url = AZURE_ENDPOINT + "/v1/chat/completions?api-version=" + API_VERSION;
        
        // Create request body
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), 
            new Gson().toJson(requestBody)
        );
        
        // Use the exact header format that works
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer " + AZURE_API_KEY)
            .build();
        
        // Execute request
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API request failed: " + e.getMessage());
                handleApiError("Request failed: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "API response: " + responseBody);
                    
                    try {
                        // Parse response
                        JsonObject jsonResponse = new Gson().fromJson(responseBody, JsonObject.class);
                        String content = jsonResponse
                            .getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();
                            
                        // Process response
                        String processedContent = processResponse(content);
                        
                        // Update UI on main thread
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> {
                            // Add to chat history
                            LLMMessage assistantMessage = new LLMMessage("assistant", processedContent, false);
                            messages.add(assistantMessage);
                            chatHistory.postValue(new ArrayList<>(messages));
                            
                            // Process for course creation
                            processForCourseCreation(content);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing API response: " + e.getMessage());
                        handleApiError("Error parsing response: " + e.getMessage());
                    }
                } else {
                    int statusCode = response.code();
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "API error: " + statusCode + ", " + errorBody);
                    handleApiError("API Error: " + statusCode + ", " + errorBody);
                }
            }
        });
    }
    
    /**
     * Process the response to handle <think> sections
     * @param content the raw response content
     * @return the processed content
     */
    private String processResponse(String content) {
        if (content == null) {
            return "Je suis désolé, mais je n'ai pas pu générer une réponse. Veuillez réessayer.";
        }
        
        if (!content.contains("<think>")) {
            return content;
        }
        
        if (showThinking) {
            // If showing thinking is enabled, return the full content
            return content;
        } else {
            // If showing thinking is disabled, remove the <think> sections
            String[] parts = content.split("<think>");
            
            StringBuilder processedContent = new StringBuilder();
            
            // Add the content before the first <think> tag, if any
            if (parts.length > 0 && !parts[0].isEmpty()) {
                processedContent.append(parts[0].trim());
            }
            
            // Process each <think> section
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                if (part.contains("</think>")) {
                    // Split at </think> and keep the content after it
                    String[] thinkParts = part.split("</think>", 2);
                    if (thinkParts.length > 1 && !thinkParts[1].isEmpty()) {
                        if (processedContent.length() > 0) {
                            processedContent.append("\n\n");
                        }
                        processedContent.append(thinkParts[1].trim());
                    }
                }
            }
            
            return processedContent.toString();
        }
    }
    
    /**
     * Handle API errors
     */
    private void handleApiError(String errorMessage) {
        Log.e(TAG, errorMessage);
        
        // Add error message to chat history
        LLMMessage errorMsg = new LLMMessage(
            "assistant",
            "Désolé, je n'ai pas pu traiter votre demande. Erreur: " + errorMessage,
            false
        );
        
        messages.add(errorMsg);
        chatHistory.postValue(new ArrayList<>(messages));
    }

    /**
     * Get the chat history
     * @return LiveData of the chat history
     */
    public LiveData<List<LLMMessage>> getChatHistory() {
        return chatHistory;
    }
    
    /**
     * Set whether to show thinking process
     * @param show true to show thinking, false to hide
     */
    public void setShowThinking(boolean show) {
        this.showThinking = show;
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        // Nothing to clean up
    }
} 