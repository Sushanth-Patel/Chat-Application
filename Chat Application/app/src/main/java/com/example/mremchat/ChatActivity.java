package com.example.mremchat;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mremchat.adapter.ChatRecyclerAdapter;
import com.example.mremchat.model.ChatMessageModel;
import com.example.mremchat.model.ChatroomModel;
import com.example.mremchat.model.UserModel;
import com.example.mremchat.utils.AndroidUtil;
import com.example.mremchat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    UserModel otherUser;
    String chatroomId;
    ChatroomModel chatroomModel;
    ChatRecyclerAdapter adapter;

    EditText messageInput;
    ImageButton sendMessageBtn;
    ImageButton backBtn;
    TextView otherUsername;
    RecyclerView recyclerView;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get other user from intent
        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        if (otherUser == null) {
            Log.e(TAG, "Other user is null");
            Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Generate chatroom ID
        String currentUserId = FirebaseUtil.currentUserId();
        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null");
            Toast.makeText(this, "Error: Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chatroomId = FirebaseUtil.getChatroomId(currentUserId, otherUser.getUserId());
        Log.d(TAG, "Chatroom ID: " + chatroomId);

        // Initialize views
        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        imageView = findViewById(R.id.profile_pic_image_view);

        // Load profile picture
        FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Uri uri = t.getResult();
                        AndroidUtil.setProfilePic(this, uri, imageView);
                    }
                });

        // UI listeners
        backBtn.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        otherUsername.setText(otherUser.getUsername());

        sendMessageBtn.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            sendMessageToUser(message);
        });

        // Get or create chatroom and setup messages
        getOrCreateChatroomModel();
    }

    void setupChatRecyclerView() {
        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy("timestamp", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options, ChatActivity.this);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);

        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        });

        Log.d(TAG, "RecyclerView set up for chatroom: " + chatroomId);
    }

    void sendMessageToUser(String message) {
        if (chatroomModel == null) {
            Log.e(TAG, "Chatroom model is null, cannot send message");
            Toast.makeText(this, "Error: Chatroom not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        sendMessageBtn.setEnabled(false);
        ChatMessageModel chatMessageModel = new ChatMessageModel(message, FirebaseUtil.currentUserId(), Timestamp.now());

        FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Message sent: " + message);

                        // Ensure userIds are not lost
                        if (chatroomModel.getUserIds() == null || chatroomModel.getUserIds().size() != 2) {
                            chatroomModel.setUserIds(Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()));
                        }

                        chatroomModel.setLastMessageTimestamp(Timestamp.now());
                        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
                        chatroomModel.setLastMessage(message);

                        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel)
                                .addOnCompleteListener(chatroomTask -> {
                                    if (chatroomTask.isSuccessful()) {
                                        Log.d(TAG, "Chatroom updated");
                                        messageInput.setText("");
                                    } else {
                                        Log.e(TAG, "Chatroom update failed", chatroomTask.getException());
                                    }
                                    sendMessageBtn.setEnabled(true);
                                });
                    } else {
                        Log.e(TAG, "Message failed to send", task.getException());
                        Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                        sendMessageBtn.setEnabled(true);
                    }
                });
    }

    void getOrCreateChatroomModel() {
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if (chatroomModel == null) {
                    chatroomModel = new ChatroomModel(
                            chatroomId,
                            Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()),
                            Timestamp.now(),
                            ""
                    );
                    chatroomModel.setLastMessageSenderId("");

                    FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel)
                            .addOnCompleteListener(createTask -> {
                                if (createTask.isSuccessful()) {
                                    Log.d(TAG, "New chatroom created");
                                } else {
                                    Log.e(TAG, "Failed to create chatroom", createTask.getException());
                                    Toast.makeText(this, "Error creating chatroom", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Log.d(TAG, "Existing chatroom found");
                }
                // 🚨 Always call setupChatRecyclerView, no matter what
                setupChatRecyclerView();
            } else {
                Log.e(TAG, "Chatroom load failed", task.getException());
                Toast.makeText(this, "Error loading chatroom", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) adapter.stopListening();
    }
}
