package com.example.chatapp.Chat;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import models.Message;
import models.User;

public class LastMessageAdapter extends RecyclerView.Adapter<LastMessageAdapter.ViewHolder> {
    private final Context context;
    private final List<Message> messageList;
    private ConstraintLayout lastChatView;

    public LastMessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;

    }

    @NonNull
    @Override
    public LastMessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_item_lastmessage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messageList.get(position);

        // Tải ảnh từ Firestore và hiển thị
        setImage(holder.avatarImageView, message.getFriendId(), holder.itemView.getContext());

        // Set the friend's name
        String friendName = message.getFriendName();
        if (friendName != null) {
            holder.userNameTextView.setText(friendName);
        } else {
            holder.userNameTextView.setText("Unknown User");
        }

        // Set message content
        if (message.isImage()) {
            holder.lastMessageTextView.setText("Image");
        } else {
            holder.lastMessageTextView.setText(message.getContent());
        }

        // Set last message time
        long timestamp = message.getTimestamp();
        String timeString = formatTimestamp(timestamp);
        holder.lastMessageTimeTextView.setText(timeString);

        // Navigate to the Chats activity when clicked
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, Chats.class);
            intent.putExtra("friendId", message.getFriendId()); // Assuming friendId is stored in Message
            intent.putExtra("friendName", message.getFriendName());
            Log.d("friend","friendId:"+message.getFriendId());
            Log.d("friend","friendName:"+message.getFriendName());
            context.startActivity(intent);
        });

        // Thể hiện trạng thái của user (tránh lắng nghe nhiều lần)
        setUserStatus(holder, message.getFriendId());
    }

    private void setUserStatus(@NonNull ViewHolder holder, String friendId) {
        // Lắng nghe trạng thái của người dùng chỉ một lần
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(friendId).child("status");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String status = dataSnapshot.getValue(String.class);
                // Xử lý trạng thái người dùng
                if ("online".equals(status)) {
                    holder.viewStatus.setBackgroundResource(R.drawable.status_online_circle); // Hình tròn xanh
                    holder.viewStatus.setVisibility(View.VISIBLE);
                } else if("offline".equals(status)){
                    holder.viewStatus.setBackgroundResource(R.drawable.status_offline_circle); // Hình tròn xám
                    holder.viewStatus.setVisibility(View.VISIBLE);
                }
                else holder.viewStatus.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error: " + databaseError.getMessage());
            }
        });
    }

    private void setImage(ImageView avt, String userId, Context context) {
        if (userId.contains(",") || userId.startsWith("GROUP_")) {
            // Nếu là nhóm chat, lấy dữ liệu từ Realtime Database
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("chatRooms").child(userId);
            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String imageUrl = snapshot.child("groupImage").getValue(String.class);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(context)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .into(avt);
                        } else {
                            avt.setImageResource(R.drawable.default_avatar);
                        }
                    } else {
                        avt.setImageResource(R.drawable.default_avatar);
                        Log.d("setImage", "Group chat document not found.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    avt.setImageResource(R.drawable.default_avatar);
                    Log.e("setImage", "Error fetching group chat data", error.toException());
                }
            });
        } else {
            // Nếu là người dùng, lấy dữ liệu từ Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userDoc = db.collection("users").document(userId);
            userDoc.get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String imageUrl = task.getResult().getString("image");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(context)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.default_avatar)
                                        .into(avt);
                            } else {
                                avt.setImageResource(R.drawable.default_avatar);
                            }
                        } else {
                            avt.setImageResource(R.drawable.default_avatar);
                            Log.d("setImage", "Failed to fetch user document.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        avt.setImageResource(R.drawable.default_avatar);
                        Log.e("setImage", "Error fetching user document", e);
                    });
        }
    }


    private String formatTimestamp(long timestamp) {
        SimpleDateFormat format = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return format.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }



    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImageView;
        TextView userNameTextView;
        TextView lastMessageTextView;
        TextView lastMessageTimeTextView;
        View viewStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            lastMessageTimeTextView = itemView.findViewById(R.id.lastMessageTimeTextView);
            viewStatus=itemView.findViewById(R.id.status);
        }
    }
}
