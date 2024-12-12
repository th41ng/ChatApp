package com.example.chatapp;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserHelper {

    // Phương thức lấy thông tin người dùng từ userId
    public interface OnUserInfoFetched {
        void onUserInfoFetched(String name, String email, String imageUrl);
        void onFailure(String errorMessage);
    }

    // Lấy thông tin người dùng từ Firestore
    public static void getUserInfo(String userId, final OnUserInfoFetched callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId); // userId là ID của người dùng

        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Lấy thông tin từ document
                        String name = document.getString("name");
                        String email = document.getString("email");
                        String imageUrl = document.getString("image");
                        String phone = document.getString("phone");
                        // Gọi callback để trả về dữ liệu
                        callback.onUserInfoFetched(name, email, imageUrl);
                    } else {
                        Log.d("Firestore", "No such document!");
                        callback.onFailure("No such document");
                    }
                } else {
                    Log.d("Firestore", "Failed to get document: ", task.getException());
                    callback.onFailure(task.getException().getMessage());
                }
            }
        });
    }
}