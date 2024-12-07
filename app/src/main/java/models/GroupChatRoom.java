package models;

import java.util.List;

public class GroupChatRoom {
    private String chatRoomId;
    private List<String> participants;
    private long timestamp;

    public GroupChatRoom() {
        // Constructor mặc định cho Firestore
    }

    public GroupChatRoom(String chatRoomId, List<String> participants, long timestamp) {
        this.chatRoomId = chatRoomId;
        this.participants = participants;
        this.timestamp = timestamp;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
