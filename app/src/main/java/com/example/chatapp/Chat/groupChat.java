package com.example.chatapp.Chat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.chatapp.CloudinaryManager;
import com.example.chatapp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.GroupChatRoom;
import models.User;

public class groupChat extends AppCompatActivity {
    private ImageButton btnCreate;
    private ImageButton btnBack, groupAvt;
    private RecyclerView allFrRecyclerView;
    private Set<String> selectedUserIds = new HashSet<>(); // Lưu các ID đã chọn
    private EditText groupname;
    private String groupName;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private Uri selectedImageUri;
    String chatRoomId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity_group_chat);

        CloudinaryManager.initialize(this);

        groupname = findViewById(R.id.groupname);
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        allFrRecyclerView = findViewById(R.id.all_fr);
        btnCreate = findViewById(R.id.btncreate);
        groupAvt = findViewById(R.id.groupAvt);
        // Lấy danh sách bạn bè
        getUser();

        btnCreate.setOnClickListener(v -> {
            if (!selectedUserIds.isEmpty()) {
                // Kiểm tra nếu người dùng đã chọn ảnh hay không
                if (selectedImageUri != null ) {
                    // Nếu có ảnh, tải ảnh lên Cloudinary
                    createGroup(selectedUserIds);
                    uploadImageToCloudinary(selectedImageUri);
                } else {
                   Toast.makeText(this, "Please select an image or group name", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please select at least one user to create a group.", Toast.LENGTH_SHORT).show();
            }
        });

        if (groupAvt != null) {
            Glide.with(this)
                    .load(groupAvt)
                    .placeholder(R.drawable.friend)
                    .into(groupAvt);
        } else {
            groupAvt.setImageResource(R.drawable.default_avatar);
        }

        pickImageLauncher();
        groupAvt.setOnClickListener(view->openImagePicker());
    }




    private void getUser() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String currentUserId = getIntent().getStringExtra("userId");

        database.collection("friends")
                .document(currentUserId)
                .get()
                .addOnCompleteListener(friendTask -> {
                    if (friendTask.isSuccessful() && friendTask.getResult() != null) {
                        DocumentSnapshot friendDocument = friendTask.getResult();
                        List<String> friendIds = new ArrayList<>();

                        if (friendDocument.getData() != null) {
                            for (String key : friendDocument.getData().keySet()) {
                                Boolean isFriend = friendDocument.getBoolean(key);
                                if (isFriend != null && isFriend) {
                                    friendIds.add(key);
                                }
                            }
                        }

                        if (friendIds.isEmpty()) {
                            // Không có bạn bè
                            Toast.makeText(this, "You have no friends to add to the group.", Toast.LENGTH_SHORT).show();
                        } else {
                            database.collection("users")
                                    .whereIn(FieldPath.documentId(), friendIds)
                                    .get()
                                    .addOnCompleteListener(userTask -> {
                                        if (userTask.isSuccessful() && userTask.getResult() != null) {
                                            List<User> users = new ArrayList<>();
                                            for (QueryDocumentSnapshot queryDocumentSnapshot : userTask.getResult()) {
                                                User user = queryDocumentSnapshot.toObject(User.class);
                                                user.setUserId(queryDocumentSnapshot.getId());
                                                users.add(user);
                                            }

                                            if (!users.isEmpty()) {
                                                all_fr_to_groupAdapter adapter = new all_fr_to_groupAdapter(users, selectedIds -> {
                                                    selectedUserIds = selectedIds;
                                                });
                                                allFrRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                                                allFrRecyclerView.setAdapter(adapter);
                                            } else {
                                                // Nếu không tìm thấy người dùng nào, hiển thị thông báo
                                                Toast.makeText(this, "No friends found.", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            // Lỗi khi lấy dữ liệu người dùng
                                            Toast.makeText(this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        // Lỗi khi lấy dữ liệu bạn bè
                        Toast.makeText(this, "Failed to retrieve friends.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createGroup(Set<String> selectedUserIds) {
        String currentUserId = getIntent().getStringExtra("userId");
        selectedUserIds.add(currentUserId); // Thêm ID của người dùng hiện tại vào nhóm

        // Tạo chatRoomID duy nhất
        chatRoomId = createChatRoomID(selectedUserIds);

        // Tạo tên nhóm (có thể là tên chatRoomID hoặc tên tùy chỉnh)
        groupName = groupname.getText().toString();

        // Chuyển danh sách ID thành chuỗi
        String friendIds = String.join(",", selectedUserIds);

        // Kiểm tra sự tồn tại của chatRoomId trong Realtime Database
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("chatRooms");
        databaseReference.child(chatRoomId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // ChatRoomId đã tồn tại
                    Toast.makeText(groupChat.this, "Chat room already exists. Please try with different users.", Toast.LENGTH_SHORT).show();
                } else {
                    // ChatRoomId chưa tồn tại, lưu vào Firestore
                    saveGroupToFirestore(chatRoomId, currentUserId, selectedUserIds, friendIds);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(groupChat.this, "Failed to check chat room existence: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void saveGroupToFirestore(String chatRoomId, String managerId, Set<String> userIds, String friendIds) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        GroupChatRoom groupChatRoom = new GroupChatRoom(chatRoomId, managerId, new ArrayList<>(userIds), System.currentTimeMillis());
        db.collection("chatRooms").document(chatRoomId)
                .set(groupChatRoom)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Group created successfully!", Toast.LENGTH_SHORT).show();
                        // Chuyển sang giao diện chat nhóm
                        Intent intent = new Intent(this, Chats.class);
                        intent.putExtra("chatRoomId", chatRoomId); // ID phòng chat
                        intent.putExtra("managerId", managerId);   // ID người quản lý
                        intent.putExtra("friendName", groupName); // Tên nhóm
                        intent.putExtra("friendId", friendIds);   // Danh sách ID
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to create group: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String createChatRoomID(Set<String> userIds) {
        List<String> sortedUserIds = new ArrayList<>(userIds);
        Collections.sort(sortedUserIds); // Sắp xếp để đảm bảo tính duy nhất
        StringBuilder chatRoomIdBuilder = new StringBuilder();
        chatRoomIdBuilder.append("GROUP_"); // Thêm tiền tố "GROUP_"
        for (String userId : sortedUserIds) {
            chatRoomIdBuilder.append(userId).append("_");
        }
        // Loại bỏ dấu "_" cuối cùng nếu có
        if (chatRoomIdBuilder.length() > 0) {
            chatRoomIdBuilder.setLength(chatRoomIdBuilder.length() - 1);
        }
        return chatRoomIdBuilder.toString();
    }




    // Mở trình chọn ảnh từ thiết bị
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent); // Sử dụng API ActivityResultContracts để mở trình chọn ảnh
    }

    // Chọn ảnh và tải lên Cloudinary
    private void pickImageLauncher() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData(); // Lấy URI ảnh đã chọn
                        if (selectedImageUri != null) {
                            Glide.with(this)
                                    .load(selectedImageUri)  // Hiển thị ảnh được chọn trong ImageView
                                    .into(groupAvt); // groupAvt là ImageView của bạn
                        }
                    }
                }
        );
    }

    // Tải ảnh lên Cloudinary
    private void uploadImageToCloudinary(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int bytesRead;
            byte[] data = new byte[1024];
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            byte[] imageData = buffer.toByteArray();

            MediaManager.get().upload(imageData)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Toast.makeText(groupChat.this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = (String) resultData.get("secure_url"); // Lấy URL của ảnh đã tải lên
                            Toast.makeText(groupChat.this, "Tải ảnh thành công!", Toast.LENGTH_SHORT).show();

                            // Lưu URL ảnh vào Firebase Realtime Database
                            saveImageUrlToFirebase(imageUrl);
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Log.e("CloudinaryError", "Lỗi tải ảnh: " + error.getDescription());
                            Toast.makeText(groupChat.this, "Lỗi tải ảnh: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                        }
                    }).dispatch();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi mở ảnh!", Toast.LENGTH_SHORT).show();
        }
    }

    // Lưu URL ảnh vào Firebase Realtime Database GROUP_Z9Tflr5SyKfyYpmb9DFjyNpZAsQ2_xjpj6KEMMQgDeIzQwbWzg7hJ3hW2 bc
    private void saveImageUrlToFirebase(String imageUrl) {
        // Giả sử bạn đã có ID của nhóm (chatRoomId) hoặc nơi lưu trữ ảnh
        String groupId = chatRoomId; // Thay đổi theo nhóm mà bạn muốn lưu ảnh
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(groupId);

        // Lưu URL ảnh vào trường "groupImage"
        groupRef.child("groupImage").setValue(imageUrl).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(groupChat.this, "Cập nhật ảnh nhóm thành công!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(groupChat.this, "Cập nhật ảnh nhóm thất bại!", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
