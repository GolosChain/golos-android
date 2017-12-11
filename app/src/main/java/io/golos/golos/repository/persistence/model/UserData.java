package io.golos.golos.repository.persistence.model;

import javax.annotation.Nullable;

/**
 * Created by yuri on 13.11.17.
 */

public class UserData implements Cloneable{
    private boolean isUserLoggedIn;
    @Nullable
    private String mMoto;
    @Nullable
    private String avatarPath;
    @Nullable
    private String userName;
    @Nullable
    private String privateActiveWif;
    @Nullable
    private String privatePostingWif;
    @Nullable
    private long subscibesCount;
    private long subscribersCount;
    private double gbgAmount;
    private double golosAmount;
    private double golosPower;
    private double accountWorth;
    private long postsCount;
    private double safeGbg;
    private double safeGolos;


    public UserData(boolean isUserLoggedIn,
                    @Nullable String mMoto,
                    @Nullable String avatarPath,
                    @Nullable  String userName,
                    @Nullable  String privateActiveWif,
                    @Nullable  String privatePostingWif,
                    long subscibesCount,
                    long subscribersCount,
                    double gbgAmount,
                    double golosAmount,
                    double golosPower,
                    double accountWorth,
                    long postsCount,
                    double safeGbg,
                    double safeGolos) {
        this.isUserLoggedIn = isUserLoggedIn;
        this.mMoto = mMoto;
        this.avatarPath = avatarPath;
        this.userName = userName;
        this.privateActiveWif = privateActiveWif;
        this.privatePostingWif = privatePostingWif;
        this.subscibesCount = subscibesCount;
        this.subscribersCount = subscribersCount;
        this.gbgAmount = gbgAmount;
        this.golosAmount = golosAmount;
        this.golosPower = golosPower;
        this.accountWorth = accountWorth;
        this.postsCount = postsCount;
        this.safeGbg = safeGbg;
        this.safeGolos = safeGolos;
    }

    public UserData() {
    }

    public double getSafeGolos() {
        return safeGolos;
    }

    public void setSafeGolos(double safeGolos) {
        this.safeGolos = safeGolos;
    }

    public double getSafeGbg() {
        return safeGbg;
    }

    public void setSafeGbg(double safeGbg) {
        this.safeGbg = safeGbg;
    }

    public long getPostsCount() {
        return postsCount;
    }

    public void setPostsCount(long postsCount) {
        this.postsCount = postsCount;
    }

    public double getGbgAmount() {
        return gbgAmount;
    }

    public void setGbgAmount(double gbgAmount) {
        this.gbgAmount = gbgAmount;
    }

    public double getGolosAmount() {
        return golosAmount;
    }

    public void setGolosAmount(double golosAmount) {
        this.golosAmount = golosAmount;
    }

    public double getGolosPower() {
        return golosPower;
    }

    public void setGolosPower(double golosPower) {
        this.golosPower = golosPower;
    }

    public double getAccountWorth() {
        return accountWorth;
    }

    public void setAccountWorth(double accountWorth) {
        this.accountWorth = accountWorth;
    }

    public boolean isUserLoggedIn() {
        return isUserLoggedIn;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void setUserLoggedIn(boolean userLoggedIn) {
        this.isUserLoggedIn = userLoggedIn;
    }

    @Nullable
    public String getmMoto() {
        return mMoto;
    }

    @Nullable
    public void setmMoto(@Nullable String mMoto) {
        this.mMoto = mMoto;
    }

    @Nullable
    public long getSubscibesCount() {
        return subscibesCount;
    }

    public void setSubscibesCount(@Nullable long subscibesCount) {
        this.subscibesCount = subscibesCount;
    }

    @Nullable
    public long getSubscribersCount() {
        return subscribersCount;
    }

    @Nullable
    public void setSubscribersCount(long subscribersCount) {
        this.subscribersCount = subscribersCount;
    }


    @Nullable
    public String getAvatarPath() {
        return avatarPath;
    }

    @Nullable
    public void setAvatarPath(@Nullable String avatarPath) {
        this.avatarPath = avatarPath;
    }

    @Nullable
    public String getUserName() {
        return userName;
    }

    @Nullable
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Nullable
    public String getPrivateActiveWif() {
        return privateActiveWif;
    }

    @Nullable
    public void setPrivateActiveWif(String privateActiveWif) {
        this.privateActiveWif = privateActiveWif;
    }

    @Nullable
    public String getPrivatePostingWif() {
        return privatePostingWif;
    }

    public void setPrivatePostingWif(String privatePostingWif) {
        this.privatePostingWif = privatePostingWif;
    }

    @Override
    public String toString() {
        return "UserData{" +
                "isUserLoggedIn=" + isUserLoggedIn +
                ", mMoto='" + mMoto + '\'' +
                ", avatarPath='" + avatarPath + '\'' +
                ", userName='" + userName + '\'' +
                ", privateActiveWif='" + privateActiveWif + '\'' +
                ", privatePostingWif='" + privatePostingWif + '\'' +
                ", subscibesCount=" + subscibesCount +
                ", subscribersCount=" + subscribersCount +
                ", gbgAmount=" + gbgAmount +
                ", golosAmount=" + golosAmount +
                ", golosPower=" + golosPower +
                ", accountWorth=" + accountWorth +
                ", postsCount=" + postsCount +
                '}';
    }
}
