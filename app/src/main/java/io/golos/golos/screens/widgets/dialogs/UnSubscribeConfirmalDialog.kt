package io.golos.golos.screens.widgets.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.utils.bundleOf
import io.golos.golos.utils.toN

/**
 * Created by yuri yurivladdurain@gmail.com on 19/12/2018.
 */
class UnSubscribeConfirmalDialog : GolosDialogRoundedCorners() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.d_unsubscribe, container, false)
        v.findViewById<TextView>(R.id.lable).text = v.resources.getString(R.string.subscribe_confirmation, arguments?.getString(NAME_TAG, null).orEmpty())
        v.findViewById<View>(R.id.cancel_btn).setOnClickListener { dismiss() }
        v.findViewById<View>(R.id.subscriprion_cancel_btn).setOnClickListener {
            val name = arguments?.getString(NAME_TAG, null)
            if (name == null) {
                dismiss()
                return@setOnClickListener
            }
            (activity as? OnUserUnsubscriptionConfirm)?.onUnsubscriptionConfirmed(name)
            (parentFragment as? OnUserUnsubscriptionConfirm)?.onUnsubscriptionConfirmed(name)
            dismiss()
        }
        return v
    }

    companion object {
        private const val NAME_TAG = "name_tag"

        fun getInstance(forUser: String): UnSubscribeConfirmalDialog {
            return UnSubscribeConfirmalDialog().apply {
                arguments = bundleOf(NAME_TAG toN forUser)
            }
        }
    }

    interface OnUserUnsubscriptionConfirm {
        fun onUnsubscriptionConfirmed(userName: String)
    }
}