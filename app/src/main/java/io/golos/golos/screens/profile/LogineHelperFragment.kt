package io.golos.golos.screens.profile


import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.utils.toHtml

/**
 * Created by yuri on 23.01.18.
 */
enum class LoginHelpType {
    FOR_POSTING_KEY, FOR_ACTIVE_KEY
}

class LoginHelperFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fr_login_help, container, false)
        val goToSiteTv = v.findViewById<TextView>(R.id.go_to_site_tv)
        goToSiteTv.text = getString(R.string.help_step_1).toHtml()
        goToSiteTv.movementMethod = LinkMovementMethod.getInstance()
        v.findViewById<TextView>(R.id.key_type_lbl).text = if (arguments?.getSerializable(TYPE_TAG) == LoginHelpType.FOR_POSTING_KEY)
            getString(R.string.help_step_3_posting) else getString(R.string.help_step_3_active)
        v.findViewById<View>(R.id.finish_btn).setOnClickListener { dismiss() }
        return v
    }


    override fun onStart() {
        super.onStart()
        isCancelable
        dialog.window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    companion object {
        private val TYPE_TAG = "TYPE_TAG"
        fun getInstance(type: LoginHelpType): LoginHelperFragment {
            val b = Bundle()
            b.putSerializable(TYPE_TAG, type)
            val f = LoginHelperFragment()
            f.arguments = b
            return f
        }
    }
}