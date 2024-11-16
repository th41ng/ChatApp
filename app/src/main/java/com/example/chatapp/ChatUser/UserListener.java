package com.example.chatapp.ChatUser;

import models.User;

public interface UserListener {
    void onUserClicked(User user);
    void onBtnAddFriend(User user);
    void onBtnRemoveFriend(User user);
}
