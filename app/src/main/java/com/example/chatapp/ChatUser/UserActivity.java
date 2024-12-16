package com.example.chatapp.ChatUser;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.Chat.Chats;
import com.example.chatapp.Chat.groupChat;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import models.User;

public class UserActivity extends AppCompatActivity implements UserListener{

    private ProgressBar progressBar;
    private RecyclerView usersRecyclerView;
    private ImageButton btncreategr;
    private ImageButton btnhome, btnfriend, btnfindfriend;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friend_activity_user);

        progressBar = findViewById(R.id.progressBar);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        getUser();
        btncreategr=findViewById(R.id.btncreategr);
        btnfriend = findViewById(R.id.btnfriend);
        btnfindfriend = findViewById(R.id.btnfindfriend);
        btnhome= findViewById(R.id.btnhome);

        String currentUserID = getIntent().getStringExtra("userId");
        String userName = getIntent().getStringExtra("name");
        String phone = getIntent().getStringExtra("phone");
        String encodedImage=getIntent().getStringExtra("image");
        String email = getIntent().getStringExtra("email");

        btnfriend.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), UserActivity.class);
            intent.putExtra("userId",currentUserID);
            intent.putExtra("name", userName);
            intent.putExtra("image", encodedImage);
            intent.putExtra("phone", phone);
            intent.putExtra("email", email);
            startActivity(intent);
        });

        // Gán sự kiện click cho btnfindfriend
        btnfindfriend.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SearchUser.class);
            intent.putExtra("userId",currentUserID);
            intent.putExtra("name", userName);
            intent.putExtra("image", encodedImage);
            intent.putExtra("phone", phone);
            intent.putExtra("email", email);
            startActivity(intent);
        });

        // Gán sự kiện click cho btnhome
        btnhome.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ChatUserMain.class);
            intent.putExtra("userId",currentUserID);
            intent.putExtra("name", userName);
            intent.putExtra("image", encodedImage);
            intent.putExtra("phone", phone);
            intent.putExtra("email", email);
            startActivity(intent);
        });
        btncreategr.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), groupChat.class);
            intent.putExtra("userId",currentUserID);
            startActivity(intent);
        });
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
        String currentUserId = getIntent().getStringExtra("userId");

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

                        if(friendIds.isEmpty())
                        {
                            loading(false);
                            Toast.makeText(getApplicationContext(), "Không có bạn bè", Toast.LENGTH_SHORT).show();
                        } else {
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
                                                Toast.makeText(getApplicationContext(), "Không có danh sách", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Lỗi lấy dữ liệu thất bại", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        loading(false);
                        Toast.makeText(getApplicationContext(), "Lấy dữ liệu không thành công", Toast.LENGTH_SHORT).show();
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