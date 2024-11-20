package com.example.chatapp.ChatUser;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.Chat.Chats;
import com.example.chatapp.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import models.FriendRequest;
import models.User;

public class UserActivity extends AppCompatActivity implements UserListener{

    private TextView textErrorMessage;
    private ProgressBar progressBar;
    private PreferenceManager preferenceManager;
    private RecyclerView usersRecyclerView;

    private ImageView imageBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        preferenceManager = new PreferenceManager(getApplicationContext());

        textErrorMessage = findViewById(R.id.textErrorMessage);
        progressBar = findViewById(R.id.progressBar);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        imageBack=findViewById(R.id.imageBack);

        setListeners();
        getUser();
    }

    private void setListeners(){
        imageBack.setOnClickListener(view-> getOnBackPressedDispatcher().onBackPressed());
    }
    private void showErrorMessage() {
        textErrorMessage.setText("No user available");
        textErrorMessage.setVisibility(View.VISIBLE);
    }
    private void loading(Boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }
    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), Chats.class);
        intent.putExtra("friendId", user.getUserId());
        intent.putExtra("friendName", user.getName());
        startActivity(intent);
        finish();
    }

    private void getUser() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String currentUserId = preferenceManager.getString("userId");

        // Lấy danh sách bạn bè
        database.collection("friends")
                .document(currentUserId)
                .get()
                .addOnCompleteListener(friendTask -> {
                    if (friendTask.isSuccessful() && friendTask.getResult() != null) {
                        DocumentSnapshot friendDocument = friendTask.getResult();
                        List<String> friendIds = new ArrayList<>();

                        // Duyệt qua tất cả các trường trong document để lấy danh sách bạn bè
                        if (friendDocument.getData() != null) {
                            for (String key : friendDocument.getData().keySet()) {
                                Boolean isFriend = friendDocument.getBoolean(key);
                                if (isFriend != null && isFriend) {
                                    friendIds.add(key);
                                }
                            }
                        }

                        // Lấy danh sách người dùng bạn bè (chỉ lấy bạn bè, không lấy những người không phải bạn)
                        database.collection("users")
                                .whereIn(FieldPath.documentId(), friendIds)  // Lọc chỉ lấy những người có trong danh sách bạn bè
                                .get()
                                .addOnCompleteListener(userTask -> {
                                    loading(false);
                                    if (userTask.isSuccessful() && userTask.getResult() != null) {
                                        List<User> users = new ArrayList<>();
                                        for (QueryDocumentSnapshot queryDocumentSnapshot : userTask.getResult()) {
                                            User user = queryDocumentSnapshot.toObject(User.class);
                                            user.setUserId(queryDocumentSnapshot.getId());
                                            user.setFriendStatus("friend");  // Chỉ cần gán trạng thái là "friend"
                                            users.add(user);
                                        }

                                        // Hiển thị danh sách
                                        if (!users.isEmpty()) {
                                            UsersAdapter usersAdapter = new UsersAdapter(users, this);
                                            usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                                            usersRecyclerView.setAdapter(usersAdapter);
                                            usersRecyclerView.setVisibility(View.VISIBLE);
                                        } else {
                                            showErrorMessage();
                                        }
                                    } else {
                                        showErrorMessage();
                                    }
                                });
                    } else {
                        loading(false);
                        showErrorMessage();
                    }
                });
    }

    @Override
    public void onBtnAddFriend(User user) {
    }

    @Override
    public void onBtnRemoveFriend(User user) {
    }
}