package com.example.chatapp.Chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.Re_Sign.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import models.User;

public class ChatInfo extends AppCompatActivity {

    private ImageButton btnBack, expandDSTV, expandAddTV, deleteGroup;
    private TextView st_NhomTruong, groupNameEditText;
    private RecyclerView LS_DSTV, LS_addTV;
    private String chatRoomId, participants, groupName, groupLeaderID;

    private List<User> membersList = new ArrayList<>();
    private LS_DSTVAdapter ls_DSTVAdapter;
    private LS_addTVAdapter ls_addTVAdapter;  // Add the adapter for friends not in group

    private List<User> friendsList = new ArrayList<>(); // List to store friends not in the group
    private boolean isDSTVExpanded = false; // Trạng thái cho LS_DSTV
    private boolean isAddTVExpanded = false; // Trạng thái cho LS_addTV
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_info);

        // Get the data from the intent
        chatRoomId = getIntent().getStringExtra("chatRoomId");
        groupName = getIntent().getStringExtra("groupName");
        //groupLeaderID = getIntent().getStringExtra("chatRoomOwner");

        // Initialize the views
        btnBack = findViewById(R.id.btnBack);
        st_NhomTruong = findViewById(R.id.st_NhomTruong);
        expandDSTV = findViewById(R.id.expandDSTV);
        expandAddTV = findViewById(R.id.expandAddTV);
        deleteGroup = findViewById(R.id.deleteGroup);
        groupNameEditText = findViewById(R.id.groupNameTextView);
        LS_DSTV = findViewById(R.id.LS_DSTV);
        LS_addTV = findViewById(R.id.LS_addTV);

        // Set the group name
        groupNameEditText.setText(groupName);

        // Create and set the adapter for RecyclerView (Group members)
        ls_DSTVAdapter = new LS_DSTVAdapter(membersList, chatRoomId, groupLeaderID);
        LS_DSTV.setLayoutManager(new LinearLayoutManager(this));
        LS_DSTV.setAdapter(ls_DSTVAdapter);

        // Create and set the adapter for RecyclerView (Friends not in group)
        ls_addTVAdapter = new LS_addTVAdapter(friendsList, chatRoomId, groupLeaderID, this);
        LS_addTV.setLayoutManager(new LinearLayoutManager(this));
        LS_addTV.setAdapter(ls_addTVAdapter);

        // Handle back button click
        btnBack.setOnClickListener(v -> finish());

        setupExpandCollapseFunctionality();

        deleteGroup.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xóa nhóm")
                    .setMessage("Bạn có chắc chắn muốn xóa nhóm này không?")
                    .setPositiveButton("Xóa", (dialog, which) -> deleteGroupFromFirebase())
                    .setNegativeButton("Hủy", null)
                    .show();
        });
        // Fetch the group leader's name
        fetchGroupLeaderName();

        // Fetch the group members and friends not in group
        showGroupMembers();
        showFriendsNotInGroup();

        // Lấy tham chiếu Realtime Database
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child("chatRooms").child(chatRoomId).child("chatRoomOwner")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            groupLeaderID = snapshot.getValue(String.class);
                            fetchGroupLeaderName(); // Lấy tên nhóm trưởng sau khi cập nhật groupLeaderID
                        } else {
                            Log.d("ChatInfo", "Chat room owner not found");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d("ChatInfo", "Failed to fetch chat room owner", error.toException());
                    }
                });
    }

    private void setupExpandCollapseFunctionality() {
        // Handle expand/collapse for LS_DSTV
        expandDSTV.setOnClickListener(v -> {
            isDSTVExpanded = !isDSTVExpanded; // Toggle state
            toggleVisibility(LS_DSTV); // Show/hide the list
            updateButtonImage(expandDSTV, isDSTVExpanded); // Update button icon
        });
        // Handle expand/collapse for LS_addTV
        expandAddTV.setOnClickListener(v -> {
            isAddTVExpanded = !isAddTVExpanded; // Toggle state
            toggleVisibility(LS_addTV); // Show/hide the list
            updateButtonImage(expandAddTV, isAddTVExpanded); // Update button icon
        });
    }
    // Update button image based on state
    private void updateButtonImage(ImageButton button, boolean isExpanded) {
        if (isExpanded) {
            button.setImageResource(R.drawable.collapse); // Icon for expanded state
        } else {
            button.setImageResource(R.drawable.expand); // Icon for collapsed state
        }
    }
    // Toggle visibility of a given view
    private void toggleVisibility(View view) {
        view.setVisibility(view.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }


    private void deleteGroupFromFirebase() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        // Xóa dữ liệu từ Firebase Realtime Database
        dbRef.child("chatRooms").child(chatRoomId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("DeleteGroup", "Chat room removed from chatRooms.");
                    } else {
                        Log.e("DeleteGroup", "Failed to remove chat room from chatRooms", task.getException());
                    }
                });
        Intent intent = new Intent(ChatInfo.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    @Override
    protected void onStart() {
        super.onStart();
        // Re-fetch the participants list when the activity starts
        showGroupMembers();
        showFriendsNotInGroup();

    }

    private void fetchGroupLeaderName() {
        if (groupLeaderID != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(groupLeaderID)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String leaderName = document.getString("name");
                                st_NhomTruong.setText(leaderName);
                            } else {
                                Log.d("ChatInfo", "No such document");
                            }
                        } else {
                            Log.d("ChatInfo", "Failed to get document", task.getException());
                        }
                    });
        }
    }

    private void showGroupMembers() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference chatRoomRef = database.getReference("chatRooms").child(chatRoomId);

        chatRoomRef.child("participants").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot participantsSnapshot = task.getResult();
                membersList.clear(); // Clear the current list to avoid duplication
                List<String> participantIds = new ArrayList<>();
                for (DataSnapshot participantSnapshot : participantsSnapshot.getChildren()) {
                    String participantId = participantSnapshot.getValue(String.class);
                    if (participantId != null) {
                        participantIds.add(participantId);
                    }
                }
                // Fetch user details for all participants at once
                fetchUserDetails(participantIds);
            } else {
                Log.d("ChatInfo", "No participants found or failed to fetch participants");
            }
        });
    }

    private void fetchUserDetails(List<String> participantIds) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // List to hold user data
        List<User> users = new ArrayList<>();

        // Fetch user data from Firestore for each participant
        for (String participantId : participantIds) {
            db.collection("users").document(participantId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot snapshot = task.getResult();
                    String participantName = snapshot.getString("name");
                    if (participantName != null) {
                        // Add the participant to the users list
                        users.add(new User(participantId, participantName));
                    }
                } else {
                    Log.d("ChatInfo", "Failed to fetch user details", task.getException());
                }

                // When all users are fetched, update the RecyclerView
                if (users.size() == participantIds.size()) {
                    membersList.clear();
                    membersList.addAll(users);
                    ls_DSTVAdapter.notifyDataSetChanged();  // Notify the adapter to update the RecyclerView
                }
            });
        }
    }

    private void showFriendsNotInGroup() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Lấy danh sách các thành viên trong nhóm từ Realtime Database
        FirebaseDatabase.getInstance().getReference("chatRooms")
                .child(chatRoomId)
                .child("participants")
                .get()
                .addOnCompleteListener(participantsTask -> {
                    if (participantsTask.isSuccessful() && participantsTask.getResult() != null) {
                        DataSnapshot participantsSnapshot = participantsTask.getResult();
                        List<String> participantIds = new ArrayList<>();
                        // Lấy danh sách participant IDs
                        for (DataSnapshot participantSnapshot : participantsSnapshot.getChildren()) {
                            String participantId = participantSnapshot.getValue(String.class);
                            if (participantId != null) {
                                participantIds.add(participantId);
                            }
                        }
                        // Lấy danh sách bạn bè của người dùng hiện tại từ Firestore
                        database.collection("friends")
                                .document(currentUserId)
                                .get()
                                .addOnCompleteListener(friendTask -> {
                                    if (friendTask.isSuccessful() && friendTask.getResult() != null) {
                                        DocumentSnapshot friendDocument = friendTask.getResult();
                                        List<String> friendIds = new ArrayList<>();
                                        // Lọc bạn bè từ document "friends"
                                        if (friendDocument.getData() != null) {
                                            for (String key : friendDocument.getData().keySet()) {
                                                Boolean isFriend = friendDocument.getBoolean(key);
                                                if (isFriend != null && isFriend && !participantIds.contains(key)) {
                                                    friendIds.add(key);
                                                }
                                            }
                                        }
                                        // Lấy thông tin chi tiết của bạn bè chưa trong nhóm
                                        if (!friendIds.isEmpty()) {
                                            database.collection("users")
                                                    .whereIn(FieldPath.documentId(), friendIds)
                                                    .get()
                                                    .addOnCompleteListener(userTask -> {
                                                        if (userTask.isSuccessful() && userTask.getResult() != null) {
                                                            List<User> filteredFriends = new ArrayList<>();
                                                            for (DocumentSnapshot userSnapshot : userTask.getResult()) {
                                                                User user = userSnapshot.toObject(User.class);
                                                                if (user != null) {
                                                                    user.setUserId(userSnapshot.getId());
                                                                    filteredFriends.add(user);
                                                                }
                                                            }
                                                            // Cập nhật RecyclerView
                                                            friendsList.clear();
                                                            friendsList.addAll(filteredFriends);
                                                            ls_addTVAdapter.notifyDataSetChanged();
                                                        } else {
                                                            Log.e("ChatInfo", "Failed to fetch user details", userTask.getException());
                                                        }
                                                    });
                                        } else {
                                            // Không có bạn bè nào chưa được thêm vào nhóm
                                            friendsList.clear();
                                            ls_addTVAdapter.notifyDataSetChanged();
                                        }
                                    } else {
                                        Log.e("ChatInfo", "Failed to fetch friends list", friendTask.getException());
                                    }
                                });
                    } else {
                        Log.e("ChatInfo", "Failed to fetch participants", participantsTask.getException());
                    }
                });
    }


}
