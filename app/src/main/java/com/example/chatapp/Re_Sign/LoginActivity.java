package com.example.chatapp.Re_Sign;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import android.widget.TextView;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.ChatUser.ChatUserMain;
import com.example.chatapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private EditText emailedit, passedit;

    private Button btnLogin;
    private FirebaseAuth mAuth;
    private CheckBox chkRememberPass;
    private TextView btnRegister;

    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_login);

        mAuth = FirebaseAuth.getInstance();
        emailedit = findViewById(R.id.email);
        passedit = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegisterSau);

        chkRememberPass = findViewById(R.id.chk_RememberPass);

        // Khởi tạo SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("USER_FILE.xml", MODE_PRIVATE);

        // Lấy trạng thái checkbox và dữ liệu đăng nhập nếu "Remember me" đã được chọn
        boolean isRemembered = sharedPreferences.getBoolean("REMEMBER", false);
        chkRememberPass.setChecked(isRemembered); // Set trạng thái của checkbox

        if (isRemembered) {
            String savedEmail = sharedPreferences.getString("EMAIL", "");
            String savedPassword = sharedPreferences.getString("PASSWORD", "");
            emailedit.setText(savedEmail);
            passedit.setText(savedPassword);

            // Tự động đăng nhập nếu đã lưu thông tin
            login(savedEmail, savedPassword);
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailedit.getText().toString();
                String pass = passedit.getText().toString();
                login(email, pass);
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                register();
            }
        });
    }

    private void register() {
        Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(i);
    }

    private void login(String email, String pass) {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng nhập email!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Vui lòng nhập password!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                    rememberUser(email, pass, chkRememberPass.isChecked()); // Lưu trạng thái đăng nhập nếu chọn ghi nhớ

                    // Lấy FirebaseUser hiện tại
                    FirebaseUser user = mAuth.getCurrentUser();

                    if (user != null) {
                        // Truy vấn Firestore để lấy thông tin người dùng
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        DocumentReference userRef = db.collection("users").document(user.getUid());

                        userRef.get()
                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task1) {
                                        if (task1.isSuccessful()) {
                                            DocumentSnapshot documentSnapshot = task1.getResult();
                                            if (documentSnapshot.exists()) {
                                                // Lấy thông tin người dùng từ Firestore
                                                String userId = user.getUid();
                                                String name = documentSnapshot.getString("name");
                                                String image = documentSnapshot.getString("image");
                                                String phone = documentSnapshot.getString("phone");
                                                String email = documentSnapshot.getString("email");

                                                // Chuyển dữ liệu sang ChatUserMain qua Intent
                                                Intent intent = new Intent(LoginActivity.this, ChatUserMain.class);
                                                intent.putExtra("userId", userId);
                                                intent.putExtra("name", name);
                                                intent.putExtra("image", image);
                                                intent.putExtra("phone", phone);
                                                intent.putExtra("email", email);

                                                // Chuyển sang ChatUserMain
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Log.d("Firestore", "Không có thông tin người dùng!");
                                                Toast.makeText(getApplicationContext(), "Không tìm thấy thông tin người dùng!", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Log.e("Firestore", "Lỗi trong việc lấy thông tin: ", task1.getException());
                                            Toast.makeText(getApplicationContext(), "Lỗi khi truy vấn dữ liệu!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    } else {
                        Log.e("Auth", "Người dùng chưa đăng nhập!");
                        Toast.makeText(getApplicationContext(), "Người dùng chưa đăng nhập. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
                    }


                } else {
                    Toast.makeText(getApplicationContext(), "Đăng nhập không thành công!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Phương thức lưu thông tin người dùng vào SharedPreferences
    public void rememberUser(String email, String pass, boolean rememberStatus) {
        SharedPreferences sharedPreferences = getSharedPreferences("USER_FILE.xml", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (rememberStatus) {
            editor.putString("EMAIL", email);
            editor.putString("PASSWORD", pass);
            editor.putBoolean("REMEMBER", true); // Lưu trạng thái của checkbox
            editor.putBoolean("isLoggedIn", true); // Đặt trạng thái là đã đăng nhập
            Log.d(TAG, "Remember status saved");
        } else {
            editor.remove("EMAIL"); // Xóa email nếu không chọn ghi nhớ
            editor.remove("PASSWORD"); // Xóa password nếu không chọn ghi nhớ
            editor.putBoolean("REMEMBER", false); // Cập nhật trạng thái của checkbox
            editor.putBoolean("isLoggedIn", false); // Cập nhật trạng thái là chưa đăng nhập
            Log.d(TAG, "Remember status cleared");
        }
        editor.apply(); // Áp dụng các thay đổi
    }

}
