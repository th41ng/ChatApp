package com.example.chatapp.Chat;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private final Context context;
    private final List<Message> messageList;
    private int highlightedPosition;

    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
        this.highlightedPosition = -1;
    }



    public void highlightMessage(int position) {
        // Nếu có tin nhắn đã được highlight trước đó, reset lại
        int previousHighlightedPosition = highlightedPosition;
        highlightedPosition = position;

        // Nếu vị trí trước đó khác với vị trí hiện tại, làm mới cả hai tin nhắn
        if (previousHighlightedPosition != -1) {
            notifyItemChanged(previousHighlightedPosition);  // Reset tin nhắn trước
        }

        notifyItemChanged(position);  // Highlight tin nhắn hiện tại

        // Đặt một delay để bỏ highlight sau 3 giây
        new Handler().postDelayed(() -> {
            // Reset vị trí highlight sau 3 giây
            highlightedPosition = -1;
            notifyItemChanged(position);  // Cập nhật lại tin nhắn sau khi highlight mất đi
        }, 3000); // Highlight trong 3 giây
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_item_message, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messageList.get(position);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        String senderId = message.getSenderId();

        // Highlight message if it's the highlighted position
        if (position == highlightedPosition) {
            holder.itemView.setBackgroundColor(Color.YELLOW); // Highlight color
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT); // Default color
        }
        if (message.isImage()) {
            // Hide text views
            holder.messageTextViewSender.setVisibility(View.GONE);
            holder.messageTextViewReceiver.setVisibility(View.GONE);
            // Load image based on sender
            if (senderId.equals(currentUserId)) {
                holder.imageViewMessageSender.setVisibility(View.VISIBLE);
                holder.imageViewMessageReceiver.setVisibility(View.GONE);
                Glide.with(context).load(message.getImageUri()).into(holder.imageViewMessageSender);
            } else {
                holder.imageViewMessageSender.setVisibility(View.GONE);
                holder.imageViewMessageReceiver.setVisibility(View.VISIBLE);
                Glide.with(context).load(message.getImageUri()).into(holder.imageViewMessageReceiver);
            }
        } else {
            // Hide image views
            holder.imageViewMessageSender.setVisibility(View.GONE);
            holder.imageViewMessageReceiver.setVisibility(View.GONE);
            // Load text message based on sender
            if (senderId.equals(currentUserId)) {
                holder.messageTextViewSender.setText(message.getContent());
                holder.messageTextViewSender.setVisibility(View.VISIBLE);
                holder.messageTextViewReceiver.setVisibility(View.GONE);
            } else {
                holder.messageTextViewReceiver.setText(message.getContent());
                holder.messageTextViewReceiver.setVisibility(View.VISIBLE);
                holder.messageTextViewSender.setVisibility(View.GONE);
            }
        }
    }


    @Override
    public int getItemCount() {
        return messageList.size();
    }
    public List<Message> getMessageList() {
        return messageList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewMessageSender;
        ImageView imageViewMessageReceiver;
        TextView messageTextViewSender;
        TextView messageTextViewReceiver;
        ViewHolder(View itemView) {
            super(itemView);
            imageViewMessageSender = itemView.findViewById(R.id.imageViewMessageSender);
            imageViewMessageReceiver = itemView.findViewById(R.id.imageViewMessageReceiver);
            messageTextViewSender = itemView.findViewById(R.id.messageTextViewSender);
            messageTextViewReceiver = itemView.findViewById(R.id.messageTextViewReceiver);
        }
    }

}
