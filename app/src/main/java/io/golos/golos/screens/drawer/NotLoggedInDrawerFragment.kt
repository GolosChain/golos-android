package io.golos.golos.screens.drawer

import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.SwitchCompat
import android.text.Editable
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.golos.golos.R
import io.golos.golos.screens.androidviewmodel.AuthUserInput
import io.golos.golos.screens.androidviewmodel.AuthViewModel
import io.golos.golos.screens.widgets.BarcodeScannerActivity
import io.golos.golos.screens.widgets.LateTextWatcher
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.hideKeyboard
import io.golos.golos.utils.nextInt
import io.golos.golos.utils.showProgressDialog

/**
 * Created by yuri on 30.10.17.
 */
class NotLoggedInDrawerFragment : Fragment() {
    private val SCAN_POSTING_REQUEST_CODE = nextInt()
    private val SCAN_ACTIVE_REQUEST_CODE = nextInt()
    private val SCAN_ACTIVE_PERMISSION = nextInt()
    private val SCAN_POSTING_PERMISSION = nextInt()
    private lateinit var mKeyEt: EditText
    private lateinit var mPostingEt: EditText
    private lateinit var mLogInEt: EditText
    private lateinit var mActiveKeyEt: EditText
    private lateinit var mScanSwitch: SwitchCompat
    private lateinit var mViewModel: AuthViewModel
    private lateinit var mScanPosting: ImageButton
    private lateinit var mScanActive: ImageButton
    private lateinit var mPostingKeyMenu: ViewGroup
    private lateinit var mActiveKeyMenu: ViewGroup
    private lateinit var mLogoText: TextView
    private lateinit var mMoreAboutGolos: TextView
    private lateinit var mAvatarIv: ImageView
    private lateinit var mRegisterTv: TextView
    private lateinit var mCancelButton: Button
    private lateinit var mEnterButton: Button
    private var mProgressDialog: Dialog? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater!!.inflate(R.layout.fr_auth_user_not_logged_in, container, false)
        mScanSwitch = v.findViewById(R.id.scan_switch)
        mPostingKeyMenu = v.findViewById(R.id.posting_key_lo)
        mActiveKeyMenu = v.findViewById(R.id.active_key_lo)
        mScanPosting = v.findViewById(R.id.scan_posting)
        mScanActive = v.findViewById(R.id.scan_active)
        mKeyEt = v.findViewById(R.id.key_et)
        mLogInEt = v.findViewById(R.id.login_et)
        mLogoText = v.findViewById(R.id.logo_text)
        mAvatarIv = v.findViewById(R.id.avatar_iv)
        mEnterButton = v.findViewById(R.id.enter_btn)

        val postingKeyRequest = View.OnClickListener {
            val camerPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (camerPermission) {
                val intent = Intent(context, BarcodeScannerActivity::class.java)
                startActivityForResult(intent, SCAN_POSTING_REQUEST_CODE)
            } else {
                ActivityCompat.requestPermissions(activity, Array(1, { android.Manifest.permission.CAMERA }), SCAN_POSTING_PERMISSION)
            }

        }
        val activeKeyRequest = View.OnClickListener {
            val camerPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (camerPermission) {
                val intent = Intent(context, BarcodeScannerActivity::class.java)
                startActivityForResult(intent, SCAN_ACTIVE_REQUEST_CODE)
            } else {
                ActivityCompat.requestPermissions(activity, Array(1, { android.Manifest.permission.CAMERA }), SCAN_ACTIVE_PERMISSION)
            }
        }
        mPostingEt = v.findViewById(R.id.posting_key_et)
        mScanPosting.setOnClickListener(postingKeyRequest)

        mScanActive.setOnClickListener(activeKeyRequest)
        mActiveKeyEt = v.findViewById(R.id.barcode_active_key_et)
        mMoreAboutGolos = v.findViewById(R.id.more_about_golos)
        mRegisterTv = v.findViewById(R.id.register_tv)
        mCancelButton = v.findViewById(R.id.cancel_btn)
        mMoreAboutGolos.text = Html.fromHtml(container!!.resources.getString(R.string.more_about_golos))
        mRegisterTv.text = Html.fromHtml(container!!.resources.getString(R.string.do_not_registered_register))
        mMoreAboutGolos.movementMethod = LinkMovementMethod.getInstance()
        mRegisterTv.movementMethod = LinkMovementMethod.getInstance()
        return v
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = ViewModelProviders.of(activity).get(AuthViewModel::class.java)
        mViewModel.userProfileState.observe(this, android.arch.lifecycle.Observer {
            if (it?.isScanMenuVisible == true) {
                mKeyEt.visibility = View.GONE
                mPostingKeyMenu.visibility = View.VISIBLE
                mActiveKeyMenu.visibility = View.VISIBLE
            } else {
                mKeyEt.visibility = View.VISIBLE
                mPostingKeyMenu.visibility = View.GONE
                mActiveKeyMenu.visibility = View.GONE
            }
            mLogInEt.setText(it?.userName)
            mProgressDialog?.let {
                it.dismiss()
                mProgressDialog = null
            }
            if (it?.isLoading == true) {
                mProgressDialog = showProgressDialog()
                view?.findFocus()?.let { context?.hideKeyboard(it) }
            }
            if (it?.error != null) {
                var message = R.string.unknown_error
                when (it.error.first) {
                    ErrorCode.ERROR_AUTH -> message = it.error.second
                }
                view?.findFocus()?.let { context?.hideKeyboard(it) }

                Snackbar.make(mLogInEt,
                        Html.fromHtml("<font color=\"#ffffff\">${resources.getString(message)}</font>"),
                        Toast.LENGTH_SHORT).show()
            }
        })
        mScanSwitch.setOnCheckedChangeListener { _, isChecked ->
            mViewModel.onScanSwitch(isChecked)
        }
        mLogInEt.addTextChangedListener(object : LateTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mViewModel.onUserInput(collectInputData())
            }
        })

        mKeyEt.addTextChangedListener(object : LateTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mViewModel.onUserInput(collectInputData());
            }
        })
        mKeyEt.setOnEditorActionListener({ _, _, _ ->
            mViewModel.onLoginClick()
            true
        })
        mPostingEt.addTextChangedListener(object : LateTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mViewModel.onUserInput(collectInputData())
            }
        })
        mPostingEt.setOnEditorActionListener({ _, _, _ ->
            mViewModel.onLoginClick()
            true
        })
        mActiveKeyEt.setOnEditorActionListener({ _, _, _ ->
            mViewModel.onLoginClick()
            true
        })
        mActiveKeyEt.addTextChangedListener(object : LateTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                mViewModel.onUserInput(collectInputData())
            }
        })
        mCancelButton.setOnClickListener({ mViewModel.onCancelClick() })
        mEnterButton.setOnClickListener({ mViewModel.onLoginClick() })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return
        if (resultCode == Activity.RESULT_OK) {
            val out = data.getStringExtra(BarcodeScannerActivity.SCAN_RESULT_TAG)
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

    private fun collectInputData(): AuthUserInput {
        return AuthUserInput(mLogInEt.text.toString(),
                mKeyEt.text.toString(),
                mPostingEt.text.toString(),
                mActiveKeyEt.text.toString())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                SCAN_ACTIVE_PERMISSION -> {
                    mActiveKeyEt.performClick()
                }
                SCAN_POSTING_PERMISSION -> {
                    mPostingEt.performClick()
                }
            }
        }
    }

    companion object {
        fun getInstance(): NotLoggedInDrawerFragment {
            val fr = NotLoggedInDrawerFragment()
            return fr
        }
    }
}