package com.example.chatapp.Chat;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatapp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

import models.Message;

public class InfoChat extends AppCompatActivity {
    private DatabaseReference messagesRef;
    private List<Message> searchResults = new ArrayList<>();
    private SearchResultsAdapter searchResultsAdapter;
    private String chatRoomId;
    long messageTimestamp;
    private ImageButton imgbtnBack;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity_info);
        // Nhận chatRoomId từ Intent
        chatRoomId = getIntent().getStringExtra("chatRoomId");
        if (chatRoomId == null) {
            Toast.makeText(this, "Chat room ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Khởi tạo Firebase reference cho tin nhắn
        messagesRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(chatRoomId).child("messages");
        // Tìm kiếm tin nhắn
        EditText editTextInput = findViewById(R.id.editTextInputtoFind);
        findViewById(R.id.btnFind).setOnClickListener(v -> {
            String searchTerm = editTextInput.getText().toString().trim();
            if (!searchTerm.isEmpty()) {
                searchMessages(searchTerm);
            }
        });

        imgbtnBack=findViewById(R.id.imgBtnBack);
        imgbtnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });
        // Cài đặt RecyclerView và adapter
//        RecyclerView searchResultsRecyclerView = findViewById(R.id.showFindWordMessage);
//        searchResultsAdapter = new SearchResultsAdapter(searchResults);
//        searchResultsRecyclerView.setAdapter(searchResultsAdapter);
//
        // Khởi tạo RecyclerView và adapter
        RecyclerView searchResultsRecyclerView = findViewById(R.id.showFindWordMessage);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));  // Thiết lập LayoutManager
        searchResultsAdapter = new SearchResultsAdapter(searchResults);
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);  // Gán adapter
        // Xử lý khi người dùng chọn tin nhắn
        searchResultsAdapter.setOnItemClickListener(message -> {
            // Inside your onItemClickListener or wherever you set the timestamp
            messageTimestamp = message.getTimestamp();
            Log.d("InfoChat", "Sending timestamp: " + messageTimestamp);
            Intent intent = new Intent(InfoChat.this, Chats.class);
            intent.putExtra("messageTimestamp", messageTimestamp);
            intent.putExtra("friendId", getIntent().getStringExtra("friendId"));
            intent.putExtra("friendName", getIntent().getStringExtra("friendName"));// Truyền friendId
            startActivity(intent);
        });
    }
    private void searchMessages(String searchTerm) {
        messagesRef.orderByChild("content")
                .startAt(searchTerm)  // startAt for search term
                .endAt(searchTerm + "\uf8ff")  // endAt for search term (matches the full string)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        searchResults.clear();  // Clear any previous search results
                        if (dataSnapshot.exists()) {
                            // Iterate over all messages and add them to the search results
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                Message message = snapshot.getValue(Message.class);
                                if (message != null) {
                                    // Log message content to verify it's correct
                                    Log.d("SearchResults", "Message found: " + message.getContent());
                                    // Add matching message to the list
                                    searchResults.add(message);
                                }
                            }
                            // Notify adapter to update the UI with the new search results
                            Log.d("SearchResults", "Number of results: " + searchResults.size());
                            searchResultsAdapter.notifyDataSetChanged();
                            // Show toast based on the results
                            if (searchResults.isEmpty()) {
                                Toast.makeText(InfoChat.this, "No results found", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(InfoChat.this, "Results found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(InfoChat.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}