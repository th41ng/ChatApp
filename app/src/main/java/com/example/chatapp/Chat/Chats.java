package com.example.chatapp.Chat;

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

import com.cloudinary.android.callback.ErrorInfo;
import com.example.chatapp.ChatUser.ChangeProfile;
import com.example.chatapp.ChatUser.ChatUserMain;
import com.example.chatapp.ChatUser.UserInfor;
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
        if (btnBack == null) {
            Log.e("Chats", "btnBack not found in layout!");
        }

        messageList = new ArrayList<>();
        MessageAdapter = new MessageAdapter(this, messageList);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(MessageAdapter);

        onl = findViewById(R.id.onl);
        //Lấy id người dùng hện tại
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show();
        }

        friendId = getIntent().getStringExtra("friendId");
        Log.d("friend","friendId:"+friendId);
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
                //handler.removeCallbacks(stopTyping);
                //handler.postDelayed(stopTyping, 2000); // Dừng sau 2 giây không gõ
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        listenForTypingStatus();
        //kết thúc TextWatcher cho ô tin nhắn để kiểm tra trạng thái "đang gõ"
    }


    private void createOrGetChatRoom() {
        if (friendId.contains(",") || friendId.startsWith("GROUP_")) {
            // Xử lý nhóm chat
            chatRoomId = getIntent().getStringExtra("chatRoomId");
            if(friendId.startsWith("GROUP_")){
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
                        chatRoomInfo.put("groupName", "New Group Chat"); // Tên nhóm chat mặc định
                        chatRoomInfo.put("timestamp", System.currentTimeMillis());
                        chatRoomInfo.put("chatRoomOwner", currentUserId);
                        // Danh sách người tham gia nhóm
                        String[] participantsArray = friendId.split(",");
                        List<String> participantsList = new ArrayList<>();
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
    private void activityInfoChatBtn(){
        // Tạo một danh sách các lựa chọn
        if(friendId.startsWith("GROUP_")){
            String[] options = { "Tìm kiếm tin nhắn trong nhóm","Chuyển vai trò quản lý", "Hủy"};

            // Hiển thị AlertDialog
            new AlertDialog.Builder(this)
                    .setTitle("Lựa chọn thao tác" )
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                Intent intent = new Intent(Chats.this, InfoChat.class);
                                intent.putExtra("chatRoomId", chatRoomId);
                                intent.putExtra("friendId", friendId);
                                intent.putExtra("friendName", friendName);
                                startActivity(intent);
                                break;
                            case 1:
                                // Khởi tạo tham chiếu đến Firestore
                                FirebaseFirestore db = FirebaseFirestore.getInstance();

                                // Truy vấn vào collection "chatRooms"
                                db.collection("chatRooms")
                                        .document(chatRoomId)
                                        .get()
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                DocumentSnapshot document = task.getResult();
                                                if (document.exists()) {
                                                    // Lấy giá trị của "managerId"
                                                    String managerId = document.getString("managerId");
                                                    Log.d("Manager ID", "Manager ID: " + managerId);
                                                    if(currentUserId.equals(managerId)){
                                                        userChangeManager();
                                                    }
                                                    else {
                                                        Toast.makeText(this,"Bạn không có quyền thay đổi quản lý",Toast.LENGTH_SHORT).show();
                                                    }
                                                } else {
                                                    Log.d("Firestore", "Không tồn tại dữ liệu!");
                                                }
                                            } else {
                                                Log.e("Firestore", "Lỗi lấy dữ liệu", task.getException());
                                            }
                                        });
                                break;
                            case 2: // Hủy
                                dialog.dismiss();
                                break;
                        }
                    })
                    .show();
        }
        else {
            Intent intent = new Intent(Chats.this, InfoChat.class);
            intent.putExtra("chatRoomId", chatRoomId);
            intent.putExtra("friendId", friendId);
            intent.putExtra("friendName", friendName);
            startActivity(intent);
        }

    }
    private void userChangeManager(){
        // Tham chiếu đến tài liệu trong Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("chatRooms").document(chatRoomId);

        // Lấy dữ liệu từ tài liệu
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Lấy danh sách participants
                List<String> participants = (List<String>) documentSnapshot.get("participants");

                if (participants != null) {
                    participants.remove(currentUserId);
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
                            });
                }
            } else {
                Log.d("Firestore", "Không có dữ liệu tồn tại ");
            }
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Lỗi lấy dữ liệu", e);
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