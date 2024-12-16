package com.example.chatapp.ChatUser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import android.widget.ImageButton;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.Chat.LastMessageAdapter;
import models.Message;

import com.example.chatapp.R;
import com.example.chatapp.Re_Sign.LoginActivity;
import com.example.chatapp.UserHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatUserMain extends AppCompatActivity  {
    TextView name;
    ImageView image;
    ImageButton btnSignout;
    ImageButton btnfriend,btnfindfriend;
    ImageButton btnhome;
    private RecyclerView recyclerViewChats;
    private LastMessageAdapter lastMessageAdapter;
    private List<Message> messageList;
    private DatabaseReference chatReference;
    private ConstraintLayout lastChatView;
    private boolean isMessagesFetched = false;
    private  ImageButton friendrequest;

    String currentUserID,userName,encodedImage,phone,email ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friend_activity_chat_user_main);

        currentUserID = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("name");
        phone = getIntent().getStringExtra("phone");
        encodedImage=getIntent().getStringExtra("image");
        email = getIntent().getStringExtra("email");

        if (currentUserID == null || currentUserID.isEmpty()) {
            showToast("User ID not found. Please log in again.");
            finish(); // Kết thúc nếu không có userId
            return;
        }

        initializeUI();
        loadUserDetails();
        setListeners();
    }
    @Override
    protected void onResume() {
        super.onResume();
            fetchMessages();

    }


    private void initializeUI() {
        name = findViewById(R.id.textName);
        image = findViewById(R.id.imageButton);

        recyclerViewChats = findViewById(R.id.recyclerViewChats);
        lastChatView = findViewById(R.id.lastChatView);
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(this));
        messageList = new ArrayList<>();
        lastMessageAdapter = new LastMessageAdapter(this, messageList);
        recyclerViewChats.setAdapter(lastMessageAdapter);

        // Initialize Firebase Realtime Database reference
        chatReference = FirebaseDatabase.getInstance().getReference("chats");
        Log.d("test", "currentUID" + currentUserID);

    }

    private void loadUserDetails() {

        name.setText(userName != null ? userName : "User");
        if (encodedImage != null && !encodedImage.isEmpty()) {
            Glide.with(this)
                    .load(encodedImage)
                    .placeholder(R.drawable.default_avatar)
                    .into(image); // Gán ảnh vào ImageView
        } else {
            setImage(image,currentUserID,this);
        }
    }
    private void setImage(ImageView imageView, String userId, Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDoc = db.collection("users").document(userId);

        userDoc.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String imageUrl = task.getResult().getString("image"); // Trường 'image' lưu URL ảnh
                        if (imageUrl != null) {
                            // Sử dụng Glide để tải ảnh từ URL
                            Glide.with(context)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .into(imageView);
                        } else {
                            imageView.setImageResource(R.drawable.default_avatar); // Hình mặc định
                        }
                    } else {
                        imageView.setImageResource(R.drawable.default_avatar); // Hình mặc định
                        Log.d("FriendRequest", "Lỗi khi lấy dữ liệu Firestore");
                    }
                })
                .addOnFailureListener(e -> {
                    imageView.setImageResource(R.drawable.default_avatar); // Hình mặc định
                    Log.d("FriendRequest", "Lỗi khi lấy dữ liệu Firestore", e);
                });
    }

    private void fetchMessages() {
        Log.d("test", "currentUID: " + currentUserID);
        if (currentUserID == null || currentUserID.isEmpty()) {
            showToast("User ID not found. Please log in again.");
            return;
        }

        DatabaseReference chatRoomsRef = FirebaseDatabase.getInstance().getReference("chatRooms");

        // Listen for all chat rooms
        chatRoomsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear(); // Clear the message list before adding new data

                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    String user1 = roomSnapshot.child("user1").getValue(String.class);
                    String user2 = roomSnapshot.child("user2").getValue(String.class);

                    // Check for one-to-one chat
                    if ((currentUserID.equals(user1) || currentUserID.equals(user2))) {
                        Log.d("test", "Match found for user: " + currentUserID);

                        // Determine the other user (the friend)
                        String userFriend = user1.equals(currentUserID) ? user2 : user1;

                        // Fetch friend info and messages
                        UserHelper.getUserInfo(userFriend, new UserHelper.OnUserInfoFetched() {
                            @Override
                            public void onUserInfoFetched(String name, String email, String imageUrl) {
                                Log.d("UserInfo", "Friend's name: " + name);

                                // Fetch the messages in the room
                                DataSnapshot messagesSnapshot = roomSnapshot.child("messages");

                                if (messagesSnapshot.exists()) {
                                    Message lastMessage = null;

                                    for (DataSnapshot messageSnapshot : messagesSnapshot.getChildren()) {
                                        Message message = messageSnapshot.getValue(Message.class);

                                        if (message != null) {
                                            message.setFriendName(name); // Set the friend's name
                                            message.setFriendId(userFriend); // Set the friend's ID
                                            lastMessage = message; // Update the last message
                                        }
                                    }

                                    if (lastMessage != null) {
                                        messageList.add(lastMessage); // Add the last message
                                    }

                                    Collections.sort(messageList, (m1, m2) -> Long.compare(m2.getTimestamp(), m1.getTimestamp()));
                                    lastMessageAdapter.notifyDataSetChanged();
                                    recyclerViewChats.scrollToPosition(messageList.size() - 1);
                                }
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                showToast("Failed to get friend info: " + errorMessage);
                            }
                        });

                    } else {
                        // Handle group chat scenario (more than 2 participants)
                        List<String> participants = new ArrayList<>();
                        for (DataSnapshot participantSnapshot : roomSnapshot.child("participants").getChildren()) {
                            participants.add(participantSnapshot.getValue(String.class));
                        }

                        if (participants.contains(currentUserID)) {
                            Log.d("test", "Group chat match found for user: " + currentUserID);

                            // Fetch group name
                            String groupName = roomSnapshot.child("groupName").getValue(String.class);
                            if (groupName == null) {
                                groupName = "Group Chat"; // Default group name if not available
                            }

                            String groupId = roomSnapshot.getKey(); // Use room ID as the group ID

                            // Fetch the messages in the room
                            DataSnapshot messagesSnapshot = roomSnapshot.child("messages");

                            if (messagesSnapshot.exists()) {
                                Message lastMessage = null;

                                for (DataSnapshot messageSnapshot : messagesSnapshot.getChildren()) {
                                    Message message = messageSnapshot.getValue(Message.class);

                                    if (message != null) {
                                        message.setFriendName(groupName); // Set the group name for group chat
                                        message.setFriendId(groupId); // Set group ID in message
                                        lastMessage = message;
                                    }
                                }

                                if (lastMessage != null) {
                                    messageList.add(lastMessage); // Add the last message for the group
                                }

                                Collections.sort(messageList, (m1, m2) -> Long.compare(m2.getTimestamp(), m1.getTimestamp()));
                                lastMessageAdapter.notifyDataSetChanged();
                                recyclerViewChats.scrollToPosition(messageList.size() - 1);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Error fetching messages: " + error.getMessage());
            }
        });
    }


    private void setListeners() {
        btnSignout = findViewById(R.id.btnSignout);
        btnSignout.setOnClickListener(view -> signOut());
        btnfriend=findViewById(R.id.btnfriend);
        btnfriend.setOnClickListener(view->{
            Intent intent=new Intent(getApplicationContext(), UserActivity.class);
            intent.putExtra("userId",currentUserID);
            intent.putExtra("name", userName);
            intent.putExtra("image", encodedImage);
            intent.putExtra("phone", phone);
            intent.putExtra("email", email);
            startActivity(intent);
        });
        btnhome=findViewById(R.id.btnhome);
        btnhome.setOnClickListener(view->{
            Intent intent=new Intent(getApplicationContext(), ChatUserMain.class);
            intent.putExtra("userId",currentUserID);
            intent.putExtra("name", userName);
            intent.putExtra("image", encodedImage);
            intent.putExtra("phone", phone);
            intent.putExtra("email", email);
            startActivity(intent);
        });
        btnfindfriend=findViewById(R.id.btnfindfriend);
        btnfindfriend.setOnClickListener(view->{
            Intent intent=new Intent(getApplicationContext(), SearchUser.class);
            intent.putExtra("userId",currentUserID);
            intent.putExtra("name", userName);
            intent.putExtra("image", encodedImage);
            intent.putExtra("phone", phone);
            intent.putExtra("email", email);
            startActivity(intent);
        });
        friendrequest=findViewById(R.id.friendrequest);
        friendrequest.setOnClickListener(view->{
            Intent intent=new Intent(getApplicationContext(), FriendRequest.class);
            intent.putExtra("userId",currentUserID);
            intent.putExtra("name", userName);
            intent.putExtra("image", encodedImage);
            intent.putExtra("phone", phone);
            intent.putExtra("email", email);
            startActivity(intent);
        });
        image.setOnClickListener(view->{
            Intent intent=new Intent(getApplicationContext(), ChangeProfile.class);
            intent.putExtra("userId",currentUserID);
            intent.putExtra("name", userName);
            intent.putExtra("image", encodedImage);
            intent.putExtra("phone", phone);
            intent.putExtra("email", email);
            startActivity(intent);
        });

    }



    private void signOut() {
        FirebaseAuth.getInstance().signOut(); // Đăng xuất khỏi Firebase

        // Xóa thông tin lưu trữ trong SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("USER_FILE.xml", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear(); // Xóa toàn bộ dữ liệu trong file SharedPreferences
        editor.apply();

        DatabaseReference userStatusRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUserID)
                .child("status");

        // Lắng nghe sự kiện khi mất kết nối
        userStatusRef.setValue("offline");

        // Chuyển về màn hình đăng nhập
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }


    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    //on off
    @Override
    protected void onStart() {
        super.onStart();

        DatabaseReference userStatusRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUserID)
                .child("status");

        // Set trạng thái "online" khi kết nối với Firebase
        userStatusRef.setValue("online");

        // Lắng nghe sự kiện khi mất kết nối
        userStatusRef.onDisconnect().setValue("offline");
    }
    @Override
    protected void onStop() {
        super.onStop();

        DatabaseReference userStatusRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUserID)
                .child("status");

        // Lắng nghe sự kiện mất kết nối và tự động cập nhật trạng thái
        userStatusRef.onDisconnect().setValue("offline");
    }
//on off
}
