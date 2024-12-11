package models;

import java.util.List;

public class GroupChatRoom {
    private String chatRoomId;

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    private String managerId;
    private List<String> participants;
    private long timestamp;

    public GroupChatRoom() {
        // Constructor mặc định cho Firestore
    }

    public GroupChatRoom(String chatRoomId,String managerId, List<String> participants, long timestamp) {
        this.chatRoomId = chatRoomId;
        this.managerId=managerId;
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
