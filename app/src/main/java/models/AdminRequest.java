package models;

import com.example.chatapp.ChatUser.AdminRequestView;

import java.io.Serializable;
import java.util.List;

public class AdminRequest implements Serializable {


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

    public String getUserChangeId() {
        return userChangeId;
    }

    public void setUserChangeId(String userChangeId) {
        this.userChangeId = userChangeId;
    }

    public String getNameChange() {
        return nameChange;
    }

    public void setNameChange(String nameChange) {
        this.nameChange = nameChange;
    }


    private String userId;
    private String name;
    private String userChangeId;
    private String nameChange;
    private String groupName;
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
