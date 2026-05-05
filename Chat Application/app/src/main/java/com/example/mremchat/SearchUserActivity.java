package com.example.mremchat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mremchat.adapter.SearchUserRecyclerAdapter;
import com.example.mremchat.model.UserModel;
import com.example.mremchat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

public class SearchUserActivity extends AppCompatActivity {

    private static final String TAG = "SearchUserActivity";

    EditText searchInput;
    ImageButton searchButton;
    ImageButton backButton;
    RecyclerView recyclerView;
    SearchUserRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search_user);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        searchInput = findViewById(R.id.search_username_input);
        searchButton = findViewById(R.id.search_user_btn);
        backButton = findViewById(R.id.back_btn);
        recyclerView = findViewById(R.id.search_user_recycler_view);
        searchInput.requestFocus();

        backButton.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        // Add text change listener for real-time search
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchTerm = s.toString().trim();
                if (searchTerm.length() >= 2) { // Reduced from 3 to 2 for better UX
                    setupSearchRecyclerView(searchTerm);
                } else if (searchTerm.isEmpty()) {
                    // Clear results when search is empty
                    clearResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchButton.setOnClickListener(v -> {
            String searchTerm = searchInput.getText().toString().trim();
            if (searchTerm.isEmpty() || searchTerm.length() < 2) {
                searchInput.setError("Username must be at least 2 characters");
                return;
            }
            setupSearchRecyclerView(searchTerm);
        });

        // Initialize with empty adapter
        setupEmptyRecyclerView();
    }

    void setupEmptyRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Don't set adapter yet
    }

    void clearResults() {
        if (adapter != null) {
            adapter.stopListening();
            adapter = null;
        }
        recyclerView.setAdapter(null);
    }

    void setupSearchRecyclerView(String searchTerm) {
        try {
            // Stop previous adapter if it exists
            if (adapter != null) {
                adapter.stopListening();
            }

            String currentUserId = FirebaseUtil.currentUserId();
            if (currentUserId == null) {
                Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create query - search for usernames that start with the search term
            Query query = FirebaseUtil.allUserCollectionReference()
                    .whereGreaterThanOrEqualTo("username", searchTerm.toLowerCase())
                    .whereLessThanOrEqualTo("username", searchTerm.toLowerCase() + "\uf8ff")
                    .whereNotEqualTo("userId", currentUserId) // Exclude current user
                    .limit(20); // Increased limit for better results

            FirestoreRecyclerOptions<UserModel> options = new FirestoreRecyclerOptions.Builder<UserModel>()
                    .setQuery(query, UserModel.class)
                    .build();

            adapter = new SearchUserRecyclerAdapter(options, getApplicationContext());
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
            adapter.startListening();

            Log.d(TAG, "Search query setup for term: " + searchTerm);

        } catch (Exception e) {
            Log.e(TAG, "Error searching users: " + e.getMessage(), e);
            Toast.makeText(this, "Error searching users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null)
            adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null)
            adapter.stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null)
            adapter.startListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null)
            adapter.stopListening();
    }
}