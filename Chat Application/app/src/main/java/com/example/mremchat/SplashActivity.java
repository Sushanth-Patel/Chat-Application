package com.example.mremchat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mremchat.model.UserModel;
import com.example.mremchat.utils.AndroidUtil;
import com.example.mremchat.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (FirebaseUtil.isLoggedIn() && getIntent().getExtras()!=null){
//from notification
          String userId = getIntent().getExtras().getString("userId");
          FirebaseUtil.allUserCollectionReference().document(userId).get()
                  .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        UserModel model = task.getResult().toObject(UserModel.class);

                        Intent mainIntent = new Intent(this, MainActivity.class);
                        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(mainIntent);

                        Intent intent = new Intent(this, ChatActivity.class);
                        AndroidUtil.passUserModelAsIntent(intent,model);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                       startActivity(intent);
                       finish();
                    }
                  });
        }
else {
        new Handler(getMainLooper()).postDelayed(() -> {
            if(FirebaseUtil.isLoggedIn()){
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            }else {
                startActivity(new Intent(SplashActivity.this, LoginPhoneNumberActivity.class));

            }
            finish();
        }, 1000);

}
    }
}