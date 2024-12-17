package com.example.chatapp.ChatUser;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.Chat.Chats;
import com.example.chatapp.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import models.FriendRequest;
import models.User;

public class SearchUser extends AppCompatActivity implements UserListener {
    private EditText searchEditText;
    private RecyclerView recyclerView;
    private SearchUserAdapter searchUserAdapter;
    private ProgressBar progressBar;
    private FirebaseFirestore database;
    private final List<User> userList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friend_activity_search_user);

        progressBar = findViewById(R.id.progressBar);
        searchEditText = findViewById(R.id.searchEditText);
        recyclerView = findViewById(R.id.usersRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchUserAdapter = new SearchUserAdapter(userList, this);
        recyclerView.setAdapter(searchUserAdapter);


        database = FirebaseFirestore.getInstance();


        findViewById(R.id.searchButton).setOnClickListener(view -> searchUsers());
        ImageButton btnfriend = findViewById(R.id.btnfriend);
        ImageButton btnfindfriend = findViewById(R.id.btnfindfriend);
        ImageButton btnhome = findViewById(R.id.btnhome);

        String userId = getIntent().getStringExtra("userId");
        String userName = getIntent().getStringExtra("name");
        String phone = getIntent().getStringExtra("phone");
        String encodedImage=getIntent().getStringExtra("image");
        String email = getIntent().getStringExtra("email");


        searchUsers();
        // Gán sự kiện click cho btnfriend
        btnfriend.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), UserActivity.class);
            intent.putExtra("userId",userId);
            intent.putExtra("name",userName);
            intent.putExtra("image", encodedImage);
            intent.putExtra("phone", phone);
            intent.putExtra("email", email);
            startActivity(intent);
        });

        // Gán sự kiện click cho btnfindfriend
        btnfindfriend.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SearchUser.class);
            intent.putExtra("userId",userId);
            intent.putExtra("name",userName);
            intent.putExtra("image", encodedImage);
            intent.putExtra("phone", phone);
            intent.putExtra("email", email);
            startActivity(intent);
        });

        // Gán sự kiện click cho btnhome
        btnhome.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ChatUserMain.class);
            intent.putExtra("userId",userId);
            intent.putExtra("name",userName);
            intent.putExtra("image", encodedImage);
            intent.putExtra("phone", phone);
            intent.putExtra("email", email);
            startActivity(intent);
        });


        // Lắng nghe khi người dùng nhập vào ô tìm kiếm
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                searchUsers();
            }
        });
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


    private void searchUsers() {
        loading(true);
        String searchQuery = searchEditText.getText().toString().trim();
        String currentUserId = getIntent().getStringExtra("userId");

        if (searchQuery.isEmpty()) {
            database.collection("users")
                    .get()
                    .addOnCompleteListener(task -> {
                        loading(false);
                        if (task.isSuccessful() && task.getResult() != null) {
                            userList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                User user = document.toObject(User.class);
                                user.setUserId(document.getId());
                                if (user.getUserId().equals(currentUserId)) {
                                    user.setName(user.getName() + " (Bạn)");
                                }
                                checkFriendStatus(user);
                                userList.add(user);
                            }
                            recyclerView.setVisibility(View.VISIBLE);
                            searchUserAdapter.notifyDataSetChanged();
                        }
                    });
            return;
        }

        database.collection("users")
                .orderBy("name")
                .startAt(searchQuery)
                .endAt(searchQuery + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if (task.isSuccessful()) {
                        userList.clear();
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                User user = document.toObject(User.class);
                                user.setUserId(document.getId());
                                if (user.getUserId().equals(currentUserId)) {
                                    user.setName(user.getName() + " (Bạn)");
                                }
                                checkFriendStatus(user);
                                userList.add(user);
                            }

                            recyclerView.setAdapter(searchUserAdapter);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        showErrorMessage("Lỗi tìm kiếm: " + task.getException().getMessage());
                    }
                });
    }

    private void checkFriendStatus(User user) {
        String currentUserId = getIntent().getStringExtra("userId");
        database.collection("friends")
                .document(currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        boolean isFriend = document.contains(user.getUserId()) && Boolean.TRUE.equals(document.getBoolean(user.getUserId()));
                        if (isFriend) {
                            user.setFriendStatus("friend");
                        }
                    }
                    finalizeUserUpdate(user);
                });

        database.collection("friend_requests")
                .document(currentUserId)
                .collection("sent")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (queryDocumentSnapshot.getId().equals(user.getUserId())) {
                                user.setFriendStatus("sent");
                                break;
                            }
                        }
                    }
                    finalizeUserUpdate(user);
                });

        database.collection("friend_requests")
                .document(currentUserId)
                .collection("received")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (queryDocumentSnapshot.getId().equals(user.getUserId())) {
                                user.setFriendStatus("received");
                                break;
                            }
                        }
                    }
                    finalizeUserUpdate(user);
                });
    }

    private void finalizeUserUpdate(User user) {
        int index = userList.indexOf(user);
        if (index >= 0) {
            userList.set(index, user);
            searchUserAdapter.notifyItemChanged(index); // Chỉ cập nhật item cụ thể
        }
    }

    @Override
    public void onBtnRemoveFriend(User user) {
        String currentUserId = getIntent().getStringExtra("userId");
        String targetUserId = user.getUserId();
        user.setFriendStatus("none");
        finalizeUserUpdate(user);

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection("friend_requests")
                .document(currentUserId)
                .collection("sent")
                .document(targetUserId)
                .delete()
                .addOnSuccessListener(unused -> {
                    database.collection("friend_requests")
                            .document(targetUserId)
                            .collection("received")
                            .document(currentUserId)
                            .delete()
                            .addOnSuccessListener(unused2 -> {
                                Toast.makeText(this, "Hủy kết bạn thành công!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Hủy trạng thái nhận thất bại!", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hủy yêu cầu kết bạn thất bại!", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), Chats.class);
        intent.putExtra("friendId", user.getUserId());
        intent.putExtra("friendName", user.getName());
        startActivity(intent);
        finish();
    }

    @Override
    public void onBtnAddFriend(User user) {
        String currentUserId = getIntent().getStringExtra("userId");
        String targetUserId = user.getUserId();
        user.setFriendStatus("sent");
        finalizeUserUpdate(user);

        FirebaseFirestore database = FirebaseFirestore.getInstance();

        // Lưu yêu cầu kết bạn từ currentUserId tới targetUserId
        database.collection("friend_requests")
                .document(currentUserId) // Tạo document với ID của người gửi
                .collection("sent")      // Sub-collection chứa các yêu cầu đã gửi
                .document(targetUserId)  // Tạo document với ID của người nhận
                .set(new FriendRequest("sent")) // Lưu trạng thái "sent"
                .addOnSuccessListener(unused -> {
                    // Tiếp tục thêm trạng thái nhận của người nhận
                    database.collection("friend_requests")
                            .document(targetUserId)
                            .collection("received")
                            .document(currentUserId)
                            .set(new FriendRequest("received"))
                            .addOnSuccessListener(unused2 -> {
                                showSuccessMessage("Yêu cầu kết bạn đã được gửi!");
                            })
                            .addOnFailureListener(e -> {
                                showErrorMessage("Gửi trạng thái nhận thất bại!");
                            });
                })
                .addOnFailureListener(e -> {
                    showErrorMessage("Gửi yêu cầu kết bạn thất bại!");
                });
    }
}