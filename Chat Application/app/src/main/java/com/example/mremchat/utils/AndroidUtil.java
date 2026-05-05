package com.example.mremchat.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.mremchat.R;
import com.example.mremchat.model.UserModel;

public class AndroidUtil {

    private static final String TAG = "AndroidUtil";

    public static void showToast(Context context, String message) {
        if (context != null && message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static void passUserModelAsIntent(Intent intent, UserModel model) {
        if (intent != null && model != null) {
            intent.putExtra("username", model.getUsername());
            intent.putExtra("phone", model.getPhone());
            intent.putExtra("userId", model.getUserId());
            Log.d(TAG, "Passing user model: " + model.getUsername() + " - " + model.getUserId());
        } else {
            Log.e(TAG, "Intent or UserModel is null in passUserModelAsIntent");
        }
    }

    public static UserModel getUserModelFromIntent(Intent intent) {
        if (intent == null) {
            Log.e(TAG, "Intent is null in getUserModelFromIntent");
            return null;
        }

        try {
            String username = intent.getStringExtra("username");
            String phone = intent.getStringExtra("phone");
            String userId = intent.getStringExtra("userId");

            // Check if essential data is present
            if (username == null || userId == null) {
                Log.e(TAG, "Essential user data is missing - username: " + username + ", userId: " + userId);
                return null;
            }

            UserModel userModel = new UserModel();
            userModel.setUsername(username);
            userModel.setPhone(phone); // Phone can be null
            userModel.setUserId(userId);

            Log.d(TAG, "Retrieved user model: " + username + " - " + userId);
            return userModel;

        } catch (Exception e) {
            Log.e(TAG, "Error retrieving user model from intent", e);
            return null;
        }
    }

    public static void setProfilePic(Context context, Uri imageUri, ImageView imageView) {
        if (context == null || imageView == null) {
            Log.e(TAG, "Context or ImageView is null in setProfilePic");
            return;
        }

        try {
            if (imageUri != null) {
                Glide.with(context)
                        .load(imageUri)
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.person_icon) // Add a placeholder
                        .error(R.drawable.person_icon) // Add an error image
                        .into(imageView);
            } else {
                // Set default profile image if URI is null
                Glide.with(context)
                        .load(R.drawable.person_icon)
                        .apply(RequestOptions.circleCropTransform())
                        .into(imageView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading profile picture", e);
            // Set default image in case of error
            try {
                imageView.setImageResource(R.drawable.person_icon);
            } catch (Exception ex) {
                Log.e(TAG, "Error setting default image", ex);
            }
        }
    }

    /**
     * Helper method to validate if a string is not null and not empty
     */
    public static boolean isValidString(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Helper method to safely get string from intent with default value
     */
    public static String getStringFromIntent(Intent intent, String key, String defaultValue) {
        if (intent != null && key != null) {
            String value = intent.getStringExtra(key);
            return value != null ? value : defaultValue;
        }
        return defaultValue;
    }

    /**
     * Enhanced method to pass user model with validation
     */
    public static boolean passUserModelAsIntentSafe(Intent intent, UserModel model) {
        if (intent == null || model == null) {
            Log.e(TAG, "Intent or UserModel is null");
            return false;
        }

        if (!isValidString(model.getUserId()) || !isValidString(model.getUsername())) {
            Log.e(TAG, "UserModel has invalid essential data");
            return false;
        }

        try {
            intent.putExtra("username", model.getUsername());
            intent.putExtra("phone", model.getPhone());
            intent.putExtra("userId", model.getUserId());
            Log.d(TAG, "Successfully passed user model: " + model.getUsername());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error passing user model as intent", e);
            return false;
        }
    }
}