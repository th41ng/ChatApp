

package com.example.chatapp.Chat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.GroupChatRoom;
import models.User;

public class groupChat extends AppCompatActivity {
    private ImageButton btnCreate;
    private ImageButton btnBack;
    private RecyclerView allFrRecyclerView;
    private Set<String> selectedUserIds = new HashSet<>(); // Lưu các ID đã chọn
    EditText groupname;
    String groupName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);
        groupname=findViewById(R.id.groupname);

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());

        allFrRecyclerView = findViewById(R.id.all_fr);
        btnCreate = findViewById(R.id.btncreate);

        // Lấy danh sách bạn bè
        getUser();

        btnCreate.setOnClickListener(v -> {
            if (!selectedUserIds.isEmpty()) {
                createGroup(selectedUserIds);
            } else {
                Toast.makeText(this, "Please select at least one user to create a group.", Toast.LENGTH_SHORT).show();
            }
        });
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
                                        }
                                    }
                                });
                    }
                });
    }
    private void createGroup(Set<String> selectedUserIds) {
        String currentUserId = getIntent().getStringExtra("userId");
        selectedUserIds.add(currentUserId); // Thêm ID của người dùng hiện tại vào nhóm

        // Tạo chatRoomID duy nhất
        String chatRoomId = createChatRoomID(selectedUserIds);

        // Tạo tên nhóm (có thể là tên chatRoomID hoặc tên tùy chỉnh)
        groupName = groupname.getText().toString();

        // Chuyển danh sách ID thành chuỗi
        String friendIds = String.join(",", selectedUserIds);

        // Lưu thông tin nhóm vào Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        GroupChatRoom groupChatRoom = new GroupChatRoom(chatRoomId,currentUserId, new ArrayList<>(selectedUserIds), System.currentTimeMillis());

        db.collection("chatRooms").document(chatRoomId)
                .set(groupChatRoom)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Group created successfully!", Toast.LENGTH_SHORT).show();

                        // Chuyển sang giao diện chat nhóm
                        Intent intent = new Intent(this, Chats.class);
                        intent.putExtra("chatRoomId", chatRoomId); // ID phòng chat
                        intent.putExtra("managerId",currentUserId);//ID người quản lý
                        intent.putExtra("friendName", groupName);  // Tên nhóm
                        intent.putExtra("friendId", friendIds);    // Danh sách ID
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


}
