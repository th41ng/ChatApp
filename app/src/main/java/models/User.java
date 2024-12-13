package models;

import java.io.Serializable;

public class User implements Serializable {
    private String userId;
    private String name;
    private String image;
    private String email;
    private Boolean disabled;

    public User(String participantId, String participantName) {
        this.userId = participantId;
        this.name = participantName;
    }
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    private String phone;
    private String token;
    private String friendStatus="none"; // Trạng thái kết bạn (none, sent, friend,received)
    public String getFriendStatus() {
        return friendStatus;
    }

    public void setFriendStatus(String friendStatus) {
        this.friendStatus = friendStatus;
    }

    public User() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getDisabled() {return disabled;}
    public void setDisabled(Boolean disabled) {this.disabled = disabled;}
}
