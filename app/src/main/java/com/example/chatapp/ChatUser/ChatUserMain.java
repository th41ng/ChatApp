package com.example.chatapp.ChatUser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.util.Base64;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;

import com.example.chatapp.R;
import com.example.chatapp.Re_Sign.LoginActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;

public class ChatUserMain extends AppCompatActivity {
    private PreferenceManager preferenceManager;
    TextView name;
    ImageView image;
    AppCompatImageView btnSignOut;
    FloatingActionButton fabNewFriend;
    FloatingActionButton fabFriends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_user_main);
        preferenceManager = new PreferenceManager(getApplicationContext());
        loadUserDetails();
        getToken();
        setListeners();
    }

    private void loadUserDetails() {
        name = findViewById(R.id.textName);
        name.setText(preferenceManager.getString("name"));
        //byte[] bytes = Base64.decode(preferenceManager.getString("image"), Base64.DEFAULT);
        //Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        //image.setImageBitmap(bitmap);
    }

    private void setListeners() {
        btnSignOut = findViewById(R.id.imageSignOut);
        btnSignOut.setOnClickListener(view -> signOut());
        fabNewFriend = findViewById(R.id.fabNewFriend);
        fabNewFriend.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), UserActivity.class)));
        fabFriends=findViewById(R.id.fabFriends);
        fabFriends.setOnClickListener(view->startActivity(new Intent(getApplicationContext(),FriendRequest.class)));
    }

    private void showToast(String message) {

        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        String userId = preferenceManager.getString("userId");
        if (userId != null && !userId.isEmpty()) {
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            DocumentReference documentReference = database.collection("users").document(userId);

            documentReference.update("fcmToken", token)
                    .addOnSuccessListener(unused -> showToast("Token updated successfully"))
                    .addOnFailureListener(e -> showToast("Unable to update token"));
        } else {
            showToast("User ID is null or empty!");
        }
    }

    private void signOut() {
        showToast("Signing out...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection("users").document(preferenceManager.getString("userId")
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }
}