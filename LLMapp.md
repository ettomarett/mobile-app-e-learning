# SkillLearn AI Assistant Implementation

## Introduction

SkillLearn integrates an AI assistant powered by Azure OpenAI to provide personalized learning support. This document explains how we implemented the chat interface, navigation, and backend integration with Azure OpenAI.

## 1. Creating the Chat Interface

### Chat Fragment Structure

The AI assistant is implemented as a dedicated fragment (`LLMChatFragment`) that shows a chat interface with message history and an input field:

```java
public class LLMChatFragment extends Fragment {
    private RecyclerView recyclerView;
    private EditText messageInput;
    private Button sendButton;
    private ChatMessageAdapter adapter;
    private LLMViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_llm_chat, container, false);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.rv_chat_messages);
        messageInput = view.findViewById(R.id.et_message);
        sendButton = view.findViewById(R.id.btn_send);
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatMessageAdapter();
        recyclerView.setAdapter(adapter);
        
        // Setup ViewModel
        viewModel = new ViewModelProvider(this).get(LLMViewModel.class);
        
        // Observe chat history
        viewModel.getChatHistory().observe(getViewLifecycleOwner(), messages -> {
            adapter.setMessages(messages);
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
        });
        
        // Setup send button
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                viewModel.sendMessage(message);
                messageInput.setText("");
            }
        });
        
        return view;
    }
}
```

### Chat Layout

The fragment layout (`fragment_llm_chat.xml`) includes a RecyclerView for the chat history and input controls:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_chat_messages"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:padding="8dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toTopOf="@id/message_input_container" />

    <LinearLayout
        android:id="@+id/message_input_container"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="8dp"
        app:layout_constraintBottom_toBottomOf="parent">

        <EditText
            android:id="@+id/et_message"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:hint="Type a message..."
            android:inputType="textMultiLine"
            android:maxLines="4" />

        <Button
            android:id="@+id/btn_send"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Send" />
    </LinearLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
```

### Message Item Layout

Each message is displayed with a distinct style based on whether it's from the user or the AI assistant:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="4dp"
    app:cardCornerRadius="8dp"
    app:cardElevation="2dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="8dp">

        <TextView
            android:id="@+id/tv_message"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textSize="16sp" />

    </LinearLayout>
</androidx.cardview.widget.CardView>
```

## 2. Adding to Navigation Menu

### Bottom Navigation Menu

We added the AI assistant to the bottom navigation menu in `bottom_nav_menu.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/nav_home"
        android:icon="@android:drawable/ic_menu_compass"
        android:title="Accueil" />
    <item
        android:id="@+id/nav_catalog"
        android:icon="@android:drawable/ic_menu_search"
        android:title="Catalogue" />
    <item
        android:id="@+id/nav_assistant"
        android:icon="@drawable/ic_ai_assistant"
        android:title="Assistant IA" />
    <item
        android:id="@+id/nav_profile"
        android:icon="@android:drawable/ic_menu_myplaces"
        android:title="Profil" />
</menu>
```

### Navigation Graph

We registered the chat fragment in the navigation graph (`nav_graph.xml`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/nav_graph"
    app:startDestination="@id/nav_home">

    <!-- Other destinations -->

    <fragment
        android:id="@+id/nav_assistant"
        android:name="com.projet.skilllearn.view.fragments.LLMChatFragment"
        android:label="Assistant IA"
        tools:layout="@layout/fragment_llm_chat" />
</navigation>
```

### MainActivity Setup

In `MainActivity.java`, we configured the navigation controller to handle the bottom navigation menu:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    // Find views
    bottomNavigationView = findViewById(R.id.bottom_navigation);
    
    // Get NavController from the NavHostFragment
    navController = ((NavHostFragment) getSupportFragmentManager()
            .findFragmentById(R.id.nav_host_fragment))
            .getNavController();
    
    // Define top level destinations
    appBarConfiguration = new AppBarConfiguration.Builder(
            R.id.nav_home,
            R.id.nav_catalog,
            R.id.nav_assistant,  // Added AI assistant as a top-level destination
            R.id.nav_profile
    ).build();
    
    // Connect bottom navigation with nav controller
    NavigationUI.setupWithNavController(bottomNavigationView, navController);
}
```

## 3. Azure OpenAI Integration

### LLM Repository

The `LLMRepository` class handles communication with the Azure OpenAI API:

```java
public class LLMRepository {
    // Azure OpenAI API Configuration
    private static final String AZURE_RESOURCE_NAME = "DeepSeek-R1-gADK";
    private static final String AZURE_ENDPOINT = "https://" + AZURE_RESOURCE_NAME + ".eastus.models.ai.azure.com";
    private static final String AZURE_API_KEY = "sczzACCarm4XtyfSQz5GQ3v5Hc2hSB2i";
    private static final String API_VERSION = "2024-06-01-preview";
    
    private final MutableLiveData<List<LLMMessage>> chatHistory;
    private final List<LLMMessage> messages;
    private final OkHttpClient httpClient;
    
    public LLMRepository() {
        chatHistory = new MutableLiveData<>();
        messages = new ArrayList<>();
        
        // Set up HTTP client with logging
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        httpClient = new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
            
        // Add initial welcome message
        messages.add(new LLMMessage("assistant", 
            "Bonjour! Je suis votre assistant d'apprentissage IA. Comment puis-je vous aider aujourd'hui?", 
            false));
        chatHistory.postValue(new ArrayList<>(messages));
    }

    public void sendMessage(String message) {
        // Add user message to chat history
        messages.add(new LLMMessage("user", message, true));
        chatHistory.postValue(new ArrayList<>(messages));
        
        // Send to Azure OpenAI API
        makeApiRequest();
    }
    
    private void makeApiRequest() {
        // Create request body with messages
        JsonObject requestBody = new JsonObject();
        JsonArray messagesArray = new JsonArray();
        
        // Add system message
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are a helpful learning assistant for an e-learning app called SkillLearn.");
        messagesArray.add(systemMessage);
        
        // Add conversation history
        for (LLMMessage msg : messages) {
            if (msg.getRole().equals("user") || msg.getRole().equals("assistant")) {
                JsonObject msgObj = new JsonObject();
                msgObj.addProperty("role", msg.getRole());
                msgObj.addProperty("content", msg.getContent());
                messagesArray.add(msgObj);
            }
        }
        
        // Set request parameters
        requestBody.add("messages", messagesArray);
        requestBody.addProperty("max_tokens", 800);
        requestBody.addProperty("temperature", 0.7);
        
        // Build URL with the exact working format
        String url = AZURE_ENDPOINT + "/v1/chat/completions?api-version=" + API_VERSION;
        
        // Create request
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), 
            new Gson().toJson(requestBody)
        );
        
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
                handleApiError("Request failed: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    // Parse response
                    JsonObject jsonResponse = new Gson().fromJson(response.body().string(), JsonObject.class);
                    String content = jsonResponse
                        .getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
                        
                    // Add to chat history
                    messages.add(new LLMMessage("assistant", content, false));
                    chatHistory.postValue(new ArrayList<>(messages));
                } else {
                    handleApiError("API Error: " + response.code());
                }
            }
        });
    }

    public LiveData<List<LLMMessage>> getChatHistory() {
        return chatHistory;
    }
}
```

### LLM Message Model

The `LLMMessage` class represents a single message in the chat:

```java
public class LLMMessage {
    private String role;        // "user" or "assistant"
    private String content;     // Message content
    private boolean isUser;     // Whether the message is from the user

    public LLMMessage(String role, String content, boolean isUser) {
        this.role = role;
        this.content = content;
        this.isUser = isUser;
    }

    // Getters and setters
    public String getRole() { return role; }
    public String getContent() { return content; }
    public boolean isUser() { return isUser; }
}
```

### ViewModel

The `LLMViewModel` connects the UI with the repository:

```java
public class LLMViewModel extends ViewModel {
    private final LLMRepository repository;

    public LLMViewModel() {
        repository = new LLMRepository();
    }

    public void sendMessage(String message) {
        repository.sendMessage(message);
    }

    public LiveData<List<LLMMessage>> getChatHistory() {
        return repository.getChatHistory();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanup();
    }
}
```

### Chat Message Adapter

The `ChatMessageAdapter` displays messages in the RecyclerView:

```java
public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {
    private List<LLMMessage> messages = new ArrayList<>();

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        LLMMessage message = messages.get(position);
        holder.messageText.setText(message.getContent());
        
        // Style based on message sender
        if (message.isUser()) {
            holder.itemView.setBackgroundResource(R.drawable.bg_user_message);
            holder.messageText.setTextColor(Color.WHITE);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_assistant_message);
            holder.messageText.setTextColor(Color.BLACK);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setMessages(List<LLMMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.tv_message);
        }
    }
}
```

## 4. Azure OpenAI API Configuration

The key to making the Azure OpenAI integration work was identifying the correct API endpoint structure:

```
https://DeepSeek-R1-gADK.eastus.models.ai.azure.com/v1/chat/completions?api-version=2024-06-01-preview
```

The essential configuration details are:

- **Resource Name**: DeepSeek-R1-gADK
- **Model Name**: DeepSeek-R1-gADK
- **API Version**: 2024-06-01-preview
- **Authentication**: Bearer token in Authorization header

The most important aspects of the working configuration:

1. Using the `/v1/chat/completions` endpoint instead of `/openai/deployments/{model}/chat/completions`
2. Using the correct API version `2024-06-01-preview`
3. Including the authorization header as `Bearer token`

## 5. Testing and Debugging

To ensure reliable communication with Azure OpenAI, we:

1. Implemented comprehensive error handling
2. Added HTTP request/response logging
3. Created Python test scripts to verify API connectivity
4. Implemented a robust retry mechanism for different configurations

The final implementation provides users with a responsive, helpful AI assistant integrated directly into the SkillLearn app's navigation system. 