package com.example.chatapp.Re_Sign;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.Chat.Chats;
import com.example.chatapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private EditText mname, emailedit, passedit, mphone;
    private FirebaseFirestore db;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;

    private static final String TAG = "RegisterActivity";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_egister);

        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressBar);
        db = FirebaseFirestore.getInstance();

        emailedit = findViewById(R.id.email);
        mname = findViewById(R.id.fullname);
        passedit = findViewById(R.id.password);
        mphone = findViewById(R.id.phone);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                register();
            }
        });
    }

    private void register() {
        String email = emailedit.getText().toString();
        String pass = passedit.getText().toString();
        String name = mname.getText().toString();
        String phone = mphone.getText().toString();

        // Kiểm tra các trường nhập liệu
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng nhập email!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hiển thị ProgressBar
        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                // Ẩn ProgressBar
                progressBar.setVisibility(View.GONE);

                if (task.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "Tạo tài khoản thành công", Toast.LENGTH_SHORT).show();
                    saveUserToFirestore(name, email, phone);
                } else {
                    // Ghi lại thông báo lỗi
                    Log.e(TAG, "Đăng ký thất bại: " + task.getException().getMessage());
                    Toast.makeText(getApplicationContext(), "Tạo tài khoản không thành công: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveUserToFirestore(String name, String email, String phone) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(getApplicationContext(), "Không thể xác định người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo một Map để lưu thông tin người dùng
        HashMap<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);

        // Thêm dữ liệu người dùng vào Firestore
        db.collection("users").document(userId).set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Thông tin người dùng đã được lưu", Toast.LENGTH_SHORT).show();
                            // Chuyển đến MainActivity sau khi lưu thành công
                            Intent intent = new Intent(RegisterActivity.this, Chats.class);
                            startActivity(intent);
                            finish(); // Kết thúc hoạt động này
                        } else {
                            Log.e(TAG, "Không thể lưu thông tin người dùng: " + task.getException().getMessage());
                            Toast.makeText(getApplicationContext(), "Không thể lưu thông tin người dùng: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
