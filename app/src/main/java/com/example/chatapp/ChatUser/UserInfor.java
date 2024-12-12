package com.example.chatapp.ChatUser;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserInfor extends AppCompatActivity {
    private View viewStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friend_activity_user_infor);

        EditText fullname = findViewById(R.id.fullname);
        EditText email = findViewById(R.id.email);
        EditText phone = findViewById(R.id.phone);
        ImageView imageButton = findViewById(R.id.image);
        ImageView imageBack = findViewById(R.id.btnBack);
        viewStatus=findViewById(R.id.status);

        // Lấy thông tin từ Intent
        String name = getIntent().getStringExtra("name");
        String image = getIntent().getStringExtra("image");
        String emailUser = getIntent().getStringExtra("email");
        String phoneNumber = getIntent().getStringExtra("phone");
        String userId = getIntent().getStringExtra("userId");

        capNhatTrangThaiUser(userId);

        // Hiển thị thông tin từ Intent
        if (name != null) fullname.setText(name);
        if (phoneNumber != null) phone.setText(phoneNumber);
        if (emailUser != null) email.setText(emailUser);
        if (image != null) {
            Glide.with(this)
                    .load(image)
                    .placeholder(R.drawable.default_avatar)
                    .into(imageButton);
        } else {
            imageButton.setImageResource(R.drawable.default_avatar);
        }

        imageBack.setOnClickListener(view-> getOnBackPressedDispatcher().onBackPressed());

    }
    private void capNhatTrangThaiUser(String userID){
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(userID).child("status");

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String status = dataSnapshot.getValue(String.class);
                // Xử lý trạng thái người dùng
                if (status!=null && status.equals("online")) {
                    viewStatus.setBackgroundResource(R.drawable.status_online_circle);// Hình tròn xanh
                } else {
                    viewStatus.setBackgroundResource(R.drawable.status_offline_circle); // Hình tròn xám
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error: " + databaseError.getMessage());
            }
        });
    }
}
