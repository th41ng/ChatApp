package com.example.chatapp.ChatUser;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import models.User;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
    private final List<User> users;
    private final UserListener userListener;

    public UsersAdapter(List<User> users, UserListener userListener) {
        this.users = users;
        this.userListener = userListener;
    }

    public List<User> getUsersList() {
        return users;  // usersList là danh sách người dùng bạn bè hiện tại trong adapter
    }


    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textEmail;
        ImageView imageProfile;
        View viewStatus;
        ImageButton btnUnfriend;  // Declare the button here

        UserViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textEmail = itemView.findViewById(R.id.textEmail);
            imageProfile = itemView.findViewById(R.id.imageProfile);
            viewStatus = itemView.findViewById(R.id.status);
            btnUnfriend = itemView.findViewById(R.id.btnUnfriend);  // Initialize it here
        }
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_item_container_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.textName.setText(user.getName());
        holder.textEmail.setText(user.getEmail());

        // Load profile image
        setImage(holder.imageProfile, user.getUserId(), holder.itemView.getContext());

        // Check if the user is a friend and show "Unfriend" button if true
        if (user.getFriendStatus().equals("friend")) {
            holder.btnUnfriend.setVisibility(View.VISIBLE);  // Make sure the button is visible
            holder.btnUnfriend.setOnClickListener(v -> {
                // Trigger the unfriend action
                userListener.onBtnRemoveFriend(user);  // Pass the clicked user to the listener
            });
        } else {
            holder.btnUnfriend.setVisibility(View.GONE);  // Hide if not a friend
        }

        holder.itemView.setOnClickListener(v -> userListener.onUserClicked(user));

        // Set item alpha if the user is disabled
        if (Boolean.TRUE.equals(user.getDisabled())) {
            holder.itemView.setAlpha(0.5f); // Dim the view
        } else {
            holder.itemView.setAlpha(1.0f); // Regular view
        }

        // Display user's online status
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUserId()).child("status");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String status = dataSnapshot.getValue(String.class);
                if (status != null && status.equals("online")) {
                    holder.viewStatus.setBackgroundResource(R.drawable.status_online_circle); // Green circle for online
                } else {
                    holder.viewStatus.setBackgroundResource(R.drawable.status_offline_circle); // Grey circle for offline
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error: " + databaseError.getMessage());
            }
        });
    }

    private void setImage(ImageView imageView, String userId, Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDoc = db.collection("users").document(userId);

        userDoc.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String imageUrl = task.getResult().getString("image"); // Get image URL from Firestore
                        if (imageUrl != null) {
                            Glide.with(context)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_avatar)  // Placeholder image
                                    .into(imageView);
                        } else {
                            imageView.setImageResource(R.drawable.default_avatar); // Default avatar if no image
                        }
                    } else {
                        imageView.setImageResource(R.drawable.default_avatar); // Default avatar
                        Log.d("FriendRequest", "Error retrieving data from Firestore");
                    }
                })
                .addOnFailureListener(e -> {
                    imageView.setImageResource(R.drawable.default_avatar); // Default avatar on failure
                    Log.d("FriendRequest", "Error retrieving data from Firestore", e);
                });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }
}
