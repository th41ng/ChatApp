package models;

public class Message {
    private String friendId;
    private String senderId;

    private String content; // This will be used for both text and image Uris
    private String imageUri; // Use this specifically for images

    private long timestamp;
    private String friendName;


    // Constructor
    public Message() {}

    // Getters and Setters
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFriendName() {
        return friendName;
    }

    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }

    public boolean isImage() {
        return imageUri != null && !imageUri.isEmpty(); // Check if imageUri is not null or empty
    }

    public String getFriendId() {
        return friendId;
    }
    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }
}
