package com.example.chatapp.Chat;

public class Message {
    private String senderId;
    private String senderName;
    private String content; // This will be used for both text and image Uris
    private String imageUri; // Use this specifically for images
    private long timestamp;

    // Constructor
    public Message() {}

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isImage() {
        return imageUri != null && !imageUri.isEmpty(); // Check if imageUri is not null or empty
    }

}
