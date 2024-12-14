package com.example.chatapp.ChatUser;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.User;

public class FriendRequest extends AppCompatActivity implements UserListener{

    private ProgressBar progressBar;
    private RecyclerView usersRecyclerView;

    List<User> users = new ArrayList<>();
    private ImageView imageBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friend_activity_request);

        progressBar = findViewById(R.id.progressBar);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        imageBack=findViewById(R.id.btnBack);

        FriendRequestAdapter friendRequestAdapter = new FriendRequestAdapter(users, this);
        usersRecyclerView.setAdapter(friendRequestAdapter);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        getUser();
        setListeners();
    }
    private void setListeners(){
        imageBack.setOnClickListener(view-> getOnBackPressedDispatcher().onBackPressed());
    }
    private void showErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
        String currentUserId = getIntent().getStringExtra("userId");
        users.clear(); // Xóa danh sách cũ

        assert currentUserId != null;
        database.collection("friend_requests")
                .document(currentUserId)
                .collection("received")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            String senderId = queryDocumentSnapshot.getId();
                            String status = queryDocumentSnapshot.getString("status");

                            if ("received".equals(status)) {
                                Task<DocumentSnapshot> userTask = database.collection("users").document(senderId).get();
                                tasks.add(userTask);
                                userTask.addOnSuccessListener(userDocument -> {
                                    if (userDocument.exists()) {
                                        User user = userDocument.toObject(User.class);
                                        user.setUserId(userDocument.getId());
                                        users.add(user);
                                    }
                                });
                            }
                        }

                        // Chờ tất cả truy vấn hoàn thành
                        Tasks.whenAllComplete(tasks).addOnCompleteListener(allTask -> {
                            loading(false);
                            updateRecyclerView();
                        });

                    } else {
                        loading(false);
                        showErrorMessage("Thất bại lấy danh sách lời mời kết bạn");
                    }
                });
    }

    private void updateRecyclerView() {
        if (!users.isEmpty()) {
            FriendRequestAdapter friendRequestAdapter = new FriendRequestAdapter(users, this);
            usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            usersRecyclerView.setAdapter(friendRequestAdapter);
            usersRecyclerView.setVisibility(View.VISIBLE);
        } else {
            showErrorMessage("Không có lời mời kết bạn");
        }
    }


    @Override
    public void onBtnAddFriend(User user) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String currentUserId = getIntent().getStringExtra("userId");
        String senderId = user.getUserId();

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
                        showSuccessMessage("Đã chấp nhận kết bạn");
                    } else {
                        showErrorMessage("Thất bại trong việc kết bạn!");
                    }
                });
    }

    @Override
    public void onUserClicked(User user) {
    }

    @Override
    public void onBtnRemoveFriend(User user) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String currentUserId =getIntent().getStringExtra("userId");
        String senderId = user.getUserId();

        // 1. Xóa lời mời kết bạn
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
                        showSuccessMessage("Đã từ chối lời mời kết bạn");
                    } else {
                        showErrorMessage("Thất bại từ chối kết bạn!");
                    }
                });
    }
}