package com.example.chatapp.ChatUser;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FriendRequestManager {
    private final FirebaseDatabase firebaseDatabase;
    private final String currentUserId;

    public FriendRequestManager(String currentUserId) {
        this.firebaseDatabase = FirebaseDatabase.getInstance();
        this.currentUserId = currentUserId;
    }

    public void checkFriendRequestStatus(String targetUserId, OnFriendRequestStatusListener listener) {
        DatabaseReference requestRef = firebaseDatabase.getReference("friend_requests")
                .child(currentUserId).child("sent").child(targetUserId);

        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    listener.onRequestSent(true);
                } else {
                    listener.onRequestSent(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }

    public void sendFriendRequest(String targetUserId, OnFriendRequestActionListener listener) {
        DatabaseReference sentRef = firebaseDatabase.getReference("friend_requests")
                .child(currentUserId).child("sent").child(targetUserId);
        DatabaseReference receivedRef = firebaseDatabase.getReference("friend_requests")
                .child(targetUserId).child("received").child(currentUserId);

        sentRef.setValue("sent").addOnSuccessListener(aVoid -> {
            receivedRef.setValue("received").addOnSuccessListener(aVoid1 -> {
                listener.onRequestSuccess();
            }).addOnFailureListener(e -> listener.onRequestFailure(e.getMessage()));
        }).addOnFailureListener(e -> listener.onRequestFailure(e.getMessage()));
    }

    public void cancelFriendRequest(String targetUserId, OnFriendRequestActionListener listener) {
        DatabaseReference sentRef = firebaseDatabase.getReference("friend_requests")
                .child(currentUserId).child("sent").child(targetUserId);
        DatabaseReference receivedRef = firebaseDatabase.getReference("friend_requests")
                .child(targetUserId).child("received").child(currentUserId);

        sentRef.removeValue().addOnSuccessListener(aVoid -> {
            receivedRef.removeValue().addOnSuccessListener(aVoid1 -> {
                listener.onRequestSuccess();
            }).addOnFailureListener(e -> listener.onRequestFailure(e.getMessage()));
        }).addOnFailureListener(e -> listener.onRequestFailure(e.getMessage()));
    }

    public interface OnFriendRequestStatusListener {
        void onRequestSent(boolean isSent);

        void onError(String message);
    }

    public interface OnFriendRequestActionListener {
        void onRequestSuccess();

        void onRequestFailure(String message);
    }
}

