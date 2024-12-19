package com.example.chatapp.ChatUser;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.chatapp.CloudinaryManager;
import com.example.chatapp.R;
import com.example.chatapp.Re_Sign.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class ChangeProfile extends AppCompatActivity {
    private EditText fullname,email,phone,password,confirmNewPass,confirmpass;
    private Button btnSave;
    private ImageView imageButton;
    private ImageView imageBack;
    private FirebaseAuth mAuth;
    // Khai báo ActivityResultLauncher
    private ActivityResultLauncher<Intent> pickImageLauncher;
    // Static flag to check if MediaManager is initialized
    private static boolean isMediaManagerInitialized = false;
    // Biến lưu trữ URI của ảnh được chọn
    private Uri selectedImageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friend_activity_change_profile);

        // Khởi tạo FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        fullname = findViewById(R.id.fullname);
        email = findViewById(R.id.email);
        phone = findViewById(R.id.phone);
        imageButton = findViewById(R.id.imageButton);
        password = findViewById(R.id.password); // Mật khẩu cũ
        confirmNewPass = findViewById(R.id.confirmNewPass); // Mật khẩu mới
        confirmpass = findViewById(R.id.confirmpass);
        btnSave=findViewById(R.id.btnSave);
        imageBack = findViewById(R.id.btnBack);

        // Lấy thông tin từ Intent
        String name = getIntent().getStringExtra("name");
        String image = getIntent().getStringExtra("image");
        String emailUser = getIntent().getStringExtra("email");
        String phoneNumber = getIntent().getStringExtra("phone");
        String userId = getIntent().getStringExtra("userId");

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

        // Nếu userId có sẵn, tải thêm thông tin từ Firebase nếu cần
        if (userId != null && !userId.isEmpty()) {
            loadUserInfo(userId);
        } else {
            Toast.makeText(this, "Không có userId, chỉ hiển thị thông tin từ Intent.", Toast.LENGTH_SHORT).show();
        }

        imageBack.setOnClickListener(view-> getOnBackPressedDispatcher().onBackPressed());

        pickImageLauncher();
        imageButton.setOnClickListener(view->openImagePicker());

        // Gắn sự kiện cho nút "Lưu"
        btnSave.setOnClickListener(view -> {
            String oldPassword = password.getText().toString().trim();
            String newPassword = confirmNewPass.getText().toString().trim();
            String confirmPassword = confirmpass.getText().toString().trim();
            String updatedName = fullname.getText().toString().trim();
            String updatedPhone = phone.getText().toString().trim();

            if (updatedName.isEmpty() || updatedPhone.isEmpty() ) {
                Toast.makeText(ChangeProfile.this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.isEmpty() && !newPassword.equals(confirmPassword)) {
                Toast.makeText(ChangeProfile.this, "Mật khẩu mới không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cập nhật mật khẩu nếu thay đổi
            if (!oldPassword.isEmpty() && !newPassword.isEmpty()) {
                updatePasswordWithAuth(emailUser, oldPassword, newPassword, updatedName, updatedPhone);
            } else {
                // Nếu có ảnh đại diện mới, tải lên Cloudinary
                if (selectedImageUri != null) { // Kiểm tra xem người dùng đã chọn ảnh chưa
                    uploadImageToCloudinary(selectedImageUri, updatedName, updatedPhone);
                } else {
                    // Nếu không thay đổi ảnh, chỉ cập nhật thông tin người dùng
                    updateUserInfo(userId, updatedName, updatedPhone);
                }
            }
        });

        setListeners();
        CloudinaryManager.initialize(this);
    }
    private void setListeners(){
        imageBack.setOnClickListener(view-> getOnBackPressedDispatcher().onBackPressed());
    }
    // Lưu URI của ảnh khi người dùng chọn ảnh
    private void pickImageLauncher() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this)
                                    .load(selectedImageUri)
                                    .into(imageButton);
                        }
                    }
                }
        );
    }
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }
    // Hàm upload ảnh lên Cloudinary và cập nhật URL ảnh vào Firestore
    private void uploadImageToCloudinary(Uri imageUri, String updatedName, String updatedPhone ) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int bytesRead;
            byte[] data = new byte[1024];
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            byte[] imageData = buffer.toByteArray();

            MediaManager.get().upload(imageData)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Toast.makeText(ChangeProfile.this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = (String) resultData.get("secure_url");
                            Toast.makeText(ChangeProfile.this, "Tải ảnh thành công!", Toast.LENGTH_SHORT).show();
                            // Cập nhật URL ảnh đại diện vào Firestore
                            updateProfileImageUrl(imageUrl, updatedName, updatedPhone);
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Log.e("CloudinaryError", "Lỗi tải ảnh: " + error.getDescription());
                            Toast.makeText(ChangeProfile.this, "Lỗi tải ảnh: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                        }
                    }).dispatch();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi mở ảnh!", Toast.LENGTH_SHORT).show();
        }
    }

    // Cập nhật URL ảnh vào Firestore
    private void updateProfileImageUrl(String imageUrl, String updatedName, String updatedPhone) {
        String userId = getIntent().getStringExtra("userId");
        if (userId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDoc = db.collection("users").document(userId);

        // Cập nhật ảnh đại diện và thông tin người dùng
        Map<String, Object> updates = new HashMap<>();
        updates.put("image", imageUrl);
        updates.put("name", updatedName);
        updates.put("phone", updatedPhone);

        userDoc.update(updates).addOnCompleteListener(updateTask -> {
            if (updateTask.isSuccessful()) {
                Toast.makeText(this, "Cập nhật ảnh đại diện và thông tin thành công!", Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "Thông tin đã được cập nhật và hãy đăng nhập lại!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Đóng MainActivity
            } else {
                Toast.makeText(this, "Cập nhật ảnh đại diện thất bại!", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadUserInfo(String userId) {
        // Tham chiếu Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("Users").document(userId);

        // Lấy dữ liệu từ Firestore
        userRef.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Lấy dữ liệu thành công
                        Map<String, Object> userData = task.getResult().getData();
                        if (userData != null) {
                            // Cập nhật UI với dữ liệu từ Firestore
                            fullname.setText((String) userData.get("name"));
                            phone.setText((String) userData.get("phone"));
                            email.setText((String) userData.get("email"));

                            // Tải ảnh đại diện nếu có
                            String imageUrl = (String) userData.get("image");
                            if (imageUrl != null) {
                                Glide.with(ChangeProfile.this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.default_avatar)
                                        .into(imageButton);
                            } else {
                                imageButton.setImageResource(R.drawable.default_avatar);
                            }
                        } else {
                            Log.d("ChangProfile", "Không tìm thấy dữ liệu người dùng!");
                        }
                    } else {
                        Toast.makeText(this, "Lỗi khi tải dữ liệu Firestore!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updatePasswordWithAuth(String email, String oldPassword, String newPassword, String updatedName, String updatedPhone) {
        String userId = getIntent().getStringExtra("userId");
        mAuth.signInWithEmailAndPassword(email, oldPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            Toast.makeText(ChangeProfile.this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                                            // Cập nhật thông tin người dùng sau khi đổi mật khẩu
                                            updateUserInfo(userId, updatedName, updatedPhone);
                                        } else {
                                            Toast.makeText(ChangeProfile.this, "Lỗi khi đổi mật khẩu!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(ChangeProfile.this, "Mật khẩu cũ không chính xác!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void updateUserInfo(String userId, String name, String phone) {
        // Tham chiếu Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Dữ liệu cần lưu
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);

        // Tham chiếu tới document của người dùng
        DocumentReference userDoc = db.collection("users").document(userId);

        // Cập nhật dữ liệu
        btnSave.setEnabled(false);
        userDoc.update(updates)
                .addOnCompleteListener(task -> {
                    btnSave.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Thông tin đã được cập nhật và hãy đăng nhập lại!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish(); // Đóng MainActivity
                    } else {
                        Toast.makeText(this, "Cập nhật thất bại!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}