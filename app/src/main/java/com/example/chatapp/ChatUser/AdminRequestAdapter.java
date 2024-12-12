package com.example.chatapp.ChatUser;

import android.annotation.SuppressLint;
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

import models.AdminRequest;

public class AdminRequestAdapter extends RecyclerView.Adapter<AdminRequestAdapter.AdminRequestViewHolder> {

    private final List<AdminRequest> adminRequestList;
    private final AdminListener adminListener;

    public AdminRequestAdapter(List<AdminRequest> adminRequestList, AdminListener adminListener) {
        this.adminRequestList = adminRequestList;
        this.adminListener = adminListener;
    }

    public static class AdminRequestViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textNameChange;
        TextView textGroup;
        ImageView imageProfile;
        Button btnAgree,btnReject;

        public AdminRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textNameChange = itemView.findViewById(R.id.textNameChange);
            textGroup=itemView.findViewById(R.id.textGroup);
            imageProfile = itemView.findViewById(R.id.imageProfile);
            btnAgree = itemView.findViewById(R.id.btnAgree);
            btnReject=itemView.findViewById(R.id.btnReject);
        }
    }
    @NonNull
    @Override
    public AdminRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_item_cointainer_admin_request, parent, false);
        return new AdminRequestAdapter.AdminRequestViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull AdminRequestViewHolder holder, int position) {
        AdminRequest adminRequest = adminRequestList.get(position);

        // Kiểm tra nếu các trường không null
        String userName = adminRequest.getName();
        String changeUserName = adminRequest.getNameChange();
        String groupName = adminRequest.getGroupName();

        // Cập nhật thông tin UI
        if (userName != null) {
            holder.textName.setText(userName + "(Manager)");
        } else {
            holder.textName.setText("(Manager)"); // Nếu không có tên, hiển thị "(Manager)"
        }

        if (changeUserName != null) {
            holder.textNameChange.setText(changeUserName + "(Changed)");
        } else {
            holder.textNameChange.setText("(Changed)"); // Nếu không có tên, hiển thị "(Changed)"
        }

        if (groupName != null) {
            holder.textGroup.setText(groupName);
        } else {
            holder.textGroup.setText("No Group"); // Nếu không có nhóm, hiển thị "No Group"
        }

        // Tải ảnh từ Firestore và hiển thị
        setImage(holder.imageProfile, adminRequest.getUserId(), holder.itemView.getContext());

        // Khi nhấn nút đồng ý
        holder.btnAgree.setOnClickListener(view -> {
            adminListener.onBtnAgree(adminRequest);
            holder.btnAgree.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
        });

        // Khi nhấn nút từ chối
        holder.btnReject.setOnClickListener(view -> {
            adminListener.onBtnRemove(adminRequest);
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
        return adminRequestList.size();
    }


}
