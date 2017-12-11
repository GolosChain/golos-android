package io.golos.golos.screens.profile

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
                            val isLoading: Boolean = false,
                            val error: GolosError? = null,
                            val userName: String = "",
                            val avatarPath: String? = null,
                            val userPostsCount: Long = 0L,
                            val userAccountWorth: Double = 0.0,
                            val isScanMenuVisible: Boolean = false,
                            val subscribesNum: Long = 0,
                            val userMoto: String? = null,
                            val subscribersNum: Long = 0,
                            val golosAmount: Double = 0.0,
                            val golosPower: Double = 0.0,
                            val gbgAmount: Double = 0.0,
                            val golosInSafeAmount: Double = 0.0,
                            val gbgInSafeAmount: Double = 0.0,
                            val accountWorth: Double = 0.0)

data class AuthState(val isLoggedIn: Boolean)

data class AuthUserInput(val login: String,
                         val masterKey: String = "",
                         val postingWif: String = "",
                         val activeWif: String = "")

class AuthViewModel(app: Application) : AndroidViewModel(app), Observer<UserData> {
    val userProfileState = MutableLiveData<UserProfileState>()
    val userAuthState = Transformations.map(userProfileState,
            {
                if (it?.isLoggedIn == true) AuthState(true)
                else AuthState(false)
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
                    isScanMenuVisible = false)
        } else if (t != null) {
            initUser(t)
        }
    }

    fun onUserInput(input: AuthUserInput) {
        mLastUserInput = input.copy()
    }

    fun onScanSwitch(isOn: Boolean) {
        if (isOn) {
            userProfileState.value = UserProfileState(false, false, null, mLastUserInput.login, isScanMenuVisible = true)
        } else {
            userProfileState.value = UserProfileState(false, false, null, mLastUserInput.login, isScanMenuVisible = false)
        }
    }

    fun onCancelClick() {

    }

    fun onLoginClick() {
        if (mLastUserInput.login.isEmpty()) {
            userProfileState.value = createStateCopyMutatingValue(error = GolosError(ErrorCode.ERROR_AUTH,
                    localizedMessage = R.string.enter_login,
                    nativeMessage = null))

        } else if (userProfileState.value?.isScanMenuVisible == false) {
            if (mLastUserInput.masterKey.isEmpty()) {
                userProfileState.value = createStateCopyMutatingValue(error = GolosError(ErrorCode.ERROR_AUTH,
                        null,
                        R.string.enter_password))
            } else {
                userProfileState.value = UserProfileState(false, true, null,
                        mLastUserInput.login, isScanMenuVisible = false)
                mRepository.authWithMasterKey(mLastUserInput.login,
                        mLastUserInput.masterKey,
                        { proceedAuthResponse(it) })
            }
        } else if (userProfileState.value?.isScanMenuVisible == true) {
            if (mLastUserInput.activeWif.isEmpty() && mLastUserInput.postingWif.isEmpty())
                userProfileState.value = createStateCopyMutatingValue(error = GolosError(ErrorCode.ERROR_AUTH,
                        null,
                        R.string.posting_or_active_key))
            else if (mLastUserInput.activeWif.isNotEmpty()) {
                userProfileState.value = UserProfileState(false, true, null,
                        mLastUserInput.login, isScanMenuVisible = true)

                mRepository.authWithActiveWif(mLastUserInput.login,
                        mLastUserInput.activeWif,
                        { proceedAuthResponse(it) })

            } else if (mLastUserInput.postingWif.isNotEmpty()) {
                userProfileState.value = UserProfileState(false, true, null,
                        mLastUserInput.login, isScanMenuVisible = true)

                mRepository.authWithPostingWif(mLastUserInput.login,
                        mLastUserInput.postingWif,
                        { proceedAuthResponse(it) })
            }
        }
    }

    private fun proceedAuthResponse(resp: UserAuthResponse) {
        if (!resp.isKeyValid) {
            userProfileState.value = createStateCopyMutatingValue(isLoggedIn = false,
                    isLoading = false,
                    error = resp.error)
        } else {
            userProfileState.value = UserProfileState(isLoggedIn = true,
                    userName = resp.userName ?: "",
                    avatarPath = resp.avatarPath,
                    userPostsCount = resp.postsCount,
                    userAccountWorth = resp.accountWorth,
                    isLoading = false,
                    subscribersNum = resp.subscribersCount,
                    subscribesNum = resp.subscibesCount,
                    userMoto = resp.userMotto,
                    golosAmount = resp.golosAmount,
                    golosPower = resp.golosPower,
                    gbgAmount = resp.gbgAmount,
                    gbgInSafeAmount = resp.safeGbg,
                    golosInSafeAmount = resp.safeGolos,
                    accountWorth = resp.accountWorth)
        }
    }

    private fun createStateCopyMutatingValue(
            isLoggedIn: Boolean? = null,
            isLoading: Boolean? = null,
            error: GolosError? = null,
            userName: String? = null,
            avatarPath: String? = null,
            userCommentsCount: Long? = null,
            userMoney: Double? = null,
            isScanMenuVisible: Boolean? = null): UserProfileState {
        return UserProfileState(isLoggedIn ?: userProfileState.value?.isLoggedIn == true,
                isLoading ?: userProfileState.value?.isLoading == true,
                error ?: userProfileState.value?.error,
                userName ?: mLastUserInput.login ?: "",
                avatarPath ?: userProfileState.value?.avatarPath,
                userCommentsCount ?: userProfileState.value?.userPostsCount ?: 0,
                userMoney ?: userProfileState.value?.userAccountWorth ?: 0.0,
                isScanMenuVisible ?: userProfileState.value?.isScanMenuVisible == true)
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
                    accountWorth = userData.accountWorth)
        } else {
            userProfileState.value = UserProfileState(isLoggedIn = false,
                    isLoading = false,
                    userName = mLastUserInput.login,
                    error = GolosError(ErrorCode.ERROR_AUTH, null,
                            R.string.wrong_credentials))
        }
    }

    fun onLogoutClick() {
        mRepository.deleteUserdata()
        userProfileState.value = UserProfileState(false, false, avatarPath = null)
    }
}