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

import com.example.chatapp.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
        Intent intent = new Intent(getApplicationContext(), ChatUserMain.class);
        intent.putExtra("user", user);
        startActivity(intent);
        finish();
    }

    private void getUser(){
        loading(true);
        FirebaseFirestore database= FirebaseFirestore.getInstance();
        database.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString("userId");
                    if(task.isSuccessful()&& task.getResult()!=null){
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot: task.getResult()){
                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                continue;
                            }
                            User user=new User();
                            user.userId=queryDocumentSnapshot.getId();;
                            user.name=queryDocumentSnapshot.getString("name");
                            user.email=queryDocumentSnapshot.getString("email");
                            user.image=queryDocumentSnapshot.getString("image");
                            user.token=queryDocumentSnapshot.getString("fcmtoken");
                            users.add(user);
                        }
                        if(!users.isEmpty()){
                            UsersAdapter usersAdapter=new UsersAdapter(users,this);
                            usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                            usersRecyclerView.setAdapter(usersAdapter);
                            usersRecyclerView.setVisibility(View.VISIBLE);
                        } else{
                            showErrorMessage();
                        }
                    } else{
                        showErrorMessage();
                    }
                });
    }
    @Override
    public void onBtnAddFriend(User user) {
        String currentUserId = preferenceManager.getString("userId");
        String targetUserId = user.userId;

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
                                Toast.makeText(this, "Yêu cầu kết bạn đã được gửi!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Gửi trạng thái nhận thất bại!", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gửi yêu cầu kết bạn thất bại!", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onBtnRemoveFriend(User user) {
        String currentUserId = preferenceManager.getString("userId");
        String targetUserId = user.userId;

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
}