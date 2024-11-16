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
}
