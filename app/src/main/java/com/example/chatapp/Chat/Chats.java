package com.example.chatapp.Chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloudinary.android.callback.ErrorInfo;
import com.example.chatapp.ChatUser.ChatUserMain;
import com.example.chatapp.CloudinaryManager;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloudinary.android.MediaManager;

import models.Message;
//Trang nhắn tin chính
public class Chats extends AppCompatActivity {

    private RecyclerView recyclerViewMessages;
    private MessageAdapter MessageAdapter;
    private List<Message> messageList;
    private EditText MessageInput;
    private ImageButton sendButton;
    private String currentUserId;
    private TextView chatwith;
    private String friendId;
    private String friendName;
    private String chatRoomId;
    private String currentUsername;
    private ImageView imageViewMessage;
    private ImageButton chooseImg;
    private static final int IMAGE_PICK_CODE = 1000;
    private Uri selectedImageUri;
    private ValueEventListener messagesListener;
    private ImageButton infoChatBtn;
    private Long scrollToTimestamp = null;
    private static boolean isMediaManagerInitialized = false;
    private TextView istyping;
    private  TextView onl;
private  ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity_main_chat);

        // Initialize views
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        MessageInput = findViewById(R.id.MessageInput);
        sendButton = findViewById(R.id.sendButton);
        chatwith = findViewById(R.id.chatwith);
        chooseImg = findViewById(R.id.chooseImg);
        imageViewMessage = findViewById(R.id.selectedImageView); // Updated ImageView ID
        infoChatBtn = findViewById(R.id.infoChatBtn);
        istyping=findViewById(R.id.istyping);
        btnBack = findViewById(R.id.btnBack);
        if (btnBack == null) {
            Log.e("Chats", "btnBack not found in layout!");
        }

        messageList = new ArrayList<>();
        MessageAdapter = new MessageAdapter(this, messageList);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(MessageAdapter);

        onl=findViewById(R.id.onl);
        //Lấy id người dùng hện tại
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show();
        }

        friendId = getIntent().getStringExtra("friendId");
        friendName = getIntent().getStringExtra("friendName");

        chatwith.setText(friendName);
        CloudinaryManager.initialize(this);
        createOrGetChatRoom();
        listenForFriendStatus(friendId); // Lắng nghe trạng thái của bạn bè
        //Code sự kiện onclick
        chooseImg.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, IMAGE_PICK_CODE);
        });
        //set up the infoChat button click
        infoChatBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Chats.this, InfoChat.class);
            intent.putExtra("chatRoomId", chatRoomId);
            intent.putExtra("friendId", friendId);
            intent.putExtra("friendName", friendName);
            startActivity(intent);
        });
        // Set up the send button click listener
        sendButton.setOnClickListener(v -> {
            String messageContent = MessageInput.getText().toString().trim();
            if (selectedImageUri != null) {
                // Send the image without text
                sendMessage(chatRoomId, currentUserId, null, currentUsername, selectedImageUri.toString());
                imageViewMessage.setVisibility(View.GONE); // Hide the image after sending
                selectedImageUri = null; // Reset the selected image URI
            } else if (!messageContent.isEmpty()) {
                // Send the text message
                sendMessage(chatRoomId, currentUserId, messageContent, currentUsername, null);
                MessageInput.setText(""); // Clear the input field after sending
            } else {
                Toast.makeText(Chats.this, "Please enter a message or select an image.", Toast.LENGTH_SHORT).show();
            }
        });
        btnBack.setOnClickListener(v -> {
            Log.d("Chats", "Back button clicked!");
            finish();
        });

        //kết thúc sự kiện onclick

        //Nhận timestamp để cuộn tới
        long messageTimestamp = getIntent().getLongExtra("messageTimestamp", -1);
        if (messageTimestamp != -1) {
            Log.d("Chats", "Received timestamp: " + messageTimestamp);
            // Store the timestamp in a variable for use later (e.g., scroll to message)
            scrollToTimestamp = messageTimestamp;
            // Load messages or perform the desired action
            loadMessages(chatRoomId);
        } else {
            Log.d("Chats", "No valid timestamp received.");
        }
        //kết thuúc nhận timestamp để cuộn tới


        // Thêm TextWatcher cho ô tin nhắn để kiểm tra trạng thái "đang gõ"
        MessageInput.addTextChangedListener(new TextWatcher() {
            private Handler handler = new Handler();
            private Runnable stopTyping = () -> setTypingStatus(false);
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Kiểm tra nếu ô tin nhắn không trống thì gọi setTypingStatus(true)
                if (s.length() > 0) {
                    setTypingStatus(true); // Người dùng đang gõ
                    handler.removeCallbacks(stopTyping);
                } else {
                    setTypingStatus(false); // Nếu ô tin nhắn trống thì dừng trạng thái "đang gõ"
                }
                //handler.removeCallbacks(stopTyping);
                //handler.postDelayed(stopTyping, 2000); // Dừng sau 2 giây không gõ
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        listenForTypingStatus();
        //kết thúc TextWatcher cho ô tin nhắn để kiểm tra trạng thái "đang gõ"
    }


    private void createOrGetChatRoom() {
        chatRoomId = currentUserId.compareTo(friendId) < 0 ?
                currentUserId + "_" + friendId :
                friendId + "_" + currentUserId;
        DatabaseReference chatRoomRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId);
        chatRoomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Map<String, Object> chatRoomInfo = new HashMap<>();
                    chatRoomInfo.put("user1", currentUserId);
                    chatRoomInfo.put("user2", friendId);
                    chatRoomInfo.put("timestamp", System.currentTimeMillis());
                    chatRoomRef.setValue(chatRoomInfo).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            loadMessages(chatRoomId);
                        } else {
                            Toast.makeText(getApplicationContext(), "Error creating chat room: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    loadMessages(chatRoomId);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Chats.this, "Error checking chat room: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Chats", "onActivityResult called with requestCode: " + requestCode + " and resultCode: " + resultCode);
        // Kiểm tra nếu kết quả từ việc chọn ảnh
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            if (data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                imageViewMessage.setImageURI(selectedImageUri);
                imageViewMessage.setVisibility(View.VISIBLE);
                // Ẩn ảnh khi người dùng nhấn vào nó
                imageViewMessage.setOnClickListener(v -> {
                    imageViewMessage.setVisibility(View.GONE);
                    selectedImageUri = null;
                });
            }
        }
    }

    private void scrollToMessage(long timestamp) {
        recyclerViewMessages.postDelayed(() -> {
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerViewMessages.getLayoutManager();
            if (layoutManager != null) {
                boolean found = false;
                for (int i = 0; i < messageList.size(); i++) {
                    if (messageList.get(i).getTimestamp() == timestamp) {
                        recyclerViewMessages.smoothScrollToPosition(i);
                        MessageAdapter.highlightMessage(i); // Trigger highlight
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    Log.d("Chats", "No message found with the specified timestamp.");
                }
            }
        }, 400);
    }

    //Bắt đầu code gửi tin nhắn
    private void sendMessage(String chatRoomId, String senderId, String messageContent, String senderName, String imageUri) {
        if (chatRoomId == null) {
            Toast.makeText(this, "Chat room ID is null.", Toast.LENGTH_SHORT).show();
            Log.d("fID","friendID:" + friendId +", currentUserId: " + currentUserId);
            return;
        }
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId).child("messages");
        // Prepare message data
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", senderId);
        message.put("senderName", senderName);
        message.put("content", messageContent);
        message.put("timestamp", System.currentTimeMillis());

        // If there's an image, upload it to Cloudinary
        if (imageUri != null) {
            uploadImageToCloudinary(imageUri, (uploadedImageUrl) -> {
                if (uploadedImageUrl != null) {
                    message.put("imageUri", uploadedImageUrl); // Set the image URL in the message
                    sendMessageToDatabase(messagesRef, message); // Send message to Firebase with the image URL
                } else {
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            sendMessageToDatabase(messagesRef, message); // If no image, just send the text message
        }
    }
    private void uploadImageToCloudinary(String imageUri, OnImageUploadedListener listener) {
        MediaManager.get().upload(Uri.parse(imageUri)).callback(new com.cloudinary.android.callback.UploadCallback() {
            @Override
            public void onStart(String requestId) {}
            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override
            public void onSuccess(String requestId, Map resultData) {
                String imageUrl = (String) resultData.get("secure_url"); // Get the uploaded image URL (https)
                listener.onImageUploaded(imageUrl); // Callback with the image URL
            }
            @Override
            public void onError(String requestId, ErrorInfo error) {
                listener.onImageUploaded(null); // If an error occurs, callback with null
            }
            @Override
            public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }
    private void sendMessageToDatabase(DatabaseReference messagesRef, Map<String, Object> message) {
        messagesRef.push().setValue(message).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Message sent successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error sending message: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    interface OnImageUploadedListener {
        void onImageUploaded(String imageUrl);
    }
    //kết thúc code gửi tin nhắn

    //bắt đầu code load tin nhắn để hiện leên
    private void loadMessages(String chatRoomId) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId).child("messages");
        messagesListener = messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messageList.clear();
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null) {
                        messageList.add(message);
                    }
                }
                MessageAdapter.notifyDataSetChanged();
                // Only scroll after messages are loaded
                if (scrollToTimestamp != null) {
                    scrollToMessage(scrollToTimestamp);
                    scrollToTimestamp = null;
                } else if (!messageList.isEmpty()) {
                    recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Chats.this, "Error loading messages: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
//kết thúc code load tin nhắn để hiện lên


//Code kiểm tra trạng thái đang nhập
    private void setTypingStatus(boolean isTyping) {
        DatabaseReference typingStatusRef = FirebaseDatabase.getInstance()
                .getReference("chatRooms")
                .child(chatRoomId)
                .child("typingStatus")
                .child(currentUserId);
        typingStatusRef.setValue(isTyping);
    }
    private void listenForTypingStatus() {
        DatabaseReference typingStatusRef = FirebaseDatabase.getInstance()
                .getReference("chatRooms")
                .child(chatRoomId)
                .child("typingStatus")
                .child(friendId);
        typingStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isTyping = snapshot.getValue(Boolean.class);
                if (isTyping != null && isTyping) {
                    istyping.setVisibility(View.VISIBLE);
                }
                else {
                    istyping.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Chats", "Error listening for typing status: " + error.getMessage());
            }
        });
    }
//kết thúc kiểm tra trạng thái đang nhập

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                    .getReference("chatRooms").child(chatRoomId).child("messages");
            messagesRef.removeEventListener(messagesListener);
        }
        // Dừng trạng thái typing
        setTypingStatus(false);
    }



    private void listenForFriendStatus(String friendId) {
        DatabaseReference friendStatusRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(friendId)
                .child("status");

        friendStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if ("online".equals(status)) {
                    onl.setText("Online");
                    onl.setVisibility(View.VISIBLE);
                } else {
                    onl.setText("Offline");
                    onl.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Chats", "Failed to get friend's status: " + error.getMessage());
            }
        });
    }


}