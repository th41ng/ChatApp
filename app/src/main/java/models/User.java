package models;

import java.io.Serializable;

public class User implements Serializable {
    public String userId,name,image,email,token;
    public Boolean isFriendRequestSent=false;
    public boolean isFriendRequestSent() {
        return isFriendRequestSent;
    }

    public void setFriendRequestSent(boolean friendRequestSent) {
        isFriendRequestSent = friendRequestSent;
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

    public Boolean getFriendRequestSent() {
        return isFriendRequestSent;
    }

    public void setFriendRequestSent(Boolean friendRequestSent) {
        isFriendRequestSent = friendRequestSent;
    }
}
