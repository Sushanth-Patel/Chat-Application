package com.example.mremchat.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mremchat.ChatActivity;
import com.example.mremchat.R;
import com.example.mremchat.model.ChatroomModel;
import com.example.mremchat.model.UserModel;
import com.example.mremchat.utils.AndroidUtil;
import com.example.mremchat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.storage.StorageReference;

public class RecentChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatroomModel, RecentChatRecyclerAdapter.ChatroomModelViewHolder> {

    Context context;

    public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatroomModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatroomModelViewHolder holder, int position, @NonNull ChatroomModel model) {
        if (model.getUserIds() == null || model.getUserIds().isEmpty()) {
            holder.usernameText.setText("Unknown User");
            holder.lastMessageText.setText("");
            holder.lastMessageTime.setText("");
            return;
        }

        FirebaseUtil.getOtherUserFromChatroom(model.getUserIds())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        UserModel otherUserModel = task.getResult().toObject(UserModel.class);

                        if (otherUserModel != null && otherUserModel.getUserId() != null) {
                            // Safely get profile pic reference
                            StorageReference profileRef = FirebaseUtil.getOtherProfilePicStorageRef(otherUserModel.getUserId());
                            if (profileRef != null) {
                                profileRef.getDownloadUrl().addOnCompleteListener(t -> {
                                    if (t.isSuccessful()) {
                                        Uri uri = t.getResult();
                                        AndroidUtil.setProfilePic(context, uri, holder.profilePic);
                                    } else {
                                        Log.w("RecentChatAdapter", "Profile image not found for user: " + otherUserModel.getUserId());
                                    }
                                });
                            }

                            holder.usernameText.setText(otherUserModel.getUsername());

                            String lastMsg = model.getLastMessage();
                            if (lastMsg == null) lastMsg = "";

                            boolean lastMessageSentByMe = model.getLastMessageSenderId() != null &&
                                    model.getLastMessageSenderId().equals(FirebaseUtil.currentUserId());

                            if (lastMessageSentByMe) {
                                holder.lastMessageText.setText("You: " + lastMsg);
                            } else {
                                holder.lastMessageText.setText(lastMsg);
                            }

                            if (model.getLastMessageTimestamp() != null) {
                                holder.lastMessageTime.setText(FirebaseUtil.timestampToString(model.getLastMessageTimestamp()));
                            } else {
                                holder.lastMessageTime.setText("");
                            }

                            holder.itemView.setOnClickListener(v -> {
                                Intent intent = new Intent(context, ChatActivity.class);
                                AndroidUtil.passUserModelAsIntent(intent, otherUserModel);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                            });
                        } else {
                            Log.e("RecentChatAdapter", "UserModel is null or missing userId");
                        }
                    } else {
                        Log.e("RecentChatAdapter", "Failed to get user document", task.getException());
                    }
                });
    }

    @NonNull
    @Override
    public ChatroomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_chat_recycler_row, parent, false);
        return new ChatroomModelViewHolder(view);
    }

    static class ChatroomModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        TextView lastMessageText;
        TextView lastMessageTime;
        ImageView profilePic;

        public ChatroomModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            lastMessageText = itemView.findViewById(R.id.last_message_text);
            lastMessageTime = itemView.findViewById(R.id.last_message_time_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
        }
    }
}
