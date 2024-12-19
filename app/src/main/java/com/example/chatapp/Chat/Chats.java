package com.example.chatapp.Chat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.callback.ErrorInfo;
import com.example.chatapp.ChatUser.UserListener;
import com.example.chatapp.ChatUser.UsersAdapter;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import models.Message;
import models.User;

//Trang nhắn tin chính
public class Chats extends AppCompatActivity implements UserListener {

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
    private TextView onl;
    private ImageButton btnBack;
    private ImageView avt;
    private ProgressDialog progressDialog;
    LastMessageAdapter lastMessageAdapter;
    ArrayList<String> participantsList = new ArrayList<>();
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
        istyping = findViewById(R.id.istyping);
        btnBack = findViewById(R.id.btnBack);
        avt = findViewById(R.id.avt);
        messageList = new ArrayList<>();
        MessageAdapter = new MessageAdapter(this, messageList);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(MessageAdapter);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading image...");
        progressDialog.setCancelable(false);


        onl = findViewById(R.id.onl);
        //Lấy id người dùng hện tại
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show();
        }


        friendId = getIntent().getStringExtra("friendId");
        Log.d("friend", "friendId:" + friendId);


        if (friendId != null) {
            setImage(avt, friendId, this);
        } else {
            Log.e("setImage", "currentUserId is null. Unable to load user image.");
            avt.setImageResource(R.drawable.default_avatar);
        }


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
            activityInfoChatBtn();
        });

        sendButton.setOnClickListener(v -> {
            String messageContent = MessageInput.getText().toString().trim();

            // Kiểm tra quan hệ bạn bè trước khi gửi tin nhắn
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("friends")
                    .document(currentUserId) // Tài liệu của currentUserId
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                // Kiểm tra xem friendId có tồn tại trong tài liệu và có giá trị true không
                                Boolean isFriend = document.getBoolean(friendId);
                                if (isFriend != null && isFriend) {
                                    // Nếu là bạn bè, gửi tin nhắn
                                    if (selectedImageUri != null) {
                                        // Gửi hình ảnh mà không có văn bản
                                        sendMessage(chatRoomId, currentUserId, null, currentUsername, selectedImageUri.toString());
                                        imageViewMessage.setVisibility(View.GONE); // Ẩn hình ảnh sau khi gửi
                                        selectedImageUri = null; // Reset URI hình ảnh
                                    } else if (!messageContent.isEmpty()) {
                                        // Gửi tin nhắn văn bản
                                        sendMessage(chatRoomId, currentUserId, messageContent, currentUsername, null);
                                        MessageInput.setText(""); // Xóa nội dung sau khi gửi
                                    } else {
                                        Toast.makeText(Chats.this, "Please enter a message or select an image.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    // Nếu không phải bạn bè, thông báo lỗi
                                    Toast.makeText(Chats.this, "You can only message friends.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(Chats.this, "Error checking friend status.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(Chats.this, "Failed to check friend status.", Toast.LENGTH_SHORT).show();
                        }
                    });
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Kiểm tra nếu ô tin nhắn không trống thì gọi setTypingStatus(true)
                if (s.length() > 0) {
                    setTypingStatus(true); // Người dùng đang gõ
                    handler.removeCallbacks(stopTyping);
                } else {
                    setTypingStatus(false); // Nếu ô tin nhắn trống thì dừng trạng thái "đang gõ"
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        listenForTypingStatus();
    }


private void setImage(ImageView avt, String userId, Context context) {
    if (friendId.contains(",") || friendId.startsWith("GROUP_")) {
        // Nếu là nhóm chat, lấy dữ liệu từ Realtime Database
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(userId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String imageUrl = snapshot.child("groupImage").getValue(String.class);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(context)
                                .load(imageUrl)
                                .placeholder(R.drawable.default_avatar)
                                .into(avt);
                    } else {
                        avt.setImageResource(R.drawable.default_avatar);
                    }
                } else {
                    avt.setImageResource(R.drawable.default_avatar);
                    Log.d("setImage", "Group chat document not found.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                avt.setImageResource(R.drawable.default_avatar);
                Log.e("setImage", "Error fetching group chat data", error.toException());
            }
        });
    } else {
        // Nếu là người dùng, lấy dữ liệu từ Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDoc = db.collection("users").document(userId);
        userDoc.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String imageUrl = task.getResult().getString("image");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(context)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .into(avt);
                        } else {
                            avt.setImageResource(R.drawable.default_avatar);
                        }
                    } else {
                        avt.setImageResource(R.drawable.default_avatar);
                        Log.d("setImage", "Failed to fetch user document.");
                    }
                })
                .addOnFailureListener(e -> {
                    avt.setImageResource(R.drawable.default_avatar);
                    Log.e("setImage", "Error fetching user document", e);
                });
    }
}

    private void createOrGetChatRoom() {
        if (friendId.contains(",") || friendId.startsWith("GROUP_")) {
            // Xử lý nhóm chat
            chatRoomId = getIntent().getStringExtra("chatRoomId");
            if (friendId.startsWith("GROUP_")) {
                chatRoomId = friendId;
            }
            if (chatRoomId == null) {
                Toast.makeText(this, "Chat Room ID for group is null.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Lấy danh sách người tham gia từ Firebase
            DatabaseReference chatRoomRef = FirebaseDatabase.getInstance()
                    .getReference("chatRooms")
                    .child(chatRoomId);
            chatRoomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        // Tạo nhóm chat mới
                        Map<String, Object> chatRoomInfo = new HashMap<>();
                        chatRoomInfo.put("groupName", friendName); // Tên nhóm chat mặc định
                        //chatRoomInfo.put("timestamp", System.currentTimeMillis());
                        chatRoomInfo.put("chatRoomOwner", currentUserId);
                        // Danh sách người tham gia nhóm
                        String[] participantsArray = friendId.split(",");
                        for (String participant : participantsArray) {
                            participantsList.add(participant.trim());
                        }
                        chatRoomInfo.put("participants", participantsList);
                        chatRoomRef.setValue(chatRoomInfo).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                sendWelcomeMessage(chatRoomId);
                                loadMessages(chatRoomId); // Tải tin nhắn nhóm
                            } else {
                                Toast.makeText(getApplicationContext(), "Error creating group chat: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        loadMessages(chatRoomId); // Tải tin nhắn nếu nhóm đã tồn tại
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(Chats.this, "Error checking group chat: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

     } else {
           // Xử lý chat cá nhân
           Log.d("test", "vao duoc");
           chatRoomId = currentUserId.compareTo(friendId) < 0
                   ? currentUserId + "_" + friendId
                   : friendId + "_" + currentUserId;
          DatabaseReference chatRoomRef = FirebaseDatabase.getInstance()
                    .getReference("chatRooms")
                    .child(chatRoomId);
            chatRoomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        // Tạo phòng chat cá nhân
                        Map<String, Object> chatRoomInfo = new HashMap<>();
                        chatRoomInfo.put("user1", currentUserId);
                       chatRoomInfo.put("user2", friendId);
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



    }


    // Hàm gửi tin nhắn chào mừng
    private void sendWelcomeMessage(String chatRoomId) {
        String welcomeMessageContent = "Welcome to the chat room!";
        Message welcomeMessage = new Message();
        welcomeMessage.setContent(welcomeMessageContent);
        welcomeMessage.setSenderId("system"); // ID của hệ thống hoặc quản trị
        welcomeMessage.setTimestamp(System.currentTimeMillis());
        // Lưu tin nhắn vào Firebase trong node tin nhắn của phòng chat
        DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                .getReference("chatRooms")
                .child(chatRoomId)
                .child("messages")
                .push();
        messagesRef.setValue(welcomeMessage).addOnCompleteListener(task -> {});
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
        progressDialog.show(); // Show the loader

        MediaManager.get().upload(Uri.parse(imageUri)).callback(new com.cloudinary.android.callback.UploadCallback() {
            @Override
            public void onStart(String requestId) {}
            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override
            public void onSuccess(String requestId, Map resultData) {
                progressDialog.dismiss();
                String imageUrl = (String) resultData.get("secure_url"); // Get the uploaded image URL (https)
                listener.onImageUploaded(imageUrl); // Callback with the image URL
            }
            @Override
            public void onError(String requestId, ErrorInfo error) {
                progressDialog.dismiss(); // Hide the loader
                listener.onImageUploaded(null); // If an error occurs, callback with null
            }
            @Override
            public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }
    private void sendMessageToDatabase(DatabaseReference messagesRef, Map<String, Object> message) {
        messagesRef.push().setValue(message).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
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
                .child("typingStatus");

        typingStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean anyUserTyping = false;

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    Boolean isTyping = userSnapshot.getValue(Boolean.class);

                    // Kiểm tra nếu đây là người khác đang nhập, không phải bạn
                    if (isTyping != null && isTyping && !userSnapshot.getKey().equals(currentUserId)) {
                        anyUserTyping = true;
                        break;
                    }
                }

                // Nếu bất kỳ người dùng nào (không phải chính bạn) đang nhập, hiển thị thông báo
                if (anyUserTyping) {
                    istyping.setVisibility(View.VISIBLE);
                } else {
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

                if(friendId.startsWith("GROUP_") || friendId.contains(",")){
                    onl.setText("");
                }else{
                    if ("online".equals(status)) {
                        onl.setText("Online");
                        onl.setVisibility(View.VISIBLE);
                    } else {
                        onl.setText("Offline");
                        onl.setVisibility(View.VISIBLE);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Chats", "Failed to get friend's status: " + error.getMessage());
            }
        });
    }
    private void activityInfoChatBtn(){
        // Tạo một danh sách các lựa chọn
        if(friendId.startsWith("GROUP_") || friendId.contains(",")){
            String[] options = { "Thông tin nhóm","Tìm kiếm tin nhắn trong nhóm","Chuyển vai trò quản lý", "Hủy"};
            // Hiển thị AlertDialog
            new AlertDialog.Builder(this)
                    .setTitle("Lựa chọn thao tác" )
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                checkChatRoomOwner(chatRoomId,
                                        () -> { // Nếu là chủ phòng
                                            Intent intentTT = new Intent(Chats.this, ChatInfo.class);
                                            intentTT.putExtra("chatRoomId", chatRoomId);
                                            intentTT.putExtra("groupName", friendName); // Tên nhóm
                                            intentTT.putExtra("chatRoomOwner", currentUserId);
                                            startActivity(intentTT);
                                        },
                                        () -> { // Nếu không phải chủ phòng
                                            Toast.makeText(Chats.this, "Bạn không có quyền truy cập vào thông tin nhóm.", Toast.LENGTH_SHORT).show();
                                        });
                                break;

                            case 1:
                                Intent intent = new Intent(Chats.this, searchInChat.class);
                                intent.putExtra("chatRoomId", chatRoomId);
                                intent.putExtra("friendId", friendId);
                                intent.putExtra("friendName", friendName);
                                startActivity(intent);
                                break;

                            case 2:
                                checkChatRoomOwner(chatRoomId,
                                        () -> { // Nếu là chủ phòng
                                            userChangeManager(); // Gọi hàm xử lý nếu người dùng là quản lý
                                        },
                                        () -> { // Nếu không phải chủ phòng
                                            Toast.makeText(Chats.this, "Bạn không có quyền thay đổi quản lý.", Toast.LENGTH_SHORT).show();
                                        });
                                break;

                            case 3: // Hủy
                                dialog.dismiss();
                                break;
                        }
                    })
                    .show();
        }
        else {
            Intent intent = new Intent(Chats.this, searchInChat.class);
            intent.putExtra("chatRoomId", chatRoomId);
            intent.putExtra("friendId", friendId);
            intent.putExtra("friendName", friendName);
            startActivity(intent);
        }

    }


    private void checkChatRoomOwner(String chatRoomId, Runnable onOwnerAction, Runnable onNotOwnerAction) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        dbRef.child("chatRooms").child(chatRoomId).child("chatRoomOwner")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Lấy giá trị của "chatRoomOwner"
                            String chatRoomOwner = snapshot.getValue(String.class);
                            Log.d("ChatRoomOwner", "Chat Room Owner: " + chatRoomOwner);
                            if (currentUserId.equals(chatRoomOwner)) {
                                if (onOwnerAction != null) onOwnerAction.run(); // Hành động khi là chủ phòng
                            } else {
                                if (onNotOwnerAction != null) onNotOwnerAction.run(); // Hành động khi không phải chủ phòng
                            }
                        } else {
                            Log.d("RealtimeDatabase", "Không tồn tại dữ liệu chatRoomOwner!");
                            if (onNotOwnerAction != null) onNotOwnerAction.run();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("RealtimeDatabase", "Lỗi khi lấy dữ liệu", error.toException());
                    }
                });
    }




    private void userChangeManager(){
// Tham chiếu đến Realtime Database để lấy thông tin từ chatRooms
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference chatRoomRef = database.getReference("chatRooms").child(chatRoomId);

// Lấy dữ liệu từ chatRoom
        chatRoomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Lấy danh sách participants
                    List<String> participants = (List<String>) dataSnapshot.child("participants").getValue();

                    if (participants != null && participants.size() > 1) {  // Kiểm tra có ít nhất 2 người tham gia (bỏ qua currentUserId)
                        participants.remove(currentUserId);

                        // Tham chiếu đến Firestore để lấy thông tin người dùng
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users")
                                .whereIn(FieldPath.documentId(), participants)  // Lọc chỉ lấy những người là thành viên
                                .get()
                                .addOnCompleteListener(userTask -> {
                                    if (userTask.isSuccessful() && userTask.getResult() != null) {
                                        List<User> users = new ArrayList<>();
                                        for (QueryDocumentSnapshot queryDocumentSnapshot : userTask.getResult()) {
                                            User user = queryDocumentSnapshot.toObject(User.class);
                                            user.setUserId(queryDocumentSnapshot.getId());
                                            users.add(user);
                                        }

                                        // Hiển thị danh sách
                                        if (!users.isEmpty()) {
                                            // Hiển thị AlertDialog khi cần
                                            showUserDialog(users);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firestore", "Lỗi lấy dữ liệu người dùng", e);
                                });
                    } else {
                        Toast.makeText(Chats.this, "Không có người tham gia", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d("RealtimeDatabase", "Không có dữ liệu tồn tại");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("RealtimeDatabase", "Lỗi lấy dữ liệu chatRoom", databaseError.toException());
            }
        });


    }
    private void showUserDialog(List<User> userList) {
        // Inflate layout của RecyclerView
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.friend_dialog_recycler, null);

        // Tìm RecyclerView
        RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Tạo adapter và thiết lập
        UsersAdapter adapter = new UsersAdapter(userList, this);
        recyclerView.setAdapter(adapter);

        // Hiển thị AlertDialog
        new AlertDialog.Builder(this)
                .setTitle("Chọn User")
                .setView(dialogView)
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
    @Override
    public void onUserClicked(User user) {
        String userId = user.getUserId();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("chatRooms").document(chatRoomId);

        // Lấy thông tin trường "changeId"
        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Kiểm tra xem trường "changeId" có tồn tại hay không
                        String currentChangeId = documentSnapshot.contains("changeId")
                                ? documentSnapshot.getString("changeId")
                                : null;

                        if (currentChangeId == null) {
                            // Trường changeId chưa được thiết lập => Chưa có yêu cầu thay đổi
                            updateChangeId(userId);
                        } else if (!currentChangeId.equals(userId)) {
                            // Trường changeId chứa userId khác => hỏi người dùng có muốn thay đổi không
                            showConfirmationDialog(userId, "Bạn có muốn thay đổi người đã yêu cầu  không?", true);
                        } else {
                            // Trường changeId trùng với userId => hỏi người dùng có muốn hủy yêu cầu không
                            showConfirmationDialog(userId, "Bạn có muốn hủy yêu cầu thay đổi không?", false);
                        }
                    } else {
                        Log.d("Firestore", "Tài liệu người dùng không tồn tại");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Lỗi lấy dữ liệu", e);
                });
    }

    private void showConfirmationDialog(String userId, String message, boolean isChangeRequest) {
        // Hiển thị hộp thoại xác nhận với hai lựa chọn "Yes" và "No"
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (isChangeRequest) {
                        // Nếu là thay đổi yêu cầu, cập nhật changeId
                        updateChangeId(userId);
                    } else {
                        // Nếu là hủy yêu cầu, xóa trường changeId
                        deleteChangeId(userId);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void updateChangeId(String userId) {
        // Thêm yêu cầu vào collection "managerChangeRequests"
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> request = new HashMap<>();
        request.put("chatRoomId", chatRoomId);
        request.put("requestedBy", currentUserId); // Người yêu cầu
        request.put("requestedTo", userId); // Người được yêu cầu làm quản lý
        request.put("timestamp", System.currentTimeMillis()); // Thời gian tạo yêu cầu

        // Kiểm tra xem tài liệu với chatRoomId đã tồn tại chưa
        db.collection("managerChangeRequests")
                .whereEqualTo("chatRoomId", chatRoomId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Tài liệu đã tồn tại, cập nhật nội dung
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            db.collection("managerChangeRequests").document(document.getId())
                                    .set(request) // Cập nhật nội dung tài liệu
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getApplicationContext(), "Yêu cầu thay đổi quản lý đã được cập nhật", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getApplicationContext(), "Có lỗi khi cập nhật yêu cầu", Toast.LENGTH_SHORT).show();
                                        Log.e("Firestore", "Error updating change request", e);
                                    });
                        }
                    } else {
                        // Tài liệu chưa tồn tại, thêm mới
                        db.collection("managerChangeRequests")
                                .add(request)
                                .addOnSuccessListener(documentReference -> {
                                    Toast.makeText(getApplicationContext(), "Yêu cầu thay đổi quản lý đã được gửi", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getApplicationContext(), "Có lỗi khi gửi yêu cầu", Toast.LENGTH_SHORT).show();
                                    Log.e("Firestore", "Error adding change request", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getApplicationContext(), "Có lỗi khi kiểm tra yêu cầu", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error checking change request", e);
                });

        // Cập nhật changeId
        DocumentReference userRef = db.collection("chatRooms").document(chatRoomId);

        userRef.update("changeId", userId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getApplicationContext(), "Yêu cầu thay đổi quản lý thành công", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getApplicationContext(), "Có lỗi khi cập nhật yêu cầu", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error updating changeId", e);
                });
    }

    private void deleteChangeId(String userId) {
        // Xóa changeId
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("chatRooms").document(chatRoomId);

        userRef.update("changeId", FieldValue.delete())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getApplicationContext(), "Yêu cầu thay đổi quản lý đã bị hủy", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getApplicationContext(), "Có lỗi khi hủy yêu cầu", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error deleting changeId", e);
                });
        // Xóa yêu cầu trong collection "managerChangeRequests"
        db.collection("managerChangeRequests")
                .whereEqualTo("chatRoomId", chatRoomId)
                .whereEqualTo("requestedTo", userId) // Điều kiện tìm đúng yêu cầu
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        db.collection("managerChangeRequests").document(document.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getApplicationContext(), "Yêu cầu thay đổi quản lý trong managerChangeRequests đã bị hủy", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getApplicationContext(), "Có lỗi khi xóa yêu cầu trong managerChangeRequests", Toast.LENGTH_SHORT).show();
                                    Log.e("Firestore", "Error deleting changeId in managerChangeRequests", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getApplicationContext(), "Có lỗi khi truy vấn yêu cầu trong managerChangeRequests", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error fetching requests in managerChangeRequests", e);
                });
    }
    @Override
    public void onBtnAddFriend(User user) {
    }
    @Override
    public void onBtnRemoveFriend(User user) {
    }
}