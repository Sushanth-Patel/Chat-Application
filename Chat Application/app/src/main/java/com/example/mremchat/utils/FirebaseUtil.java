package com.example.mremchat.utils;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class FirebaseUtil {

    private static final String TAG = "FirebaseUtil";

    public static String currentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getUid();
        }
        return null;
    }

    public static boolean isLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    public static DocumentReference currentUserDetails() {
        String userId = currentUserId();
        if (userId != null) {
            return FirebaseFirestore.getInstance().collection("users").document(userId);
        }
        return null;
    }

    public static CollectionReference allUserCollectionReference() {
        return FirebaseFirestore.getInstance().collection("users");
    }

    public static DocumentReference getChatroomReference(String chatroomId) {
        return FirebaseFirestore.getInstance().collection("chatrooms").document(chatroomId);
    }

    public static CollectionReference getChatroomMessageReference(String chatroomId) {
        return getChatroomReference(chatroomId).collection("chats");
    }

    public static CollectionReference allChatroomCollectionReference() {
        return FirebaseFirestore.getInstance().collection("chatrooms");
    }

    public static String getChatroomId(String userId1, String userId2) {
        if (userId1 == null || userId2 == null) {
            Log.e(TAG, "One or both user IDs are null");
            return null;
        }

        // Ensure consistent ordering for chatroom ID
        if (userId1.compareTo(userId2) < 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    public static DocumentReference getOtherUserFromChatroom(List<String> userIds) {
        if (userIds == null || userIds.size() < 2) {
            Log.e(TAG, "Invalid userIds list");
            return null;
        }

        String currentUserId = currentUserId();
        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null");
            return null;
        }

        if (userIds.get(0).equals(currentUserId)) {
            return allUserCollectionReference().document(userIds.get(1));
        } else {
            return allUserCollectionReference().document(userIds.get(0));
        }
    }

    public static String timestampToString(Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        try {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp.toDate());
        } catch (Exception e) {
            Log.e(TAG, "Error formatting timestamp", e);
            return "";
        }
    }

    public static void logout() {
        FirebaseAuth.getInstance().signOut();
    }

    public static StorageReference getCurrentProfilePicStorageRef() {
        String userId = currentUserId();
        if (userId != null) {
            return FirebaseStorage.getInstance().getReference().child("profile_pic").child(userId);
        }
        return null;
    }

    public static StorageReference getOtherProfilePicStorageRef(String otherUserId) {
        if (otherUserId != null && !otherUserId.isEmpty()) {
            return FirebaseStorage.getInstance().getReference().child("profile_pic").child(otherUserId);
        }
        return null;
    }
}