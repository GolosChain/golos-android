package io.golos.golos.screens.profile

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import io.golos.golos.R
import io.golos.golos.screens.profile.viewmodel.AuthUserInput
import io.golos.golos.screens.profile.viewmodel.AuthViewModel
import io.golos.golos.screens.profile.viewmodel.LoginOptions
import io.golos.golos.screens.widgets.BarcodeScannerActivity
import io.golos.golos.screens.widgets.LateTextWatcher
import io.golos.golos.screens.widgets.dialogs.MasterPasswordWarningDialog
import io.golos.golos.utils.*

/**
 * Created by yuri on 30.10.17.
 */
class AuthFragment : Fragment(), MasterPasswordWarningDialog.OnAcceptListener {
    private val SCAN_POSTING_REQUEST_CODE = nextInt()
    private val SCAN_ACTIVE_REQUEST_CODE = nextInt()
    private val SCAN_ACTIVE_PERMISSION = nextInt()
    private val SCAN_POSTING_PERMISSION = nextInt()
    private lateinit var mPostingEt: EditText
    private lateinit var mMasterEt: EditText
    private lateinit var mLogInEt: EditText
    private lateinit var mActiveKeyEt: EditText
    private lateinit var mViewModel: AuthViewModel
    private lateinit var mScanPosting: ImageButton
    private lateinit var mScanActive: ImageButton
    private lateinit var mPostingKeyMenu: ViewGroup
    private lateinit var mMasterKeyMenu: ViewGroup
    private lateinit var mActiveKeyMenu: ViewGroup
    private lateinit var mMoreAboutGolos: TextView
    private lateinit var mRegisterTv: TextView
    private lateinit var mCancelButton: Button
    private lateinit var mLoginWithPosting: View
    private lateinit var mLoginWithActive: View
    private lateinit var mLoginWithMaster: View
    private lateinit var mEnterButton: Button
    private var mProgressDialog: Dialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fr_auth_user_not_logged_in, container, false)
        mPostingKeyMenu = v.findViewById(R.id.posting_key_lo)
        mMasterKeyMenu = v.findViewById(R.id.master_key_lo)
        mLoginWithPosting = v.findViewById(R.id.posting_key_og_btn)
        mLoginWithActive = v.findViewById(R.id.active_key_og_btn)
        mLoginWithMaster = v.findViewById(R.id.master_key_log_btn)
        mActiveKeyMenu = v.findViewById(R.id.active_key_lo)
        mScanPosting = v.findViewById(R.id.scan_posting)
        mScanActive = v.findViewById(R.id.scan_active)
        mLogInEt = v.findViewById(R.id.login_et)
        mEnterButton = v.findViewById(R.id.enter_btn)

        val postingKeyRequest = View.OnClickListener {
            val camerPermission = context?.let { it1 -> ContextCompat.checkSelfPermission(it1, android.Manifest.permission.CAMERA) } == PackageManager.PERMISSION_GRANTED
            if (camerPermission) {
                val intent = Intent(context, BarcodeScannerActivity::class.java)
                startActivityForResult(intent, SCAN_POSTING_REQUEST_CODE)
            } else {
                activity?.let { it1 -> ActivityCompat.requestPermissions(it1, Array(1, { android.Manifest.permission.CAMERA }), SCAN_POSTING_PERMISSION) }
            }

        }
        val activeKeyRequest = View.OnClickListener {
            val camerPermission = context?.let { it1 -> ContextCompat.checkSelfPermission(it1, android.Manifest.permission.CAMERA) } == PackageManager.PERMISSION_GRANTED
            if (camerPermission) {
                val intent = Intent(context, BarcodeScannerActivity::class.java)
                startActivityForResult(intent, SCAN_ACTIVE_REQUEST_CODE)
            } else {
                activity?.let { it1 -> ActivityCompat.requestPermissions(it1, Array(1, { android.Manifest.permission.CAMERA }), SCAN_ACTIVE_PERMISSION) }
            }
        }
        mPostingEt = v.findViewById(R.id.posting_key_et)
        mScanPosting.setOnClickListener(postingKeyRequest)

        mScanActive.setOnClickListener(activeKeyRequest)
        mActiveKeyEt = v.findViewById(R.id.barcode_active_key_et)
        mMasterEt = v.findViewById(R.id.master_key_et)
        mMoreAboutGolos = v.findViewById(R.id.more_about_golos)
        mRegisterTv = v.findViewById(R.id.register_tv)
        mCancelButton = v.findViewById(R.id.cancel_btn)
        mMoreAboutGolos.text = container!!.resources.getString(R.string.more_about_golos).toHtml()
        mRegisterTv.text = container.resources.getString(R.string.do_not_registered_register).toHtml()
        mMoreAboutGolos.movementMethod = GolosMovementMethod.instance
        mRegisterTv.movementMethod = GolosMovementMethod.instance
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = ViewModelProviders.of(activity!!).get(AuthViewModel::class.java)
        mViewModel.userProfileState.observe(this, androidx.lifecycle.Observer {
            it ?: return@Observer

            when (it.keyOptions) {
                LoginOptions.POSTING_KEY -> {
                    mPostingKeyMenu.setViewVisible()
                    mActiveKeyMenu.setViewGone()
                    mMasterKeyMenu.setViewGone()

                    mLoginWithPosting.setViewGone()
                    mLoginWithMaster.setViewVisible()
                    mLoginWithActive.setViewVisible()
                    mPostingEt.post { mPostingEt.showKeyboard() }
                }
                LoginOptions.ACTIVE_KEY -> {
                    mPostingKeyMenu.setViewGone()
                    mActiveKeyMenu.setViewVisible()
                    mMasterKeyMenu.setViewGone()

                    mLoginWithPosting.setViewVisible()
                    mLoginWithMaster.setViewVisible()
                    mLoginWithActive.setViewGone()
                    mActiveKeyEt.post { mActiveKeyEt.showKeyboard() }

                }
                LoginOptions.MASTER_KEY -> {
                    mPostingKeyMenu.setViewGone()
                    mActiveKeyMenu.setViewGone()
                    mMasterKeyMenu.setViewVisible()

                    mLoginWithPosting.setViewVisible()
                    mLoginWithMaster.setViewGone()
                    mLoginWithActive.setViewVisible()
                    mMasterEt.post { mMasterEt.showKeyboard() }
                }
            }

            val pd = mProgressDialog
            if (pd != null) {
                pd.dismiss()
                mProgressDialog = null
            }

            if (it.isLoading) {

                mProgressDialog = showProgressDialog()
                view.findFocus()?.let { context?.hideKeyboard(it) }
            }
            if (it.error != null) {
                view.findFocus()?.let { context?.hideKeyboard(it) }
                Snackbar.make(mLogInEt,
                        Html.fromHtml("<font color=\"#ffffff\">${resources.getString(it.error.localizedMessage
                                ?: 0)}</font>"),
                        Snackbar.LENGTH_SHORT).show()
            }
        })
        mLoginWithPosting.setOnClickListener {
            mViewModel.onChangeKeyTypeClick(LoginOptions.POSTING_KEY)
        }
        mLoginWithMaster.setOnClickListener {
            mViewModel.onChangeKeyTypeClick(LoginOptions.MASTER_KEY)
        }
        mLoginWithActive.setOnClickListener {
            mViewModel.onChangeKeyTypeClick(LoginOptions.ACTIVE_KEY)
        }
        mLogInEt.addTextChangedListener(object : LateTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mViewModel.onUserInput(collectInputData())
            }
        })
        mPostingEt.addTextChangedListener(object : LateTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mViewModel.onUserInput(collectInputData())
            }
        })
        mMasterEt.addTextChangedListener(object : LateTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mViewModel.onUserInput(collectInputData())
            }
        })
        mPostingEt.setOnEditorActionListener { _, _, _ ->
            mViewModel.onLoginClick()
            true
        }
        mActiveKeyEt.setOnEditorActionListener { _, _, _ ->
            mViewModel.onLoginClick()
            true
        }
        mMasterEt.setOnEditorActionListener { _, _, _ ->
            mViewModel.onLoginClick()
            true
        }
        mActiveKeyEt.addTextChangedListener(object : LateTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mViewModel.onUserInput(collectInputData())
            }
        })
        mCancelButton.setOnClickListener { mViewModel.onCancelClick() }
        mEnterButton.setOnClickListener { mViewModel.onLoginClick() }
        view.findViewById<View>(R.id.login_help_posting)?.setOnClickListener {
            LoginHelperFragment
                    .getInstance(LoginHelpType.FOR_POSTING_KEY)
                    .show(activity?.supportFragmentManager ?: return@setOnClickListener, null)
        }
        view.findViewById<View>(R.id.login_help_active)?.setOnClickListener {
            LoginHelperFragment
                    .getInstance(LoginHelpType.FOR_ACTIVE_KEY)
                    .show(activity?.supportFragmentManager ?: return@setOnClickListener, null)
        }
        mViewModel.masterPassDialogLiveData.observe(this, Observer {
            it ?: return@Observer
            MasterPasswordWarningDialog.getInstance(0L).show(childFragmentManager, null)
        })
    }

    override fun onAccept(id: Long) {
        mViewModel.onMasterKeyConfirmed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val resultCode = PreferenceManager.getDefaultSharedPreferences(activity
                ?: return).getInt(BarcodeScannerActivity.LAST_RESULT_CODE, Activity.RESULT_CANCELED)
        val out = PreferenceManager.getDefaultSharedPreferences(activity).getString(BarcodeScannerActivity.LAST_RESULT_TEXT, null)
        //fix of a problem for some old samsung phones

        if (resultCode == Activity.RESULT_OK) {
            if (out == "http://") {
                view?.showSnackbar(R.string.scan_fail)
            } else {
                when (requestCode) {
                    SCAN_POSTING_REQUEST_CODE -> {

                        mPostingEt.setText(out)
                    }
                    SCAN_ACTIVE_REQUEST_CODE -> {
                        mActiveKeyEt.setText(out)
                    }
                }
            }
        }
    }

    private fun collectInputData(): AuthUserInput {
        return AuthUserInput(mLogInEt.text.toString(),
                mMasterEt.text.toString(),
                mPostingEt.text.toString(),
                mActiveKeyEt.text.toString())
    }

    override fun onDestroy() {
        if (mProgressDialog != null) mProgressDialog!!.dismiss()
        super.onDestroy()

    }

    override fun onDestroyView() {
        activity?.hideKeyboard(activity?.currentFocus ?: return super.onDestroyView())
        super.onDestroyView()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                SCAN_ACTIVE_PERMISSION -> {
                    mScanActive.performClick()
                }
                SCAN_POSTING_PERMISSION -> {
                    mScanPosting.performClick()
                }
            }
        }
    }

    companion object {
        fun getInstance(): AuthFragment {
            val fr = AuthFragment()
            return fr
        }
    }
}