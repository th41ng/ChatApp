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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import models.Message;

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

        // Set avatar image (use Glide to load the avatar URL)
        String avatarUrl = message.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(context).load(avatarUrl).into(holder.avatarImageView);
        }

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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            lastMessageTimeTextView = itemView.findViewById(R.id.lastMessageTimeTextView);
        }
    }
}
