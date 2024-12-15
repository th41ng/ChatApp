package com.example.chatapp.ChatUser;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.Re_Sign.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import models.User;

public class AdminMain extends AppCompatActivity implements UserListener{
    private RecyclerView usersRecyclerView;
    private EditText searchEditText;
    private TextView txtSoOnl,txtSoUser;
    private ImageView imageRequest;
    ImageButton btnSignout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friend_activity_admin_main);
        usersRecyclerView = findViewById(R.id.user_list);
        searchEditText = findViewById(R.id.search_bar);
        txtSoOnl=findViewById(R.id.txtSoOnl);
        txtSoUser=findViewById(R.id.txtSoUser);
        imageRequest=findViewById(R.id.imageRequest);

        getAllUsers();
        getStatusUser();
        imageRequest.setOnClickListener(view->{
            Intent intent=new Intent(getApplicationContext(), AdminRequestView.class);
            startActivity(intent);
        });
//        usersRecyclerView.setVerticalScrollBarEnabled(true);

        // Lắng nghe khi người dùng nhập vào ô tìm kiếm
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                String query = editable.toString().trim(); // Lấy nội dung đã nhập và xóa khoảng trắng hai bên

                if (query.isEmpty()) {
                    resetSearchResults();
                }
                else searchUsers();
            }
        });
        btnSignout = findViewById(R.id.btnSignout);
        btnSignout.setOnClickListener(view -> signOut());
    }
    private void resetSearchResults() {
        usersRecyclerView.setAdapter(null); // Xóa adapter
        getAllUsers();
    }

    private void getStatusUser() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int onlineCount = 0; // Bộ đếm số người online

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String status = userSnapshot.child("status").getValue(String.class);

                    if ("online".equals(status)) { // Kiểm tra nếu trạng thái là "online"
                        onlineCount++;
                    }
                }

                // Hiển thị số người online (có thể cập nhật giao diện tại đây)
                txtSoOnl.setText("Số người online: " + onlineCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error: " + databaseError.getMessage());
            }
        });

    }

    @SuppressLint("SetTextI18n")
    private void getAllUsers() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        // Lấy tất cả user trong collection "users"
        database.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<User> users = new ArrayList<>();
                        int totalUsers = 0; // Biến này dùng để tính tổng số người dùng hợp lệ

                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            User user = queryDocumentSnapshot.toObject(User.class);
                            user.setUserId(queryDocumentSnapshot.getId());
                            users.add(user); // 🟥 Thay đổi: thêm tất cả người dùng vào danh sách
                            totalUsers++;
                        }

                        // Cập nhật giao diện người dùng
                        txtSoUser.setText("Tổng số user: " + totalUsers); // Cập nhật số lượng người dùng hợp lệ

                        // Hiển thị danh sách người dùng
                        if (!users.isEmpty()) {
                            UsersAdapter usersAdapter = new UsersAdapter(users, this);
                            usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                            usersRecyclerView.setAdapter(usersAdapter);
                            usersRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            Log.d("AdminMain", "Không có người dùng hợp lệ");
                        }
                    } else {
                        Log.d("AdminMain", "Cập nhật người dùng không thành công");
                    }
                });
    }

    private void searchUsers() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String searchQuery = searchEditText.getText().toString().trim();
        List<User> userList = new ArrayList<>();

        database.collection("users")
                .orderBy("name")
                .startAt(searchQuery)
                .endAt(searchQuery + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                User user = document.toObject(User.class);
                                user.setUserId(document.getId());
                                userList.add(user);
                            }

                            UsersAdapter usersAdapter = new UsersAdapter(userList, this);
                            usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                            usersRecyclerView.setAdapter(usersAdapter);
                            usersRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Lỗi tìm kiếm: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onUserClicked(User user) {
        // Tạo một danh sách các lựa chọn
        String[] options = {"Vô hiệu hóa tài khoản","Hủy vô hiệu hóa tài khoản","Thay đổi thông tin tài khoản","Xem thông tin chi tiết", "Hủy"};

        // Hiển thị AlertDialog
        new AlertDialog.Builder(this)
                .setTitle("Chọn thao tác cho " + user.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Vô hiệu hóa tài khoản
                            if (Boolean.FALSE.equals(user.getDisabled())) {
                                // Nếu tài khoản chưa bị vô hiệu hóa, vô hiệu hóa tài khoản
                                disableUser(user);
                            } else {
                                Toast.makeText(this, "Tài khoản đã bị vô hiệu hóa trước đó", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 1: // Hủy vô hiệu hóa tài khoản
                            if (Boolean.TRUE.equals(user.getDisabled())) {
                                // Nếu tài khoản đang bị vô hiệu hóa, kích hoạt lại
                                activateUser(user);
                            } else {
                                Toast.makeText(this, "Tài khoản chưa bị vô hiệu hóa", Toast.LENGTH_SHORT).show();
                            }
                            break;

                        case 2://Thay đổi thông tin tài khoản
                            Intent intent=new Intent(getApplicationContext(), ChangeProfile.class);
                            intent.putExtra("name", user.getName());
                            intent.putExtra("image", user.getImage());
                            intent.putExtra("phone", user.getPhone());
                            intent.putExtra("email", user.getEmail());
                            intent.putExtra("userId",user.getUserId());
                            startActivity(intent);
                            break;
                        case 3: // Xem thông tin chi tiết
                            Intent intent3=new Intent(getApplicationContext(), UserInfor.class);
                            intent3.putExtra("name", user.getName());
                            intent3.putExtra("image", user.getImage());
                            intent3.putExtra("phone", user.getPhone());
                            intent3.putExtra("email", user.getEmail());
                            intent3.putExtra("userId",user.getUserId());
                            startActivity(intent3);
                            break;
                        case 4: // Hủy
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }
    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        // Chuyển về màn hình đăng nhập
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void disableUser(User user) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference userRef = database.collection("users").document(user.getUserId());

        // Vô hiệu hóa tài khoản
        userRef.update("disabled", true)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(AdminMain.this, "Tài khoản đã bị vô hiệu hóa", Toast.LENGTH_SHORT).show();
                    getAllUsers(); // Làm mới danh sách người dùng
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminMain.this, "Lỗi khi vô hiệu hóa tài khoản: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void activateUser(User user) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference userRef = database.collection("users").document(user.getUserId());

        // Kích hoạt lại tài khoản
        userRef.update("disabled", false)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(AdminMain.this, "Tài khoản đã được kích hoạt lại", Toast.LENGTH_SHORT).show();
                    getAllUsers(); // Làm mới danh sách người dùng
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminMain.this, "Lỗi khi kích hoạt lại tài khoản: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    @Override
    public void onBtnAddFriend(User user) {
    }

    @Override
    public void onBtnRemoveFriend(User user) {
    }
}