package com.example.chatapp.Chat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import models.User;

public class LS_DSTVAdapter extends RecyclerView.Adapter<LS_DSTVAdapter.ViewHolder> {

    private List<User> membersList;
    private String chatRoomId, groupLeaderID;  // Add this field to store chatRoomId and groupLeaderID

    // Update constructor to accept chatRoomId and groupLeaderID
    public LS_DSTVAdapter(List<User> membersList, String chatRoomId, String groupLeaderID) {
        this.membersList = membersList;
        this.chatRoomId = chatRoomId;  // Assign chatRoomId
        this.groupLeaderID = groupLeaderID; // Assign groupLeaderID
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_dstv, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        User user = membersList.get(position);
        holder.nameTextView.setText(user.getName());

        // Add click listener for remove button
        holder.removeMemberButton.setOnClickListener(v -> {
            // Call removeMember with the correct chatRoomId
            removeMember(user.getUserId());
        });

        // Check if the user is the current user or the group leader
        hideKickButtonForCurrentUser(holder, user.getUserId());
    }

    private void removeMember(String userId) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference chatRoomRef = database.getReference("chatRooms").child(chatRoomId);

        // Fetch the participants array to remove the user
        chatRoomRef.child("participants").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Get the current list of participants
                        List<String> participantsList = new ArrayList<>();
                        for (DataSnapshot participantSnapshot : task.getResult().getChildren()) {
                            participantsList.add(participantSnapshot.getValue(String.class));
                        }

                        // Remove the user ID from the list
                        if (participantsList.contains(userId)) {
                            participantsList.remove(userId);
                            // Update the participants array in the Firebase Realtime Database
                            chatRoomRef.child("participants").setValue(participantsList)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("ChatInfo", "Member removed successfully from Realtime Database");

                                        // Remove the member from the local list (membersList)
                                        for (int i = 0; i < membersList.size(); i++) {
                                            if (membersList.get(i).getUserId().equals(userId)) {
                                                membersList.remove(i);
                                                break; // Stop once the user is found and removed
                                            }
                                        }

                                        // Notify adapter that data has changed so it refreshes the RecyclerView
                                        notifyDataSetChanged();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.d("ChatInfo", "Failed to remove member from Realtime Database", e);
                                    });
                        } else {
                            Log.d("ChatInfo", "User not found in the participants list.");
                        }
                    } else {
                        Log.d("ChatInfo", "Failed to fetch participants list", task.getException());
                    }
                });
    }

    @Override
    public int getItemCount() {
        return membersList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageButton removeMemberButton;
        TextView nameTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.NameTextView);
            removeMemberButton = itemView.findViewById(R.id.addMemberButton);
        }
    }

    private void hideKickButtonForCurrentUser(ViewHolder holder, String userId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Check if the current user is the group leader and if the current user is not the one being viewed
        if (currentUserId.equals(userId)) {
            holder.removeMemberButton.setVisibility(View.GONE);  // Hide "kick" button for the current user (group leader)
        } else {
            holder.removeMemberButton.setVisibility(View.VISIBLE); // Show "kick" button for other members
        }
    }

}

