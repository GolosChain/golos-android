package io.golos.golos.screens.profile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import com.fasterxml.jackson.annotation.JsonProperty
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.ApplicationUser
import io.golos.golos.repository.model.UserAuthResponse
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError
import timber.log.Timber

enum class LoginOptions {
    POSTING_KEY, ACTIVE_KEY, MASTER_KEY
}

data class UserProfileState(val isLoggedIn: Boolean,
                            val isLoading: Boolean,
                            val error: GolosError? = null,
                            val userName: String,
                            val avatarPath: String? = null,
                            val userPostsCount: Long = 0L,
                            val userAccountWorth: Double = 0.0,
                            val keyOptions: LoginOptions,
                            val subscribesNum: Int = 0,
                            val userMoto: String? = null,
                            val subscribersNum: Int = 0,
                            val golosAmount: Double = 0.0,
                            val golosPower: Double = 0.0,
                            val gbgAmount: Double = 0.0,
                            val golosInSafeAmount: Double = 0.0,
                            val gbgInSafeAmount: Double = 0.0,
                            val accountWorth: Double = 0.0)

data class AuthState(@JsonProperty("loggedIn") val isLoggedIn: Boolean,
                     @JsonProperty("username") val username: String = "")

data class AuthUserInput(val login: String,
                         val masterKey: String = "",
                         val postingWif: String = "",
                         val activeWif: String = "")

class AuthViewModel(app: Application) : AndroidViewModel(app), Observer<ApplicationUser> {
    val userProfileState = MutableLiveData<UserProfileState>()
    val userAuthState = Transformations.map(Repository.get.appUserData
    ) {
        if (it?.isLogged == true) AuthState(true, it.name)
        else AuthState(false, "")
    }!!
    private var mLastUserInput = AuthUserInput("")
    private val mRepository = Repository.get

    init {
        mRepository
                .appUserData.observeForever(this)
    }

    override fun onChanged(it: ApplicationUser?) {
        if (it?.isLogged != true) {
            userProfileState.value = UserProfileState(false, false, null, userProfileState.value?.userName.orEmpty(),
                    keyOptions = userProfileState.value?.keyOptions ?: LoginOptions.POSTING_KEY)
        } else {
            initUser()
        }
    }

    fun onUserInput(input: AuthUserInput) {
        mLastUserInput = input.copy()
    }

    fun onChangeKeyTypeClick(newKey: LoginOptions) {
        userProfileState.value = userProfileState.value?.copy(isLoading = false, keyOptions = newKey)
    }

    fun onCancelClick() {

    }

    private fun showAuthError(error: GolosError) {
        userProfileState.value = userProfileState.value?.copy(error = error, isLoading = false)
    }

    fun onLoginClick() {
        if (mLastUserInput.login.isEmpty()) {
            showAuthError(GolosError(ErrorCode.ERROR_AUTH,
                    localizedMessage = R.string.enter_login,
                    nativeMessage = null))

            return
        }

        when (userProfileState.value?.keyOptions ?: return) {
            LoginOptions.ACTIVE_KEY -> {
                if (mLastUserInput.activeWif.isEmpty()) {
                    showAuthError(GolosError(ErrorCode.ERROR_AUTH,
                            null,
                            R.string.enter_private_active_wif))
                } else {
                    userProfileState.value = userProfileState.value?.copy(isLoading = true, error = null)
                    mRepository.authWithActiveWif(mLastUserInput.login,
                            mLastUserInput.activeWif
                    ) { proceedAuthResponse(it) }
                }
            }
            LoginOptions.POSTING_KEY -> {

                if (mLastUserInput.postingWif.isEmpty()) {
                    showAuthError(GolosError(ErrorCode.ERROR_AUTH,
                            null,
                            R.string.enter_private_posting_wif))
                } else {
                    userProfileState.value = userProfileState.value?.copy(isLoading = true, error = null)

                    mRepository.authWithPostingWif(mLastUserInput.login,
                            mLastUserInput.postingWif
                    ) { proceedAuthResponse(it) }
                }
            }
            LoginOptions.MASTER_KEY -> {

                if (mLastUserInput.masterKey.isEmpty()) {
                    showAuthError(GolosError(ErrorCode.ERROR_AUTH,
                            null,
                            R.string.enter_private_master))
                } else {
                    userProfileState.value = userProfileState.value?.copy(isLoading = true, error = null)

                    mRepository.authWithMasterKey(mLastUserInput.login,
                            mLastUserInput.masterKey
                    ) { proceedAuthResponse(it) }
                }
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
                    userName = resp.accountInfo.userName,
                    avatarPath = resp.accountInfo.avatarPath,
                    userPostsCount = resp.accountInfo.postsCount,
                    userAccountWorth = resp.accountInfo.accountWorth,
                    isLoading = false,
                    subscribersNum = resp.accountInfo.subscribersCount,
                    subscribesNum = resp.accountInfo.subscriptionsCount,
                    userMoto = resp.accountInfo.userMotto,
                    golosAmount = resp.accountInfo.golosAmount,
                    golosPower = resp.accountInfo.golosPower,
                    gbgAmount = resp.accountInfo.gbgAmount,
                    gbgInSafeAmount = resp.accountInfo.safeGbg,
                    golosInSafeAmount = resp.accountInfo.safeGolos,
                    accountWorth = resp.accountInfo.accountWorth,
                    keyOptions = userProfileState.value?.keyOptions ?: LoginOptions.POSTING_KEY)
        }
    }

    private fun initUser() {
        val appUserData =
                mRepository.getGolosUserAccountInfos()
                        .value
                        ?.get(mRepository.appUserData.value?.name
                                ?: return) ?: return

        userProfileState.value = UserProfileState(isLoggedIn = true,
                isLoading = false,
                userName = appUserData.userName,
                avatarPath = appUserData.avatarPath,
                userAccountWorth = appUserData.gbgAmount,
                userPostsCount = appUserData.postsCount,
                subscribesNum = appUserData.subscriptionsCount,
                subscribersNum = appUserData.subscribersCount,
                userMoto = appUserData.userMotto,
                golosAmount = appUserData.golosAmount,
                golosPower = appUserData.golosPower,
                gbgAmount = appUserData.gbgAmount,
                gbgInSafeAmount = appUserData.safeGbg,
                golosInSafeAmount = appUserData.safeGolos,
                accountWorth = appUserData.accountWorth,
                keyOptions = userProfileState.value?.keyOptions ?: LoginOptions.POSTING_KEY)

    }
}