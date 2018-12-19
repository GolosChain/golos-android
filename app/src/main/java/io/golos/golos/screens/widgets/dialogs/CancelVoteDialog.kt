package io.golos.golos.screens.widgets.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import io.golos.golos.R
import io.golos.golos.utils.bundleOf

/**
 * Created by yuri yurivladdurain@gmail.com on 17/10/2018.
 */
class CancelVoteDialog : GolosDialog() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.d_vote_cancel, container, false)
        v.findViewById<View>(R.id.cancel_btn).setOnClickListener { dismiss() }
        v.findViewById<View>(R.id.change_vote_btn).setOnClickListener {
            (parentFragment as? OnChangeConfirmal)?.onChangeConfirm(arguments?.getLong("id", Long.MIN_VALUE)
                    ?: return@setOnClickListener)
            (activity as? OnChangeConfirmal)?.onChangeConfirm(arguments?.getLong("id", Long.MIN_VALUE)
                    ?: return@setOnClickListener)
            dismiss()
        }
        return v
    }

    override fun onStart() {
        super.onStart()
        val d = dialog
        d?.let {
            d.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    companion object {
        fun getInstance(id: Long): DialogFragment {
            return CancelVoteDialog().apply { arguments = bundleOf("id" to id) }
        }
    }

    interface OnChangeConfirmal {
        fun onChangeConfirm(id: Long)
    }
}