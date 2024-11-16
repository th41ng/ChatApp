package com.example.chatapp.ChatUser;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.User;

public class FriendRequest extends AppCompatActivity implements UserListener{

    private TextView textErrorMessage;
    private ProgressBar progressBar;
    private PreferenceManager preferenceManager;
    private RecyclerView usersRecyclerView;

    private FriendRequestAdapter friendRequestAdapter;
    List<User> users = new ArrayList<>();
    private ImageView imageBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_request);
        preferenceManager = new PreferenceManager(getApplicationContext());

        textErrorMessage = findViewById(R.id.textErrorMessage);
        progressBar = findViewById(R.id.progressBar);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        imageBack=findViewById(R.id.imageBack);
        friendRequestAdapter = new FriendRequestAdapter(users, this);
        usersRecyclerView.setAdapter(friendRequestAdapter);

        getUser();
        setListeners();
    }
    private void setListeners(){
        imageBack.setOnClickListener(view-> getOnBackPressedDispatcher().onBackPressed());
    }
    private void showErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        textErrorMessage.setVisibility(View.VISIBLE);
    }
    private void showSuccessMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void loading(Boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void getUser() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String currentUserId = preferenceManager.getString("userId");

        // Lấy danh sách lời mời kết bạn từ "received" của người dùng hiện tại
        database.collection("friend_requests")
                .document(currentUserId)
                .collection("received")
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            // Lấy thông tin người gửi từ document
                            String senderId = queryDocumentSnapshot.getId();
                            String status = queryDocumentSnapshot.getString("status");

                            // Chỉ hiển thị yêu cầu có trạng thái "received"
                            if ("received".equals(status)) {
                                User user = new User();
                                user.userId = senderId;
                                user.name = queryDocumentSnapshot.getString("name");
                                user.email = queryDocumentSnapshot.getString("email");
                                user.image = queryDocumentSnapshot.getString("image");
                                user.token=queryDocumentSnapshot.getString("fcmtoken");
                                users.add(user);
                            }
                        }

                        // Cập nhật giao diện RecyclerView
                        if (!users.isEmpty()) {
                            FriendRequestAdapter friendRequestAdapter = new FriendRequestAdapter(users, this);
                            usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                            usersRecyclerView.setAdapter(friendRequestAdapter);
                            usersRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage("No user available");
                        }
                    } else {
                        showErrorMessage("No user available");
                    }
                });
    }

    @Override
    public void onBtnAddFriend(User user) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String currentUserId = preferenceManager.getString("userId");
        String senderId = user.userId;

        // 1. Thêm người dùng vào danh sách bạn bè
        database.collection("friends").document(currentUserId).set(
                Collections.singletonMap(senderId, true), SetOptions.merge()
        );
        database.collection("friends").document(senderId).set(
                Collections.singletonMap(currentUserId, true), SetOptions.merge()
        );

        // 2. Xóa lời mời kết bạn
        database.collection("friend_requests").document(currentUserId)
                .collection("received").document(senderId)
                .delete();

        database.collection("friend_requests").document(senderId)
                .collection("sent").document(currentUserId)
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Cập nhật danh sách RecyclerView
                        users.remove(user);
                        friendRequestAdapter.notifyDataSetChanged();
                        showSuccessMessage("Friend request accepted!");
                    } else {
                        showErrorMessage("Failed to accept friend request!");
                    }
                });
    }

    @Override
    public void onUserClicked(User user) {

    }

    @Override
    public void onBtnRemoveFriend(User user) {

    }
}