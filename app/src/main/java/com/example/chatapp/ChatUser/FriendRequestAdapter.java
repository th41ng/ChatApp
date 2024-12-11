package com.example.chatapp.ChatUser;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import models.User;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder> {

    private final List<User> friendRequestList;
    private final UserListener userListener;

    public FriendRequestAdapter(List<User> friendRequestList, UserListener userListener) {
        this.friendRequestList = friendRequestList;
        this.userListener= userListener;
    }

    public static class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textEmail;
        ImageView imageProfile;

        Button btnAgree,btnReject;
        FriendRequestViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textEmail = itemView.findViewById(R.id.textEmail);
            imageProfile = itemView.findViewById(R.id.imageProfile);
            btnAgree = itemView.findViewById(R.id.btnAgree);
            btnReject=itemView.findViewById(R.id.btnReject);
        }
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_item_container, parent, false);
        return new FriendRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FriendRequestViewHolder holder, int position) {
        User user = friendRequestList.get(position);
        holder.textName.setText(user.getName());
        holder.textEmail.setText(user.getEmail());

        // Tải ảnh từ Firestore và hiển thị
        setImage(holder.imageProfile, user.getUserId(), holder.itemView.getContext());

        holder.btnAgree.setOnClickListener(view -> {
            userListener.onBtnAddFriend(user);
            holder.btnAgree.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);

        });
        holder.btnReject.setOnClickListener(view -> {
            userListener.onBtnRemoveFriend(user);
            holder.btnAgree.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
        });
    }
    private void setImage(ImageView imageView, String userId, Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDoc = db.collection("users").document(userId);

        userDoc.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String imageUrl = task.getResult().getString("image"); // Trường 'image' lưu URL ảnh
                        if (imageUrl != null) {
                            // Sử dụng Glide để tải ảnh từ URL
                            Glide.with(context)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .into(imageView);
                        } else {
                            imageView.setImageResource(R.drawable.default_avatar); // Hình mặc định
                        }
                    } else {
                        imageView.setImageResource(R.drawable.default_avatar); // Hình mặc định
                        Log.d("FriendRequest", "Lỗi khi lấy dữ liệu Firestore");
                    }
                })
                .addOnFailureListener(e -> {
                    imageView.setImageResource(R.drawable.default_avatar); // Hình mặc định
                    Log.d("FriendRequest", "Lỗi khi lấy dữ liệu Firestore", e);
                });
    }

    @Override
    public int getItemCount() {
        return friendRequestList.size();
    }


}