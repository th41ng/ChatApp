package com.example.chatapp.Chat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatapp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import models.Message;
//Adapter để tìm kiếm thông tin
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    private List<Message> searchResults;
    private OnItemClickListener onItemClickListener;

    public SearchResultsAdapter(List<Message> searchResults) {
        this.searchResults = searchResults;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_search_result, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = searchResults.get(position);
        holder.messageContent.setText(message.getContent());  // Bind content
        String formattedTimestamp = formatTimestamp(message.getTimestamp());
        holder.messageTimestamp.setText(formattedTimestamp);  // Optionally bind timestamp

        // Log binding process
        Log.d("SearchResultsAdapter", "Binding message: " + message.getContent());
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(message);
            }
        });
    }

    @Override
    public int getItemCount() {
        Log.d("SearchResultsAdapter", "Item count: " + searchResults.size());
        return searchResults.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView messageContent;
        TextView messageTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContent = itemView.findViewById(R.id.messageContent);
            messageTimestamp = itemView.findViewById(R.id.messageTimestamp);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Message message);
    }
    private String formatTimestamp(long timestamp) {
        // Create an instance of SimpleDateFormat for formatting the timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        // Convert the timestamp (milliseconds) into a Date object
        Date date = new Date(timestamp);
        // Format the Date object to a readable string
        return sdf.format(date);
    }

}