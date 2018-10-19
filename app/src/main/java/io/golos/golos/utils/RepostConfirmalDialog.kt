package io.golos.golos.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import io.golos.golos.R
import io.golos.golos.screens.widgets.dialogs.GolosDialog

/**
 * Created by yuri yurivladdurain@gmail.com on 19/10/2018.
 */
class RepostConfirmalDialog() : GolosDialog() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.d_reblog_question, container, false)
        v.findViewById<View>(R.id.reblog_btn).setOnClickListener {
            (parentFragment as? OnRepostConfirmed)?.onConfirmed(arguments?.getLong("id", Long.MIN_VALUE)
                    ?: return@setOnClickListener)
            (activity as? OnRepostConfirmed)?.onConfirmed(arguments?.getLong("id", Long.MIN_VALUE)
                    ?: return@setOnClickListener)
            dismiss()
        }
        v.findViewById<View>(R.id.cancel_btn).setOnClickListener { dismiss() }
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
            return RepostConfirmalDialog().apply { arguments = bundleOf("id" toN id) }
        }
    }

    interface OnRepostConfirmed {
        fun onConfirmed(id: Long)
    }
}