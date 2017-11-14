package io.golos.golos.repository.persistence.model;

import javax.annotation.Nullable;

/**
 * Created by yuri on 13.11.17.
 */

public class UserData {
    @Nullable
    private String avatarPath;
    private String userName;
    private String privateActiveWif;
    private String privatePostingWif;

    public UserData(@Nullable String avatarPath, String userName, String privateActiveWif, String privatePostingWif) {
        this.avatarPath = avatarPath;
        this.userName = userName;
        this.privateActiveWif = privateActiveWif;
        this.privatePostingWif = privatePostingWif;
    }

    @Nullable
    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(@Nullable String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPrivateActiveWif() {
        return privateActiveWif;
    }

    public void setPrivateActiveWif(String privateActiveWif) {
        this.privateActiveWif = privateActiveWif;
    }

    public String getPrivatePostingWif() {
        return privatePostingWif;
    }

    public void setPrivatePostingWif(String privatePostingWif) {
        this.privatePostingWif = privatePostingWif;
    }

    @Override
    public String toString() {
        return "UserData{" +
                "avatarPath='" + avatarPath + '\'' +
                ", userName='" + userName + '\'' +
                ", privateActiveWif='" + privateActiveWif + '\'' +
                ", privatePostingWif='" + privatePostingWif + '\'' +
                '}';
    }
}
