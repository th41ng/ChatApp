package com.example.chatapp.Chat;

import android.app.MediaRouteButton;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

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
        // Ẩn tên người gửi nếu là tin nhắn của chính người dùng hiện tại
        if (senderId != null && senderId.equals(currentUserId)) {
            holder.senderName.setVisibility(View.GONE); // Ẩn TextView tên người gửi
        } else {
            // Hiển thị tên người gửi nếu tin nhắn trước đó là từ người khác
            if (position == 0 || !messageList.get(position - 1).getSenderId().equals(senderId)) {
                getUserName(senderId, holder.senderName); // Lấy tên người gửi
                holder.senderName.setVisibility(View.VISIBLE);
            } else {
                holder.senderName.setVisibility(View.GONE); // Ẩn nếu tin nhắn liên tiếp từ cùng người
            }
        }

        // Highlight tin nhắn nếu là tin nhắn tại vị trí được highlight
        if (position == highlightedPosition) {
            holder.itemView.setBackgroundColor(Color.YELLOW); // Màu nền khi highlight
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT); // Màu nền mặc định
        }

        if (message.isImage()) {
            // Ẩn các TextViews và Avatars
            holder.messageTextViewSender.setVisibility(View.GONE);
            holder.messageTextViewReceiver.setVisibility(View.GONE);
            holder.imageViewAvtReceiver.setVisibility(View.GONE);
            if ("system".equals(senderId)) {
                // Hiển thị tin nhắn hệ thống
                holder.messagetextViewSystem.setText(message.getContent());
                holder.messagetextViewSystem.setVisibility(View.VISIBLE);
                holder.imageViewMessageSender.setVisibility(View.GONE);
                holder.imageViewMessageReceiver.setVisibility(View.GONE);
            } else if (senderId.equals(currentUserId)) {
                // Hiển thị ảnh của người gửi
                holder.messagetextViewSystem.setVisibility(View.GONE);
                holder.imageViewMessageSender.setVisibility(View.VISIBLE);
                holder.imageViewMessageReceiver.setVisibility(View.GONE);
                Glide.with(context).load(message.getImageUri()).into(holder.imageViewMessageSender);
            } else {
                // Hiển thị ảnh của người nhận
                holder.imageViewMessageSender.setVisibility(View.GONE);
                holder.imageViewMessageReceiver.setVisibility(View.VISIBLE);
                holder.messagetextViewSystem.setVisibility(View.GONE);
                if (position == 0 || !messageList.get(position - 1).getSenderId().equals(senderId)) {
                    // Hiển thị avatar nếu là tin nhắn đầu tiên hoặc khác người gửi trước đó
                    setImage(holder.imageViewAvtReceiver, senderId, holder.itemView.getContext());
                    holder.imageViewAvtReceiver.setVisibility(View.VISIBLE);
                } else {
                    holder.imageViewAvtReceiver.setVisibility(View.INVISIBLE);
                }
                Glide.with(context).load(message.getImageUri()).into(holder.imageViewMessageReceiver);
            }
        } else {
            // Nếu tin nhắn là văn bản
            holder.imageViewMessageSender.setVisibility(View.GONE);
            holder.imageViewMessageReceiver.setVisibility(View.GONE);
            holder.imageViewAvtReceiver.setVisibility(View.GONE);
            holder.messagetextViewSystem.setVisibility(View.GONE);
            if ("system".equals(senderId)) {
                // Hiển thị tin nhắn hệ thống
                holder.messagetextViewSystem.setText(message.getContent());
                holder.messagetextViewSystem.setVisibility(View.VISIBLE);
            } else if (senderId.equals(currentUserId)) {
                // Tin nhắn văn bản từ người gửi
                holder.messageTextViewSender.setText(message.getContent());
                holder.messageTextViewSender.setVisibility(View.VISIBLE);
                holder.messageTextViewReceiver.setVisibility(View.GONE);
                holder.messagetextViewSystem.setVisibility(View.GONE);
            } else {
                holder.messagetextViewSystem.setVisibility(View.GONE);
                holder.messageTextViewReceiver.setText(message.getContent());
                holder.messageTextViewReceiver.setVisibility(View.VISIBLE);
                holder.messageTextViewSender.setVisibility(View.GONE);
                // Tin nhắn văn bản từ người khác
                if (position == 0 || !messageList.get(position - 1).getSenderId().equals(senderId)) {
                    setImage(holder.imageViewAvtReceiver, senderId, holder.itemView.getContext());
                    holder.imageViewAvtReceiver.setVisibility(View.VISIBLE);
                } else {
                    holder.imageViewAvtReceiver.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private void getUserName(String userId, TextView nameTextView) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        nameTextView.setText(name);
                    }
                });

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
        ImageView imageViewAvtReceiver;
        ImageView imageViewMessageSender; // ImageView cho tin nhắn gửi
        ImageView imageViewMessageReceiver; // ImageView cho tin nhắn nhận
        TextView messageTextViewSender; // TextView cho tin nhắn gửi
        TextView messageTextViewReceiver; // TextView cho tin nhắn nhận
        TextView messagetextViewSystem;
        TextView senderName;

        // Constructor của ViewHolder, khởi tạo các tham chiếu đến View trong item
        ViewHolder(View itemView) {
            super(itemView);
            senderName = itemView.findViewById(R.id.senderName);
            imageViewAvtReceiver = itemView.findViewById(R.id.imageViewAvtReceiver);
            imageViewMessageSender = itemView.findViewById(R.id.imageViewMessageSender);
            imageViewMessageReceiver = itemView.findViewById(R.id.imageViewMessageReceiver);
            messageTextViewSender = itemView.findViewById(R.id.messageTextViewSender);
            messageTextViewReceiver = itemView.findViewById(R.id.messageTextViewReceiver);
            messagetextViewSystem = itemView.findViewById(R.id.messagetextViewSystem);
        }
    }

    private void setImage(ImageView imageView, String userId, Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDoc = db.collection("users").document(userId);

        userDoc.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String imageUrl = task.getResult().getString("image"); // Trường 'image' lưu URL ảnh
                        if (imageUrl != null) {
                            // Sử dụng Glide để tải ảnh từ URL
                            Glide.with(context)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_avatar)
                                    .into(imageView);
                        } else {
                            imageView.setImageResource(R.drawable.default_avatar); // Hình mặc định
                        }
                    } else {
                        imageView.setImageResource(R.drawable.default_avatar); // Hình mặc định
                        Log.d("FriendRequest", "Lỗi khi lấy dữ liệu Firestore");
                    }
                })
                .addOnFailureListener(e -> {
                    imageView.setImageResource(R.drawable.default_avatar); // Hình mặc định
                    Log.d("FriendRequest", "Lỗi khi lấy dữ liệu Firestore", e);
                });
    }
}
