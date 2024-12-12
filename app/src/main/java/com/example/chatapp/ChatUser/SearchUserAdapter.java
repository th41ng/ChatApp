package com.example.chatapp.ChatUser;


import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import models.User;

public class SearchUserAdapter  extends RecyclerView.Adapter<SearchUserAdapter.UserViewHolder>{
    private List<User> userList;
    private final UserListener userListener;

    public SearchUserAdapter(List<User> userList,UserListener userListener) {
        this.userList = userList;
        this.userListener= userListener;
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textEmail;
        Button btnAddFriend;
        ImageView imageProfile;

        public UserViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textEmail = itemView.findViewById(R.id.textEmail);
            btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
            imageProfile = itemView.findViewById(R.id.imageProfile);
        }
    }
    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_item_container_search_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.textName.setText(user.getName());
        holder.textEmail.setText(user.getEmail());

//        holder.itemView.setOnClickListener(view->{userListener.onUserClicked(user);});

        // Tải ảnh từ Firestore và hiển thị
        setImage(holder.imageProfile, user.getUserId(), holder.itemView.getContext());

        // Hiển thị đúng trạng thái nút bấm
        // Ẩn nút nếu là người dùng hiện tại
        if (user.getUserId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            holder.btnAddFriend.setVisibility(View.GONE);
        } else {
            holder.btnAddFriend.setVisibility(View.VISIBLE);
            switch (user.getFriendStatus()) {
                case "friend":
                    holder.btnAddFriend.setText("Nhắn tin");
                    holder.btnAddFriend.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
                    holder.btnAddFriend.setOnClickListener(v -> {
                        userListener.onUserClicked(user);
                    });
                    break;
                case "sent":
                    holder.btnAddFriend.setText("Hủy kết bạn");
                    holder.btnAddFriend.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
                    holder.btnAddFriend.setOnClickListener(view -> {
                        userListener.onBtnRemoveFriend(user);
                    });
                    break;
                case "received":
                    holder.btnAddFriend.setText("Lời mời kết bạn");
                    holder.btnAddFriend.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.gray));
                    holder.btnAddFriend.setOnClickListener(view -> {
                        Intent intent=new Intent(holder.itemView.getContext(), FriendRequest.class);
                        holder.itemView.getContext().startActivity(intent);
                    });
                    break;
                default:
                    holder.btnAddFriend.setText("Kết bạn");
                    holder.btnAddFriend.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue));
                    holder.btnAddFriend.setOnClickListener(view -> {
                        // Gửi lời mời kết bạn
                        userListener.onBtnAddFriend(user);
                    });
                    break;
            }
        }
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
        return userList.size();
    }

}