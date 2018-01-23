package io.golos.golos.screens.profile


import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R

/**
 * Created by yuri on 23.01.18.
 */
class LoginHelperFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fr_login_help, container, false)
    }


    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    companion object {
        fun getInstance(): LoginHelperFragment {
            return LoginHelperFragment()
        }
    }
}