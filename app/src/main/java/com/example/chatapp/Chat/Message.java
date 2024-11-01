package com.example.chatapp.Chat;

public class Message {
    private String senderId;
    private String senderName;
    private String content;
    private long timestamp;
    private boolean isImage;

    public Message(){
        // Constructor rỗng cần thiết cho Firebase
    }

    public Message(String senderId, String senderName, String content, long timestamp, boolean isImage){
        this.senderId=senderId;
        this.senderName=senderName;
        this.content=content;
        this.timestamp=timestamp;
        this.isImage=isImage;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }


    public boolean isImage() {
        return isImage;
    }

}
