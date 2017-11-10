package io.golos.golos.screens.drawer

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.os.Handler
import android.os.Looper
import eu.bittrade.libs.steemj.exceptions.SteemCommunicationException
import eu.bittrade.libs.steemj.exceptions.SteemConnectionException
import eu.bittrade.libs.steemj.exceptions.SteemTimeoutException
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.model.UserAuthResponse
import io.golos.golos.utils.ErrorCodes
import timber.log.Timber
import java.security.InvalidParameterException

data class UserProfileState(val isLoggedIn: Boolean,
                            val isLoading: Boolean = false,
                            val error: Pair<ErrorCodes, Int>? = null,
                            val userName: String = "",
                            val avatarPath: String? = null,
                            val userPostsCount: Long = 0L,
                            val userMoney: Double = 0.0,
                            val isScanMenuVisible: Boolean = false)

data class AuthState(val isLoggedIn: Boolean,
                     val userName: String = "",
                     val postingWif: String? = null,
                     val activeWif: String? = null)

data class AuthUserInput(val login: String,
                         val masterKey: String = "",
                         val postingWif: String = "",
                         val activeWif: String = "")

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    val userProfileState = MutableLiveData<UserProfileState>()
    val userAuthState = MutableLiveData<AuthState>()
    val onCloseDrawerRequest = MutableLiveData<Void>()
    private var mLastUserInput = AuthUserInput("")
    private val mRepository = Repository.get
    private val mExecutor = Repository.sharedExecutor
    private val mHandler = Handler(Looper.getMainLooper())

    init {
        userProfileState.value = UserProfileState(isLoggedIn = false, isScanMenuVisible = false)
        userAuthState.value = AuthState(isLoggedIn = false)
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
        onCloseDrawerRequest.value = null
    }

    fun onLoginClick() {
        Timber.e(mLastUserInput.toString())
        if (mLastUserInput.login.isEmpty()) {
            Timber.e("curremt input")
            userProfileState.value = createStateCopyMutatingValue(error = Pair(ErrorCodes.ERROR_AUTH, R.string.enter_login))
        } else if (userProfileState.value?.isScanMenuVisible == false) {
            if (mLastUserInput.masterKey.isEmpty()) {
                userProfileState.value = createStateCopyMutatingValue(error = Pair(ErrorCodes.ERROR_AUTH, R.string.enter_password))
            } else {
                userProfileState.value = UserProfileState(false, true, null,
                        mLastUserInput.login, isScanMenuVisible = false)
                postWithCatch({
                    val resp = mRepository.authWithMasterKey(mLastUserInput.login, mLastUserInput.masterKey)
                    proceedAuthResponse(resp)
                })
            }
        } else if (userProfileState.value?.isScanMenuVisible == true) {
            if (mLastUserInput.activeWif.isEmpty() && mLastUserInput.postingWif.isEmpty())
                userProfileState.value = createStateCopyMutatingValue(error = Pair(ErrorCodes.ERROR_AUTH, R.string.posting_or_active_key))
            else if (mLastUserInput.activeWif.isNotEmpty()) {
                mHandler.post({
                    userProfileState.value = UserProfileState(false, true, null,
                            mLastUserInput.login, isScanMenuVisible = true)
                })
                postWithCatch {
                    val resp = mRepository.authWithActiveWif(mLastUserInput.login, mLastUserInput.activeWif)
                    proceedAuthResponse(resp)
                }
            } else if (mLastUserInput.postingWif.isNotEmpty()) {
                mHandler.post({
                    userProfileState.value = UserProfileState(false, true, null,
                            mLastUserInput.login, isScanMenuVisible = true)
                })
                postWithCatch {
                    val resp = mRepository.authWithPostingWif(mLastUserInput.login, mLastUserInput.activeWif)
                    proceedAuthResponse(resp)
                }
            }
        }
    }

    private fun proceedAuthResponse(resp: UserAuthResponse) {
        mHandler.post({
            if (!resp.isKeyValid) {
                userProfileState.value = createStateCopyMutatingValue(isLoggedIn = false,
                        isLoading = false,
                        error = Pair(ErrorCodes.ERROR_AUTH, R.string.wrong_credentials))
            } else {
                userProfileState.value = createStateCopyMutatingValue(isLoggedIn = true,
                        isLoading = false,
                        error = null,
                        userName = resp.userName,
                        avatarPath = resp.avatarPath,
                        userMoney = resp.golosBalance,
                        userCommentsCount = resp.postsCount)
                userAuthState.value = AuthState(true, resp.userName ?: "",
                        resp.postingAuth?.second, resp.activeAuth?.second)
            }
        })
    }

    private fun createStateCopyMutatingValue(
            isLoggedIn: Boolean? = null,
            isLoading: Boolean? = null,
            error: Pair<ErrorCodes, Int>? = null,
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
                userMoney ?: userProfileState.value?.userMoney ?: 0.0,
                isScanMenuVisible ?: userProfileState.value?.isScanMenuVisible == true)
    }

    private fun postWithCatch(action: () -> Unit) {
        mExecutor.execute({
            try {
                action.invoke()
            } catch (e: SteemTimeoutException) {
                mHandler.post({
                    userProfileState.value = createStateCopyMutatingValue(isLoading = false,
                            error = Pair(ErrorCodes.ERROR_SLOW_CONNECTION, R.string.slow_internet_connection))
                })
            } catch (e: SteemCommunicationException) {
                e.printStackTrace()
                mHandler.post({
                    userProfileState.value = createStateCopyMutatingValue(isLoading = false,
                            error = Pair(ErrorCodes.ERROR_NO_CONNECTION, R.string.no_internet_connection))
                })
            } catch (e: SteemConnectionException) {
                e.printStackTrace()
                mHandler.post({
                    userProfileState.value = createStateCopyMutatingValue(isLoading = false,
                            error = Pair(ErrorCodes.ERROR_NO_CONNECTION, R.string.no_internet_connection))
                })
            } catch (e: InvalidParameterException) {
                mHandler.post({
                    userProfileState.value = createStateCopyMutatingValue(isLoading = false,
                            error = Pair(ErrorCodes.ERROR_AUTH, R.string.wrong_credentials))
                })
            } catch (e: Exception) {
                e.printStackTrace()
                mHandler.post({
                    userProfileState.value = createStateCopyMutatingValue(isLoading = false,
                            error = Pair(ErrorCodes.ERROR_NO_CONNECTION, R.string.no_internet_connection))
                })
            }
        })
    }

    fun onLogoutClick() {
        userProfileState.value = UserProfileState(false)
        userAuthState.value = AuthState(false)
    }
}