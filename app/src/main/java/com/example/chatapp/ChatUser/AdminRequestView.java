package com.example.chatapp.ChatUser;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import models.User;
import models.AdminRequest;

public class AdminRequestView extends AppCompatActivity implements AdminListener {

    private ProgressBar progressBar;
    private RecyclerView usersRecyclerView;
    private ImageView imageBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friend_activity_admin_request);

        progressBar = findViewById(R.id.progressBar);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        imageBack = findViewById(R.id.btnBack);

        setListeners();
        getInforRequest();

    }

    private void setListeners() {
        imageBack.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void showErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccessMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void getInforRequest() {
        loading(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("managerChangeRequests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<AdminRequest> changeRequests = new ArrayList<>();
                        List<Task<Void>> tasks = new ArrayList<>();  // To manage asynchronous user fetch tasks

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String userId = document.getString("requestedBy");
                            String userChangeId = document.getString("requestedTo");
                            String groupId = document.getString("chatRoomId");

                            // Fetch user data asynchronously
                            Task<User> userTask = getUserById(userId);
                            Task<User> userChangeTask = getUserById(userChangeId);

                            tasks.add(userTask.continueWithTask(task -> {
                                if (task.isSuccessful()) {
                                    User user = task.getResult();
                                    String name = user.getName();
                                    AdminRequest request = new AdminRequest();
                                    request.setName(name);
                                    request.setUserId(userId);
                                    request.setUserChangeId(userChangeId);
                                    request.setGroupName(groupId);

                                    // Handling the second task for userChangeId
                                    return userChangeTask.continueWithTask(changeTask -> {
                                        if (changeTask.isSuccessful()) {
                                            User changeUser = changeTask.getResult();
                                            request.setNameChange(changeUser.getName());
                                            changeRequests.add(request);
                                        }
                                        return null;  // Continue with null, as we don't need to return anything
                                    });
                                }
                                return null;
                            }));
                        }

                        // Wait for all tasks to complete
                        Tasks.whenAllComplete(tasks).addOnCompleteListener(allTask -> {
                            loading(false);
                            if (!changeRequests.isEmpty()) {
                                AdminRequestAdapter adminRequestAdapter = new AdminRequestAdapter(changeRequests, AdminRequestView.this);
                                usersRecyclerView.setLayoutManager(new LinearLayoutManager(AdminRequestView.this));
                                usersRecyclerView.setAdapter(adminRequestAdapter);
                                usersRecyclerView.setVisibility(View.VISIBLE);
                            }
                        });
                    } else {
                        loading(false);
                        showErrorMessage("Không có yêu cầu chuyển nào");
                    }
                })
                .addOnFailureListener(e -> {
                    loading(false);
                    showErrorMessage("Lỗi khi lấy danh sách yêu cầu chuyển");
                    Log.e("Firestore", "Error fetching change requests", e);
                });
    }

    private Task<User> getUserById(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId);
        TaskCompletionSource<User> taskCompletionSource = new TaskCompletionSource<>();

        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setUserId(documentSnapshot.getId());
                            taskCompletionSource.setResult(user);  // Set the result of the task
                        } else {
                            taskCompletionSource.setException(new Exception("User data is null"));
                        }
                    } else {
                        taskCompletionSource.setException(new Exception("User does not exist"));
                    }
                })
                .addOnFailureListener(e -> taskCompletionSource.setException(e));  // In case of failure

        return taskCompletionSource.getTask();  // Return the Task
    }

    @Override
    public void onUserClicked(models.AdminRequest adminRequest) {

    }

    @Override
    public void onBtnAgree(models.AdminRequest adminRequest) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference chatRoomRef = database.getReference("chatRooms").child(adminRequest.getGroupName());
        // Step 1: Update chatRoomOwner in Realtime Database
        chatRoomRef.child("chatRoomOwner").setValue(adminRequest.getUserChangeId())
                .addOnSuccessListener(aVoid -> {
                    Log.d("RealtimeDB", "Chat room owner updated successfully");

                    // Step 2: Remove the changeId field in Firestore
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("chatRooms").document(adminRequest.getGroupName())
                            .update("changeId", null)
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d("Firestore1", "Field changeId removed");

                                // Step 3: Delete the request from Firestore's managerChangeRequests
                                db.collection("managerChangeRequests")
                                        .whereEqualTo("chatRoomId", adminRequest.getGroupName())
                                        .get()
                                        .addOnSuccessListener(queryDocumentSnapshots -> {
                                            if (!queryDocumentSnapshots.isEmpty()) {
                                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                                    db.collection("managerChangeRequests").document(document.getId())
                                                            .delete()
                                                            .addOnSuccessListener(aVoid2 -> {
                                                                Toast.makeText(getApplicationContext(), "Yêu cầu đã được xóa", Toast.LENGTH_SHORT).show();
                                                                Log.d("Firestore1", "Request deleted");
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Toast.makeText(getApplicationContext(), "Lỗi khi xóa yêu cầu", Toast.LENGTH_SHORT).show();
                                                                Log.e("Firestore1", "Error deleting request", e);
                                                            });
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getApplicationContext(), "Lỗi khi truy vấn yêu cầu", Toast.LENGTH_SHORT).show();
                                            Log.e("Firestore1", "Error querying requests", e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getApplicationContext(), "Lỗi khi cập nhật chatRoomOwner", Toast.LENGTH_SHORT).show();
                                Log.e("Firestore1", "Error updating chatRoomOwner", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getApplicationContext(), "Lỗi khi cập nhật chatRoomOwner", Toast.LENGTH_SHORT).show();
                    Log.e("RealtimeDB", "Error updating chatRoomOwner", e);
                });
    }

    @Override
    public void onBtnRemove(models.AdminRequest adminRequest) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference chatRoomRef = db.collection("chatRooms").document(adminRequest.getGroupName());
        // Bước 1: Xóa trường changeId trong tài liệu chatrooms
        chatRoomRef.update("changeId", FieldValue.delete())
                .addOnSuccessListener(aVoid1 -> {
                    Log.d("Firestore", "Field changeId đã được xóa");

                    // Bước 2: Xóa tài liệu yêu cầu từ "managerChangeRequests"
                    // Truy vấn để tìm tài liệu có chatRoomId khớp
                    db.collection("managerChangeRequests")
                            .whereEqualTo("chatRoomId", adminRequest.getGroupName())
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        // Xóa tài liệu khớp
                                        db.collection("managerChangeRequests").document(document.getId())
                                                .delete()
                                                .addOnSuccessListener(e -> {
                                                    Toast.makeText(getApplicationContext(), "Yêu cầu đã được xóa", Toast.LENGTH_SHORT).show();
                                                    Log.d("Firestore", "Yêu cầu đã bị xóa");
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(getApplicationContext(), "Lỗi khi xóa yêu cầu", Toast.LENGTH_SHORT).show();
                                                    Log.e("Firestore", "Lỗi khi xóa yêu cầu", e);
                                                });
                                    }
                                } else {
                                    Toast.makeText(getApplicationContext(), "Không tìm thấy yêu cầu nào khớp", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getApplicationContext(), "Lỗi khi truy vấn yêu cầu", Toast.LENGTH_SHORT).show();
                                Log.e("Firestore", "Lỗi khi truy vấn yêu cầu", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getApplicationContext(), "Lỗi khi cập nhật managerId", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Lỗi khi cập nhật managerId", e);
                });
    }
}