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

import models.Message;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private final Context context; // Ngữ cảnh ứng dụng
    private final List<Message> messageList; // Danh sách tin nhắn
    private int highlightedPosition; // Vị trí của tin nhắn được highlight

    // Constructor khởi tạo adapter với ngữ cảnh và danh sách tin nhắn
    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
        this.highlightedPosition = -1; // Không có tin nhắn nào được highlight khi bắt đầu
    }

    // Phương thức dùng để highlight một tin nhắn tại vị trí nhất định
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

    // Tạo ViewHolder cho mỗi item trong RecyclerView
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout chat_item_message cho mỗi item
        View view = LayoutInflater.from(context).inflate(R.layout.chat_item_message, parent, false);
        return new ViewHolder(view); // Trả về ViewHolder mới
    }

    // Liên kết dữ liệu (tin nhắn) với view
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messageList.get(position); // Lấy tin nhắn tại vị trí hiện tại

        // Lấy ID người dùng hiện tại
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        String senderId = message.getSenderId(); // Lấy ID người gửi tin nhắn

        // Highlight tin nhắn nếu là tin nhắn tại vị trí được highlight
        if (position == highlightedPosition) {
            holder.itemView.setBackgroundColor(Color.YELLOW); // Màu nền khi highlight
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT); // Màu nền mặc định
        }

        if (message.isImage()) { // Nếu tin nhắn là hình ảnh
            // Ẩn các TextViews
            holder.messageTextViewSender.setVisibility(View.GONE);
            holder.messageTextViewReceiver.setVisibility(View.GONE);

            // Hiển thị ảnh tùy thuộc vào người gửi
            if (senderId.equals(currentUserId)) {
                holder.imageViewMessageSender.setVisibility(View.VISIBLE);
                holder.imageViewMessageReceiver.setVisibility(View.GONE);
                Glide.with(context).load(message.getImageUri()).into(holder.imageViewMessageSender); // Tải ảnh của người gửi
            } else {
                holder.imageViewMessageSender.setVisibility(View.GONE);
                holder.imageViewMessageReceiver.setVisibility(View.VISIBLE);
                Glide.with(context).load(message.getImageUri()).into(holder.imageViewMessageReceiver); // Tải ảnh của người nhận
            }
        } else { // Nếu tin nhắn là văn bản
            // Ẩn các ImageViews
            holder.imageViewMessageSender.setVisibility(View.GONE);
            holder.imageViewMessageReceiver.setVisibility(View.GONE);

            // Hiển thị tin nhắn văn bản tùy thuộc vào người gửi
            if (senderId.equals(currentUserId)) {
                holder.messageTextViewSender.setText(message.getContent());
                holder.messageTextViewSender.setVisibility(View.VISIBLE); // Hiển thị tin nhắn của người gửi
                holder.messageTextViewReceiver.setVisibility(View.GONE); // Ẩn tin nhắn của người nhận
            } else {
                holder.messageTextViewReceiver.setText(message.getContent());
                holder.messageTextViewReceiver.setVisibility(View.VISIBLE); // Hiển thị tin nhắn của người nhận
                holder.messageTextViewSender.setVisibility(View.GONE); // Ẩn tin nhắn của người gửi
            }
        }
    }

    // Trả về số lượng tin nhắn trong danh sách
    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // Trả về danh sách tin nhắn
    public List<Message> getMessageList() {
        return messageList;
    }

    // ViewHolder giữ các tham chiếu đến các View trong mỗi item tin nhắn
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewMessageSender; // ImageView cho tin nhắn gửi
        ImageView imageViewMessageReceiver; // ImageView cho tin nhắn nhận
        TextView messageTextViewSender; // TextView cho tin nhắn gửi
        TextView messageTextViewReceiver; // TextView cho tin nhắn nhận

        // Constructor của ViewHolder, khởi tạo các tham chiếu đến View trong item
        ViewHolder(View itemView) {
            super(itemView);
            imageViewMessageSender = itemView.findViewById(R.id.imageViewMessageSender);
            imageViewMessageReceiver = itemView.findViewById(R.id.imageViewMessageReceiver);
            messageTextViewSender = itemView.findViewById(R.id.messageTextViewSender);
            messageTextViewReceiver = itemView.findViewById(R.id.messageTextViewReceiver);
        }
    }
}
