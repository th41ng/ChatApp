package com.example.chatapp.Chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import models.User;

public class LS_addTVAdapter extends RecyclerView.Adapter<LS_addTVAdapter.ViewHolder> {
    private List<User> friendsList;
    private String chatRoomId;
    private String groupLeaderID;
    private Context context;

    public LS_addTVAdapter(List<User> friendsList, String chatRoomId, String groupLeaderID, Context context) {
        this.friendsList = friendsList;
        this.chatRoomId = chatRoomId;
        this.groupLeaderID = groupLeaderID;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_addtv, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        User user = friendsList.get(position);
        holder.nameTextView.setText(user.getName());

        // Handle item click event for adding users to the group
        holder.addMemberButton.setOnClickListener(v -> {
            // Logic to add the friend to the group
            addUserToGroup(user.getUserId());
        });
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    private void addUserToGroup(String userId) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference chatRoomRef = database.getReference("chatRooms").child(chatRoomId);

        // Lấy danh sách hiện tại và thêm userId vào mảng
        chatRoomRef.child("participants").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot snapshot = task.getResult();
                List<String> participants = new ArrayList<>();

                // Đọc danh sách hiện tại từ snapshot
                for (DataSnapshot participantSnapshot : snapshot.getChildren()) {
                    String participantId = participantSnapshot.getValue(String.class);
                    if (participantId != null) {
                        participants.add(participantId);
                    }
                }

                // Thêm userId mới nếu chưa có trong danh sách
                if (!participants.contains(userId)) {
                    participants.add(userId);

                    // Cập nhật lại mảng participants trong Realtime Database
                    chatRoomRef.child("participants").setValue(participants)
                            .addOnCompleteListener(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    Toast.makeText(context, "User added to group", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, "Failed to add user", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Toast.makeText(context, "User is already in the group", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Failed to fetch current participants", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        ImageButton addMemberButton;
        public ViewHolder(View itemView) {
            super(itemView);
            addMemberButton = itemView.findViewById(R.id.addMemberButton);
            nameTextView = itemView.findViewById(R.id.NameTextView);
        }
    }
}
