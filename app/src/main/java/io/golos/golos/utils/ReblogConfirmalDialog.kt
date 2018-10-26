package io.golos.golos.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import io.golos.golos.R
import io.golos.golos.screens.widgets.dialogs.GolosDialog
import timber.log.Timber

/**
 * Created by yuri yurivladdurain@gmail.com on 19/10/2018.
 */
class ReblogConfirmalDialog : GolosDialog() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.d_reblog_question, container, false)
        Timber.e("onCreateView")
        v.findViewById<View>(R.id.reblog_btn).setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                Timber.e("clicked")
                (parentFragment as? OnReblogConfirmed)?.oReblogConfirmed(arguments?.getLong("id", Long.MIN_VALUE)
                        ?: return)
                (activity as? OnReblogConfirmed)?.oReblogConfirmed(arguments?.getLong("id", Long.MIN_VALUE)
                        ?: return)
                dismiss()
            }
        })
        v.findViewById<View>(R.id.cancel_btn).setOnClickListener {
            Timber.e("clicke cancel")
            dismiss()
        }

        return v
    }

    override fun onStart() {
        super.onStart()
        val d = dialog
        d?.let {
            d.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    companion object {
        fun getInstance(id: Long): DialogFragment {
            return ReblogConfirmalDialog().apply { arguments = bundleOf("id" toN id) }
        }
    }

    interface OnReblogConfirmed {
        fun oReblogConfirmed(id: Long)
    }
}