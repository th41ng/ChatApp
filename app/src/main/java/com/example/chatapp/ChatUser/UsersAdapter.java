package com.example.chatapp.ChatUser;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textEmail;
        ImageView imageProfile;
        View viewStatus;
        UserViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textEmail = itemView.findViewById(R.id.textEmail);
            imageProfile = itemView.findViewById(R.id.imageProfile);
            viewStatus=itemView.findViewById(R.id.status);
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
        // Tải ảnh từ Firestore và hiển thị
        setImage(holder.imageProfile, user.getUserId(), holder.itemView.getContext());

        holder.itemView.setOnClickListener(v -> userListener.onUserClicked(user));
        // Thiết lập trạng thái của item (mờ và vô hiệu hóa sự kiện click nếu bị vô hiệu hóa)
        if (Boolean.TRUE.equals(user.getDisabled())) {
            // Thay đổi giao diện để chỉ rõ tài khoản bị vô hiệu hóa
            holder.itemView.setAlpha(0.5f); // Làm mờ
        } else {
            holder.itemView.setAlpha(1.0f); // Hiển thị bình thường
        }

        //Thể hiện trạng thái của user
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUserId()).child("status");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String status = dataSnapshot.getValue(String.class);
                // Xử lý trạng thái người dùng
                if (status!=null && status.equals("online")) {
                    holder.viewStatus.setBackgroundResource(R.drawable.status_online_circle);// Hình tròn xanh
                } else {
                    holder.viewStatus.setBackgroundResource(R.drawable.status_offline_circle); // Hình tròn xám
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
        return users.size();
    }

}