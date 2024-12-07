package com.example.chatapp.Chat;

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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import models.User;

public class all_fr_to_groupAdapter extends RecyclerView.Adapter<all_fr_to_groupAdapter.GroupUserViewHolder> {
    private final List<User> users;
    private final Set<String> selectedUserIds = new HashSet<>(); // Lưu các userId đã chọn
    private final OnUserSelectionListener selectionListener;

    public interface OnUserSelectionListener {
        void onUserSelectionChanged(Set<String> selectedUserIds);
    }

    public all_fr_to_groupAdapter(List<User> users, OnUserSelectionListener selectionListener) {
        this.users = users;
        this.selectionListener = selectionListener;
    }

    class GroupUserViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImageView;
        TextView userNameTextView;
        ImageButton addToGroupButton;

        public GroupUserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            addToGroupButton = itemView.findViewById(R.id.btnadd_fr_gr);
        }
    }

    @NonNull
    @Override
    public GroupUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_item_all_fr_to_gr, parent, false);
        return new GroupUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupUserViewHolder holder, int position) {
        User user = users.get(position);
        holder.userNameTextView.setText(user.getName());
        setImage(holder.avatarImageView, user.getUserId(), holder.itemView.getContext());

        // Cập nhật trạng thái nút "Thêm" dựa trên việc người dùng đã được chọn hay chưa
        boolean isSelected = selectedUserIds.contains(user.getUserId());
        holder.addToGroupButton.setImageResource(
                isSelected ? R.drawable.ban : R.drawable.baseline_add_circle_outline_24
        );


        holder.addToGroupButton.setOnClickListener(v -> {
            if (isSelected) {
                selectedUserIds.remove(user.getUserId());
            } else {
                selectedUserIds.add(user.getUserId());
            }
            notifyItemChanged(position); // Cập nhật nút bấm
            selectionListener.onUserSelectionChanged(selectedUserIds); // Gửi thông báo cập nhật
        });
    }

    private void setImage(ImageView imageView, String userId, Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDoc = db.collection("users").document(userId);

        userDoc.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String imageUrl = task.getResult().getString("image");
                        if (imageUrl != null) {
                            Glide.with(context)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .into(imageView);
                        } else {
                            imageView.setImageResource(R.drawable.default_avatar);
                        }
                    } else {
                        imageView.setImageResource(R.drawable.default_avatar);
                        Log.d("FriendRequest", "Lỗi khi lấy dữ liệu Firestore");
                    }
                })
                .addOnFailureListener(e -> {
                    imageView.setImageResource(R.drawable.default_avatar);
                    Log.d("FriendRequest", "Lỗi khi lấy dữ liệu Firestore", e);
                });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }
}

