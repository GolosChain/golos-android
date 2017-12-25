package io.golos.golos.screens.profile.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.Transformations
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.UserAuthResponse
import io.golos.golos.repository.persistence.model.UserData
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError

data class UserProfileState(val isLoggedIn: Boolean,
                            val isLoading: Boolean,
                            val error: GolosError? = null,
                            val userName: String,
                            val avatarPath: String? = null,
                            val userPostsCount: Long = 0L,
                            val userAccountWorth: Double = 0.0,
                            val isPostingKeyVisible: Boolean,
                            val subscribesNum: Long = 0,
                            val userMoto: String? = null,
                            val subscribersNum: Long = 0,
                            val golosAmount: Double = 0.0,
                            val golosPower: Double = 0.0,
                            val gbgAmount: Double = 0.0,
                            val golosInSafeAmount: Double = 0.0,
                            val gbgInSafeAmount: Double = 0.0,
                            val accountWorth: Double = 0.0)

data class AuthState(val isLoggedIn: Boolean,
                     val username: String = "")

data class AuthUserInput(val login: String,
                         val masterKey: String = "",
                         val postingWif: String = "",
                         val activeWif: String = "")

class AuthViewModel(app: Application) : AndroidViewModel(app), Observer<UserData> {
    val userProfileState = MutableLiveData<UserProfileState>()
    val userAuthState = Transformations.map(userProfileState,
            {
                if (it?.isLoggedIn == true) AuthState(true, it.userName)
                else AuthState(false, "")
            })
    private var mLastUserInput = AuthUserInput("")
    private val mRepository = Repository.get

    init {
        mRepository
                .getCurrentUserDataAsLiveData().observeForever(this)
        mRepository.requestActiveUserDataUpdate()
    }

    override fun onChanged(t: UserData?) {
        if (t == null || !t.isUserLoggedIn) {
            userProfileState.value = UserProfileState(isLoggedIn = false,
                    isPostingKeyVisible = true,
                    isLoading = false,
                    userName = t?.userName ?: "")
        } else if (t != null) {
            initUser(t)
        }
    }

    fun onUserInput(input: AuthUserInput) {
        mLastUserInput = input.copy()
    }

    fun onChangeKeyTypeClick() {
        val isCurrentPostingVisible = userProfileState.value?.isPostingKeyVisible ?: true
        if (isCurrentPostingVisible) {
            userProfileState.value = UserProfileState(false, false, null, mLastUserInput.login, isPostingKeyVisible = false)
        } else {
            userProfileState.value = UserProfileState(false, false, null, mLastUserInput.login, isPostingKeyVisible = true)
        }
    }

    fun onCancelClick() {

    }

    private fun showAuthError(error: GolosError) {
        userProfileState.value = UserProfileState(error = error,
                isPostingKeyVisible = userProfileState.value?.isPostingKeyVisible == true,
                isLoggedIn = false,
                isLoading = false,
                userName = userProfileState.value?.userName ?: "")
    }

    fun onLoginClick() {
        if (mLastUserInput.login.isEmpty()) {
            showAuthError(GolosError(ErrorCode.ERROR_AUTH,
                    localizedMessage = R.string.enter_login,
                    nativeMessage = null))

        } else if (userProfileState.value?.isPostingKeyVisible == false) {
            if (mLastUserInput.activeWif.isEmpty()) {
                showAuthError(GolosError(ErrorCode.ERROR_AUTH,
                        null,
                        R.string.enter_private_active_wif))
            } else {
                userProfileState.value = UserProfileState(false, true, null,
                        mLastUserInput.login, isPostingKeyVisible = false)
                mRepository.authWithActiveWif(mLastUserInput.login,
                        mLastUserInput.activeWif,
                        { proceedAuthResponse(it) })
            }
        } else if (userProfileState.value?.isPostingKeyVisible == true) {
            if (mLastUserInput.postingWif.isEmpty()) {
                showAuthError(GolosError(ErrorCode.ERROR_AUTH,
                        null,
                        R.string.enter_private_posting_wif))
            } else {
                userProfileState.value = UserProfileState(false,
                        true, null,
                        mLastUserInput.login,
                        isPostingKeyVisible = true,
                        userMoto = userProfileState.value?.userName ?: "")

                mRepository.authWithPostingWif(mLastUserInput.login,
                        mLastUserInput.postingWif,
                        { proceedAuthResponse(it) })

            }
        }
    }

    private fun proceedAuthResponse(resp: UserAuthResponse) {
        if (!resp.isKeyValid) {
            resp.error?.let {
                showAuthError(it)
            }
        } else {
            userProfileState.value = UserProfileState(isLoggedIn = true,
                    userName = resp.accountInfo.userName ?: "",
                    avatarPath = resp.accountInfo.avatarPath,
                    userPostsCount = resp.accountInfo.postsCount,
                    userAccountWorth = resp.accountInfo.accountWorth,
                    isLoading = false,
                    subscribersNum = resp.accountInfo.subscribersCount,
                    subscribesNum = resp.accountInfo.subscibesCount,
                    userMoto = resp.accountInfo.userMotto,
                    golosAmount = resp.accountInfo.golosAmount,
                    golosPower = resp.accountInfo.golosPower,
                    gbgAmount = resp.accountInfo.gbgAmount,
                    gbgInSafeAmount = resp.accountInfo.safeGbg,
                    golosInSafeAmount = resp.accountInfo.safeGolos,
                    accountWorth = resp.accountInfo.accountWorth,
                    isPostingKeyVisible = userProfileState.value?.isPostingKeyVisible == true)
        }
    }

    private fun initUser(userData: UserData) {
        if (userData.privateActiveWif != null || userData.privatePostingWif != null) {
            userProfileState.value = UserProfileState(isLoggedIn = true,
                    isLoading = false,
                    userName = userData.userName ?: "",
                    avatarPath = userData.avatarPath,
                    userAccountWorth = userData.gbgAmount,
                    userPostsCount = userData.postsCount,
                    subscribesNum = userData.subscibesCount,
                    subscribersNum = userData.subscribersCount,
                    userMoto = userData.getmMoto(),
                    golosAmount = userData.golosAmount,
                    golosPower = userData.golosPower,
                    gbgAmount = userData.gbgAmount,
                    gbgInSafeAmount = userData.safeGbg,
                    golosInSafeAmount = userData.safeGolos,
                    accountWorth = userData.accountWorth,
                    isPostingKeyVisible = userProfileState.value?.isPostingKeyVisible == true)
        } else {
            showAuthError(GolosError(ErrorCode.ERROR_AUTH, null,
                    R.string.wrong_credentials))
        }
    }

    fun onLogoutClick() {
        mRepository.deleteUserdata()
        userProfileState.value = UserProfileState(false,
                false,
                avatarPath = null,
                isPostingKeyVisible = true,
                userName = "")
    }
}