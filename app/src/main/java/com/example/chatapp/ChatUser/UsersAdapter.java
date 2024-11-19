package com.example.chatapp.ChatUser;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.Chat.Chats;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.net.Authenticator;
import java.util.List;

import models.User;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
    private final List<User> users;
    private final UserListener userListener;
    TextView textName;
    TextView textEmail;
    ImageView imageProfile;
    private ConstraintLayout lastChatView;
    private Button btnAddFriend;
    public UsersAdapter(List<User> users, UserListener userListener) {
        this.users = users;
        this.userListener = userListener;
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        UserViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textEmail = itemView.findViewById(R.id.textEmail);
            imageProfile = itemView.findViewById(R.id.imageProfile);
            btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
            lastChatView = itemView.findViewById(R.id.lastChatView);
        }

        void setUserData(User user) {
            textName.setText(user.name);
            textEmail.setText(user.email);
            imageProfile.setImageBitmap(getUserImage(user.image));

            updateButtonState(user.isFriendRequestSent(), btnAddFriend);


            textName.setOnClickListener(v -> userListener.onUserClicked(user));


            // Handle Add/Remove Friend button click
            btnAddFriend.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    return; // Exit if position is invalid
                }
                User currentUser = users.get(position);
                if (currentUser.isFriendRequestSent()) {
                    userListener.onBtnRemoveFriend(currentUser);
                    currentUser.setFriendRequestSent(false); // Update state
                    updateButtonState(false, btnAddFriend);
                } else {
                    userListener.onBtnAddFriend(currentUser);
                    currentUser.setFriendRequestSent(true); // Update state
                    updateButtonState(true, btnAddFriend);
                }
                notifyItemChanged(position);
            });
        }
    }


    private void updateButtonState(boolean isFriendRequestSent, Button btnAddFriend) {
        if (isFriendRequestSent) {
            btnAddFriend.setText("Hủy kết bạn");
            btnAddFriend.setBackgroundColor(ContextCompat.getColor(btnAddFriend.getContext(), R.color.red));
        } else {
            btnAddFriend.setText("Kết bạn");
            btnAddFriend.setBackgroundColor(ContextCompat.getColor(btnAddFriend.getContext(), R.color.primary));
        }
    }

    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @NonNull
    @Override
    public UsersAdapter.UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_container_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersAdapter.UserViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }


}