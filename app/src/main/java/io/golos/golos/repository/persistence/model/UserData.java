package io.golos.golos.repository.persistence.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.annotation.Nullable;

import io.golos.golos.repository.model.UserAuthResponse;

/**
 * Created by yuri on 13.11.17.
 */
@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserData implements Cloneable {
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
    private String publicActiveWif;
    @Nullable
    private String privatePostingWif;
    @Nullable
    private String publicPostingWif;
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
    private int votingPower;


    public UserData(boolean isUserLoggedIn,
                    @Nullable String mMoto,
                    @Nullable String avatarPath,
                    @Nullable String userName,
                    @Nullable String privateActiveWif,
                    @Nullable String privatePostingWif,
                    @Nullable String publicActiveWif,
                    @Nullable String publicPostingWif,
                    long subscibesCount,
                    long subscribersCount,
                    double gbgAmount,
                    double golosAmount,
                    double golosPower,
                    double accountWorth,
                    long postsCount,
                    double safeGbg,
                    double safeGolos,
                    int votingPower) {
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
        this.publicActiveWif = publicActiveWif;
        this.publicPostingWif = publicPostingWif;
        this.votingPower = votingPower;
    }

    public UserData() {
    }

    public String getPublicActiveWif() {
        return publicActiveWif;
    }

    public void setPublicActiveWif(String publicActiveWif) {
        this.publicActiveWif = publicActiveWif;
    }

    public String getPublicPostingWif() {
        return publicPostingWif;
    }

    public void setPublicPostingWif(String publicPostingWif) {
        this.publicPostingWif = publicPostingWif;
    }

    public int getVotingPower() {
        return votingPower;
    }

    public void setVotingPower(int votingPower) {
        this.votingPower = votingPower;
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

    public static UserData fromPositiveAuthResponse(UserAuthResponse response) {
        return new UserData(true,
                response.getAccountInfo().getUserMotto(),
                response.getAccountInfo().getAvatarPath(),
                response.getAccountInfo().getUserName(),
                response.getActiveAuth().getSecond(),
                response.getPostingAuth().getSecond(),
                response.getAccountInfo().getActivePublicKey(),
                response.getAccountInfo().getPostingPublicKey(),
                response.getAccountInfo().getSubscibesCount(),
                response.getAccountInfo().getSubscribersCount(),
                response.getAccountInfo().getGbgAmount(),
                response.getAccountInfo().getGolosAmount(),
                response.getAccountInfo().getGolosPower(),
                response.getAccountInfo().getAccountWorth(),
                response.getAccountInfo().getPostsCount(),
                response.getAccountInfo().getSafeGbg(),
                response.getAccountInfo().getSafeGolos(),
                response.getAccountInfo().getVotingPower());
    }

    public AccountInfo toAccountInfo() {

        return new AccountInfo(userName, mMoto, avatarPath,
                postsCount, accountWorth, subscibesCount, subscribersCount,
                gbgAmount, golosAmount, golosPower, safeGbg,
                safeGolos, publicPostingWif, publicActiveWif, false, votingPower);
    }

    @Override
    public String toString() {
        return "UserData{" +
                "isUserLoggedIn=" + isUserLoggedIn +
                ", mMoto='" + mMoto + '\'' +
                ", avatarPath='" + avatarPath + '\'' +
                ", userName='" + userName + '\'' +
                ", privateActiveWif='" + privateActiveWif + '\'' +
                ", publicActiveWif='" + publicActiveWif + '\'' +
                ", privatePostingWif='" + privatePostingWif + '\'' +
                ", publicPostingWif='" + publicPostingWif + '\'' +
                ", subscibesCount=" + subscibesCount +
                ", subscribersCount=" + subscribersCount +
                ", gbgAmount=" + gbgAmount +
                ", golosAmount=" + golosAmount +
                ", golosPower=" + golosPower +
                ", accountWorth=" + accountWorth +
                ", postsCount=" + postsCount +
                ", safeGbg=" + safeGbg +
                ", safeGolos=" + safeGolos +
                ", votingPower=" + votingPower +
                '}';
    }
}
