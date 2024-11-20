package com.example.chatapp.ChatUser;


import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;

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

        public UserViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textEmail = itemView.findViewById(R.id.textEmail);
            btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
        }
    }
    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_container_search_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.textName.setText(user.getName());
        holder.textEmail.setText(user.getEmail());

        holder.itemView.setOnClickListener(view->{userListener.onUserClicked(user);});

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

    @Override
    public int getItemCount() {
        return userList.size();
    }

}



