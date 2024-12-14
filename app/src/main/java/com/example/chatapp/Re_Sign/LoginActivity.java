package com.example.chatapp.Re_Sign;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import android.widget.TextView;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.ChatUser.AdminMain;
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
    private static final String[] ADMIN_EMAILS = {"admin1@gmail.com", "admin2@gmail.com"};

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
        passedit.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Kiểm tra xem người dùng có chạm vào biểu tượng bên phải không
                    if (event.getRawX() >= (passedit.getRight() - passedit.getCompoundDrawables()[2].getBounds().width())) {
                        // Toggle ẩn/hiện mật khẩu
                        if (passedit.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                            passedit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                            passedit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_lock_24, 0, R.drawable.anmatkhau, 0);
                        } else {
                            passedit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            passedit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_lock_24, 0, R.drawable.hienmatkhau, 0);
                        }
                        passedit.setSelection(passedit.getText().length()); // Giữ con trỏ ở cuối
                        return true;
                    }
                }
                return false;
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


                    // Lấy FirebaseUser hiện tại
                    FirebaseUser user = mAuth.getCurrentUser();

                    if (user != null) {
                        String userId = user.getUid();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        // Kiểm tra nếu là admin
                        db.collection("admins").document(userId).get()
                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task1) {
                                        if (task1.isSuccessful() && task1.getResult() != null) {
                                            DocumentSnapshot adminSnapshot = task1.getResult();
                                            if (adminSnapshot.exists()) {
                                                // Nếu tồn tại trong collection "admins"
                                                Log.d(TAG, "Đăng nhập với vai trò Admin");
                                                Intent adminIntent = new Intent(LoginActivity.this, AdminMain.class);
                                                startActivity(adminIntent);
                                                finish();
                                            } else {
                                                // Nếu không phải admin, tìm trong collection "users"
                                                db.collection("users").document(userId).get()
                                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task2) {
                                                                if (task2.isSuccessful() && task2.getResult() != null) {
                                                                    DocumentSnapshot userSnapshot = task2.getResult();
                                                                    if (userSnapshot.exists()) {
                                                                        // ** Kiểm tra trạng thái vô hiệu hóa **
                                                                        Boolean disabled = userSnapshot.getBoolean("disabled");
                                                                        if (Boolean.TRUE.equals(disabled)) {
                                                                            Toast.makeText(getApplicationContext(), "Tài khoản của bạn đã bị vô hiệu hóa!", Toast.LENGTH_SHORT).show();
                                                                        } else {

                                                                            Log.d(TAG, "Đăng nhập với vai trò User");

                                                                            String name = userSnapshot.getString("name");
                                                                            String phone = userSnapshot.getString("phone");
                                                                            String image = userSnapshot.getString("image");
                                                                            String email = userSnapshot.getString("email");
                                                                            Toast.makeText(getApplicationContext(), "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

                                                                            rememberUser(email, pass, chkRememberPass.isChecked());
                                                                            Intent intent = new Intent(LoginActivity.this, ChatUserMain.class);
                                                                            intent.putExtra("userId", userId);
                                                                            intent.putExtra("name", name);
                                                                            intent.putExtra("phone", phone);
                                                                            intent.putExtra("image", image);
                                                                            intent.putExtra("email", email);
                                                                            startActivity(intent);
                                                                            finish();
                                                                        }
                                                                    } else {
                                                                        Toast.makeText(getApplicationContext(), "Không tìm thấy thông tin người dùng!", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                } else {
                                                                    Log.e(TAG, "Lỗi khi truy vấn người dùng: ", task2.getException());
                                                                    Toast.makeText(getApplicationContext(), "Lỗi khi truy vấn người dùng!", Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });
                                            }
                                        } else {
                                            Log.e(TAG, "Lỗi khi truy vấn admin: ", task1.getException());
                                            Toast.makeText(getApplicationContext(), "Lỗi khi truy vấn admin!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    } else {
                        Log.e(TAG, "Người dùng chưa đăng nhập!");
                        Toast.makeText(getApplicationContext(), "Người dùng chưa đăng nhập. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Đăng nhập không thành công!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private boolean isAdmin(String email) {
        for (String adminEmail : ADMIN_EMAILS) {
            if (adminEmail.equals(email)) {
                return true;
            }
        }
        return false;
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
