package io.golos.golos.repository.persistence.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.annotation.Nullable;

import io.golos.golos.repository.model.UserAuthResponse;

/**
 * Created by yuri on 13.11.17.
 */
@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppUserData implements Cloneable {
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
    private String location;
    @Nullable
    private String webSite;
    @Nullable
    private String cover;

    private long registerDate;
    private int subscibesCount;
    private int subscribersCount;
    private double gbgAmount;
    private double golosAmount;
    private double golosPower;
    private double accountWorth;
    private long postsCount;
    private double safeGbg;
    private double safeGolos;
    private int votingPower;


    public AppUserData(boolean isUserLoggedIn,
                       @Nullable String mMoto,
                       @Nullable String avatarPath,
                       @Nullable String userName,
                       @Nullable String privateActiveWif,
                       @Nullable String privatePostingWif,
                       @Nullable String publicActiveWif,
                       @Nullable String publicPostingWif,
                       int subscibesCount,
                       int subscribersCount,
                       double gbgAmount,
                       double golosAmount,
                       double golosPower,
                       double accountWorth,
                       long postsCount,
                       double safeGbg,
                       double safeGolos,
                       int votingPower,
                       @Nullable
                               String location,
                       @Nullable
                               String webSite,
                       long registerDate,
                       @Nullable
                               String cover) {
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
        this.location = location;
        this.webSite = webSite;
        this.registerDate = registerDate;
        this.cover = cover;
    }

    public AppUserData() {
    }

    @Nullable
    public String getLocation() {
        return location;
    }

    public void setLocation(@Nullable String location) {
        this.location = location;
    }

    @Nullable
    public String getWebSite() {
        return webSite;
    }

    public void setWebSite(@Nullable String webSite) {
        this.webSite = webSite;
    }

    @Nullable
    public String getCover() {
        return cover;
    }

    public void setCover(@Nullable String cover) {
        this.cover = cover;
    }

    public long getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(long registerDate) {
        this.registerDate = registerDate;
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
    public int getSubscibesCount() {
        return subscibesCount;
    }

    public void setSubscibesCount(@Nullable int subscibesCount) {
        this.subscibesCount = subscibesCount;
    }

    @Nullable
    public int getSubscribersCount() {
        return subscribersCount;
    }

    @Nullable
    public void setSubscribersCount(int subscribersCount) {
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

    public static AppUserData fromPositiveAuthResponse(UserAuthResponse response) {
        return new AppUserData(true,
                response.getAccountInfo().getUserMotto(),
                response.getAccountInfo().getAvatarPath(),
                response.getAccountInfo().getUserName(),
                response.getActiveAuth().getSecond(),
                response.getPostingAuth().getSecond(),
                response.getAccountInfo().getActivePublicKey(),
                response.getAccountInfo().getPostingPublicKey(),
                0,
                0,
                response.getAccountInfo().getGbgAmount(),
                response.getAccountInfo().getGolosAmount(),
                response.getAccountInfo().getGolosPower(),
                response.getAccountInfo().getAccountWorth(),
                response.getAccountInfo().getPostsCount(),
                response.getAccountInfo().getSafeGbg(),
                response.getAccountInfo().getSafeGolos(),
                response.getAccountInfo().getVotingPower(),
                response.getAccountInfo().getLocation(),
                response.getAccountInfo().getWebsite(),
                response.getAccountInfo().getRegistrationDate(),
                response.getAccountInfo().getUserCover());
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
                ", subscribesCount=" + subscibesCount +
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
