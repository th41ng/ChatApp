package com.example.chatapp.ChatUser;

import models.AdminRequest;

public interface AdminListener {
    void onUserClicked(AdminRequest adminRequest);
    void onBtnAgree(AdminRequest adminRequest);
    void onBtnRemove(AdminRequest adminRequest);
}
