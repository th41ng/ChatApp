package com.example.chatapp.Chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatapp.R;
import java.util.ArrayList;
import java.util.List;

public class Chats extends AppCompatActivity {

    private EditText editTextMessage;
    private Button buttonSend;
    private RecyclerView recyclerViewMessages;
    private MessageAdapter messageAdapter;
    private List<String> messages;
    private  Button chooseImage;
    private static final int IMAGE_PICK_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats); // Tên layout XML của bạn

        // Khởi tạo các view
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        editTextMessage = findViewById(R.id.messageInput);
        buttonSend = findViewById(R.id.sendButton);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        chooseImage=findViewById(R.id.chooseImg);

        // Khởi tạo danh sách tin nhắn
        messages = new ArrayList<>();
        messages.add("Chào bạn!");
        messages.add("Bạn có khỏe không?");
        messages.add("Mình đang học lập trình Android.");
        messages.add("Hôm nay thời tiết thật đẹp.");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");
        messages.add("Bạn có muốn đi dạo không?");



        // Cấu hình RecyclerView
        messageAdapter = new MessageAdapter(this, messages);
        recyclerViewMessages.setAdapter(messageAdapter);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));

        // Thêm sự kiện click cho nút gửi
        buttonSend.setOnClickListener(v -> sendMessage());

        chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, IMAGE_PICK_CODE);

            }
        });
    }



    private void sendMessage() {
        String message = editTextMessage.getText().toString().trim();
        if (!message.isEmpty()) {
            messages.add(message);
            messageAdapter.notifyItemInserted(messages.size() - 1);
            recyclerViewMessages.scrollToPosition(messages.size() - 1); // Cuộn xuống tin nhắn mới
            editTextMessage.setText(""); // Xóa ô nhập sau khi gửi
        }
    }
}
