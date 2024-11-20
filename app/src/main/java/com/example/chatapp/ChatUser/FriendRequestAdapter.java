package com.example.chatapp.ChatUser;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;

import java.util.List;

import models.User;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder> {

    private List<User> friendRequestList;
    private final UserListener userListener;

    public FriendRequestAdapter(List<User> friendRequestList, UserListener userListener) {
        this.friendRequestList = friendRequestList;
        this.userListener= userListener;
    }

    class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textEmail;
        ImageView imageProfile;
        private Button btnAgree,btnReject;
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
        // imageProfile.setImageBitmap(getUserImage(user.image));
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

    @Override
    public int getItemCount() {
        return friendRequestList.size();
    }


}