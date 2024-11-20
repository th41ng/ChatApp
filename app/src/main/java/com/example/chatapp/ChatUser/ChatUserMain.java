package com.example.chatapp.ChatUser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.Chat.LastMessageAdapter;
import models.Message;
import com.example.chatapp.PreferenceManager;
import com.example.chatapp.R;
import com.example.chatapp.Re_Sign.LoginActivity;
import com.example.chatapp.UserHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
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
import java.util.List;

public class ChatUserMain extends AppCompatActivity  {
    private PreferenceManager preferenceManager;
    TextView name;
    ImageView image;
    AppCompatImageView btnSignOut;
    ImageView imageSearch;
    BottomNavigationView footer_menu;
    private RecyclerView recyclerViewChats;
    private LastMessageAdapter lastMessageAdapter;
    private List<Message> messageList;
    private DatabaseReference chatReference;
    private ConstraintLayout lastChatView;
    private boolean isMessagesFetched = false;
    String currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friend_activity_chat_user_main);

        preferenceManager = new PreferenceManager(getApplicationContext());

        initializeUI();
        loadUserDetails();
        getToken();
        setListeners();



    }
    @Override
    protected void onResume() {
        super.onResume();
            fetchMessages();
    }


    private void initializeUI() {
        name = findViewById(R.id.textName);
        image = findViewById(R.id.imageProfile);

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
        String userName = preferenceManager.getString("name");
        String encodedImage = preferenceManager.getString("image");

        if (userName != null) {
            name.setText(userName);
        } else {
            name.setText("User");
        }

        if (encodedImage != null && !encodedImage.isEmpty()) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            image.setImageBitmap(bitmap);
        }
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
                    String userFriend = "";

                    Log.d("test", "Checking room: user1=" + user1 + ", user2=" + user2);

                    // Check who the friend is
                    if (!user1.equals(currentUserID)) {
                        userFriend = user1;
                    } else if (!user2.equals(currentUserID)) {
                        userFriend = user2;
                    }

                    // Check if currentUserId is in either user1 or user2
                    if (currentUserID.equals(user1) || currentUserID.equals(user2)) {
                        Log.d("test", "Match found for user: " + currentUserID);

                        // Fetch the name of the user friend
                        String finalUserFriend = userFriend;
                        UserHelper.getUserInfo(userFriend, new UserHelper.OnUserInfoFetched() {
                            @Override
                            public void onUserInfoFetched(String name, String email, String imageUrl) {
                                Log.d("UserInfo", "Friend's name: " + name);

                                // Fetch the messages in the room
                                DataSnapshot messagesSnapshot = roomSnapshot.child("messages");

                                // Check if there are any messages
                                if (messagesSnapshot.exists()) {
                                    Message lastMessage = null; // Track the last message for this chat

                                    // Iterate through the messages
                                    for (DataSnapshot messageSnapshot : messagesSnapshot.getChildren()) {
                                        Message message = messageSnapshot.getValue(Message.class);

                                        // Set the correct friend's name for each message
                                        if (message != null) {
                                            message.setFriendName(name); // Set the friend's name in the message
                                            message.setFriendId(finalUserFriend); // Set the friend's ID in the message);
                                            lastMessage = message; // Update the last message
                                        }
                                    }

                                    // After iterating through all the messages, add the last message to the list
                                    if (lastMessage != null) {
                                        messageList.add(lastMessage); // Only add the last message
                                    }

                                    // Notify the adapter that the data has changed
                                    lastMessageAdapter.notifyDataSetChanged();

                                    // Check if user is at the bottom of the list, and scroll to new message
                                    if (recyclerViewChats != null && messageList.size() > 0) {
                                        recyclerViewChats.scrollToPosition(messageList.size() - 1);
                                    }
                                }
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                showToast("Failed to get friend info: " + errorMessage);
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Error fetching messages: " + error.getMessage());
            }
        });
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
    private void setListeners() {
        btnSignOut = findViewById(R.id.imageSignOut);
        btnSignOut.setOnClickListener(view -> signOut());

        footer_menu=findViewById(R.id.footer_menu);
        footer_menu.setOnItemReselectedListener(new NavigationBarView.OnItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) {
                if(item.getItemId()==R.id.menu_friends){
                    Intent intent = new Intent(getApplicationContext(), UserActivity.class);
                    startActivity(intent);
                }
                if(item.getItemId()==R.id.menu_add_friend){
                    Intent intent = new Intent(getApplicationContext(), FriendRequest.class);
                    startActivity(intent);
                }
            }
        });

        imageSearch=findViewById(R.id.imageSearch);
        imageSearch.setOnClickListener(view->{
            Intent intent=new Intent(getApplicationContext(), SearchUser.class);
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

        // Chuyển về màn hình đăng nhập
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }


    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
