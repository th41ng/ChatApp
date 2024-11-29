package com.example.chatapp;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DatabaseConfig {
    private FirebaseFirestore db;

    public void FirestoreManager() {
        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();
    }
    // Hàm lưu người dùng vào Firestore
    public void saveUser(String uid, String userName, String email, String numberPhone,
                         OnSuccessListener<Void> successListener,
                         OnFailureListener failureListener) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", userName);
        user.put("email", email);
        user.put("number_phone", numberPhone);

        // Lưu thông tin người dùng vào Firestore
        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }
}
