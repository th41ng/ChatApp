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

    TextView textName;
    TextView textEmail;
    ImageView imageProfile;
    private Button btnAgree,btnReject;
    public FriendRequestAdapter(List<User> friendRequestList, UserListener userListener) {
        this.friendRequestList = friendRequestList;
        this.userListener= userListener;
    }

    class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        FriendRequestViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textEmail = itemView.findViewById(R.id.textEmail);
            imageProfile = itemView.findViewById(R.id.imageProfile);
            btnAgree = itemView.findViewById(R.id.btnAgree);
            btnReject=itemView.findViewById(R.id.btnReject);
        }
        void setFriendRequestData(User user){
            textName.setText(user.name);
            textEmail.setText(user.email);
            // imageProfile.setImageBitmap(getUserImage(user.image));
            btnAgree.setOnClickListener(v -> userListener.onBtnAddFriend(user));
        }
    }

        @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_container_friend, parent, false);
        return new FriendRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FriendRequestViewHolder holder, int position) {
        holder.setFriendRequestData(friendRequestList.get(position));
    }

    @Override
    public int getItemCount() {
        return friendRequestList.size();
    }


}
