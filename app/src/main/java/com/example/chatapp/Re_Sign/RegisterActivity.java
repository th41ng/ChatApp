package com.example.chatapp.Re_Sign;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
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
    private EditText confirmPassEdit;
    private TextView tvLogin;
    private FirebaseFirestore db;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    // private ProgressBar progressBar;

    private static final String TAG = "RegisterActivity";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_egister);

        mAuth = FirebaseAuth.getInstance();
        //progressBar = findViewById(R.id.progressBar);
        db = FirebaseFirestore.getInstance();

        emailedit = findViewById(R.id.email);
        mname = findViewById(R.id.fullname);
        passedit = findViewById(R.id.password);
        confirmPassEdit = findViewById(R.id.confirmpass);
        mphone = findViewById(R.id.phone);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.haveAC);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                register();
            }
        });
        tvLogin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // Chuyển đến màn hình đăng nhập
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Kết thúc hoạt động này để không quay lại trang đăng ký
            }
        });

        // Xử lý sự kiện chạm để ẩn/hiện mật khẩu
        passedit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (passedit.getRight() - passedit.getCompoundDrawables()[2].getBounds().width())) {
                        if (passedit.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                            passedit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                            passedit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_lock_24, 0, R.drawable.anmatkhau, 0);
                        } else {
                            passedit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            passedit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_lock_24, 0, R.drawable.hienmatkhau, 0);
                        }
                        passedit.setSelection(passedit.getText().length());
                        return true;
                    }
                }
                return false;
            }
        });

        confirmPassEdit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (confirmPassEdit.getRight() - confirmPassEdit.getCompoundDrawables()[2].getBounds().width())) {
                        if (confirmPassEdit.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                            confirmPassEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                            confirmPassEdit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_lock_24, 0, R.drawable.anmatkhau, 0);
                        } else {
                            confirmPassEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            confirmPassEdit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_lock_24, 0, R.drawable.hienmatkhau, 0);
                        }
                        confirmPassEdit.setSelection(confirmPassEdit.getText().length());
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void register() {
        String email = emailedit.getText().toString();
        String pass = passedit.getText().toString();
        String confirmPass = confirmPassEdit.getText().toString();
        String name = mname.getText().toString();
        String phone = mphone.getText().toString();

        // Kiểm tra các trường nhập liệu
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng nhập email!", Toast.LENGTH_SHORT).show();
            return;
        }
        // Kiểm tra xem email có phải là của admin không
        if (email.equalsIgnoreCase("admin1@gmail.com") || email.equalsIgnoreCase("admin2@gmail.com")) {
            Toast.makeText(this, "Email này không thể đăng ký.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(confirmPass)) {
            Toast.makeText(this, "Vui lòng nhập lại mật khẩu!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }


        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                // Ẩn ProgressBar
                //progressBar.setVisibility(View.GONE);

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
        user.put("disabled", false);

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
