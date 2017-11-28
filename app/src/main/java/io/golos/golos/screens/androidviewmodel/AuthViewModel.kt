package io.golos.golos.screens.androidviewmodel

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
import io.golos.golos.utils.ErrorCode
import org.bitcoinj.core.AddressFormatException
import java.security.InvalidParameterException

data class UserProfileState(val isLoggedIn: Boolean,
                            val isLoading: Boolean = false,
                            val error: Pair<ErrorCode, Int>? = null,
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
        val userData = mRepository.getSavedActiveUserData()
        if (userData != null && (userData.privateActiveWif != null || userData.privatePostingWif != null)) {
            initUser()
        } else {
            userProfileState.value = UserProfileState(isLoggedIn = false, isScanMenuVisible = false)
            userAuthState.value = AuthState(isLoggedIn = false)
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
        onCloseDrawerRequest.value = null
    }

    fun onLoginClick() {
        if (mLastUserInput.login.isEmpty()) {
            userProfileState.value = createStateCopyMutatingValue(error = Pair(ErrorCode.ERROR_AUTH, R.string.enter_login))
        } else if (userProfileState.value?.isScanMenuVisible == false) {
            if (mLastUserInput.masterKey.isEmpty()) {
                userProfileState.value = createStateCopyMutatingValue(error = Pair(ErrorCode.ERROR_AUTH, R.string.enter_password))
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
                userProfileState.value = createStateCopyMutatingValue(error = Pair(ErrorCode.ERROR_AUTH, R.string.posting_or_active_key))
            else if (mLastUserInput.activeWif.isNotEmpty()) {
                mHandler.post({
                    userProfileState.value = UserProfileState(false, true, null,
                            mLastUserInput.login, isScanMenuVisible = true)
                })
                postWithCatch {
                    val resp = mRepository.authWithActiveWif(mLastUserInput.login, mLastUserInput.activeWif)
                    mHandler.post {
                        userProfileState.value = UserProfileState(isLoggedIn = true,
                                userName = resp.userName ?: "",
                                avatarPath = resp.avatarPath)
                        userAuthState.value = AuthState(isLoggedIn = true)
                    }

                }
            } else if (mLastUserInput.postingWif.isNotEmpty()) {
                mHandler.post({
                    userProfileState.value = UserProfileState(false, true, null,
                            mLastUserInput.login, isScanMenuVisible = true)
                })
                postWithCatch {
                    val resp = mRepository.authWithPostingWif(mLastUserInput.login, mLastUserInput.postingWif)
                    mHandler.post {
                        userProfileState.value = UserProfileState(isLoggedIn = true,
                                userName = resp.userName ?: "",
                                avatarPath = resp.avatarPath)
                        userAuthState.value = AuthState(isLoggedIn = true)
                    }
                }
            }
        }
    }

    private fun proceedAuthResponse(resp: UserAuthResponse) {
        mHandler.post({
            if (!resp.isKeyValid) {
                userProfileState.value = createStateCopyMutatingValue(isLoggedIn = false,
                        isLoading = false,
                        error = Pair(ErrorCode.ERROR_AUTH, R.string.wrong_credentials))
            } else {
                initUser()
            }
        })
    }

    private fun createStateCopyMutatingValue(
            isLoggedIn: Boolean? = null,
            isLoading: Boolean? = null,
            error: Pair<ErrorCode, Int>? = null,
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
            } catch (e: AddressFormatException) {
                mHandler.post({
                    userProfileState.value = createStateCopyMutatingValue(isLoading = false,
                            error = Pair(ErrorCode.UNKNOWN, R.string.unknown_error))
                })
            } catch (e: SteemTimeoutException) {
                mHandler.post({
                    userProfileState.value = createStateCopyMutatingValue(isLoading = false,
                            error = Pair(ErrorCode.ERROR_SLOW_CONNECTION, R.string.slow_internet_connection))
                })
            } catch (e: SteemCommunicationException) {
                e.printStackTrace()
                mHandler.post({
                    userProfileState.value = createStateCopyMutatingValue(isLoading = false,
                            error = Pair(ErrorCode.ERROR_NO_CONNECTION, R.string.no_internet_connection))
                })
            } catch (e: SteemConnectionException) {
                e.printStackTrace()
                mHandler.post({
                    userProfileState.value = createStateCopyMutatingValue(isLoading = false,
                            error = Pair(ErrorCode.ERROR_NO_CONNECTION, R.string.no_internet_connection))
                })
            } catch (e: InvalidParameterException) {
                mHandler.post({
                    userProfileState.value = createStateCopyMutatingValue(isLoading = false,
                            error = Pair(ErrorCode.ERROR_AUTH, R.string.wrong_credentials))
                })
            } catch (e: Exception) {
                e.printStackTrace()
                mHandler.post({
                    userProfileState.value = createStateCopyMutatingValue(isLoading = false,
                            error = Pair(ErrorCode.ERROR_NO_CONNECTION, R.string.no_internet_connection))
                })
            }
        })
    }

    private fun initUser() {
        val userData = mRepository.getSavedActiveUserData()
        if (userData != null && (userData.privateActiveWif != null || userData.privatePostingWif != null)) {
            userProfileState.value = UserProfileState(isLoggedIn = true, userName = userData.userName, avatarPath = userData.avatarPath)
            userAuthState.value = AuthState(isLoggedIn = true)
            mRepository.setActiveUserAccount(userData.userName, userData.privateActiveWif, userData.privatePostingWif)
            postWithCatch {
                val data = mRepository.getAccountData(userData.userName)
                mHandler.post({
                    userProfileState.value = UserProfileState(isLoggedIn = true, isLoading = false, userName = data.userName,
                            avatarPath = data.avatarPath, userMoney = data.golosCount, userPostsCount = data.postsCount)
                })
            }
        } else {
            mHandler.post {
                userProfileState.value = UserProfileState(isLoggedIn = false, isLoading = false, userName = mLastUserInput.login,
                        error = Pair(ErrorCode.ERROR_AUTH, R.string.wrong_credentials))
            }
        }
    }

    fun onLogoutClick() {
        mRepository.deleteUserdata()
        userProfileState.value = UserProfileState(false)
        userAuthState.value = AuthState(false)
    }
}