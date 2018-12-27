package io.golos.golos.screens.widgets.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.utils.bundleOf
import io.golos.golos.utils.toN

/**
 * Created by yuri yurivladdurain@gmail.com on 21/12/2018.
 */
class MasterPasswordWarningDialog : GolosDialogRoundedCorners() {

    interface OnAcceptListener {
        fun onAccept(id: Long)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.d_master_password_acc, container, false)
        v.findViewById<View>(R.id.cancel_btn).setOnClickListener { dismiss() }
        v.findViewById<View>(R.id.ok_btn).setOnClickListener {
            val id = arguments?.getLong("id", Long.MIN_VALUE) ?: return@setOnClickListener dismiss()
            (parentFragment as? OnAcceptListener)?.onAccept(id)
            (activity as? OnAcceptListener)?.onAccept(id)
            dismiss()
        }
        return v
    }

    companion object {
        fun getInstance(id: Long): MasterPasswordWarningDialog {
            return MasterPasswordWarningDialog().apply {
                arguments = bundleOf("id" toN id)
            }
        }
    }
}