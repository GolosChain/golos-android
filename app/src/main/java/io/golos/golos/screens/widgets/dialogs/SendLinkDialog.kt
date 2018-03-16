package io.golos.golos.screens.widgets.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import io.golos.golos.R
import io.golos.golos.utils.showKeyboard

/**
 * Created by yuri on 22.11.17.
 */
class SendLinkDialog : android.support.v4.app.DialogFragment() {
    var listener: OnLinkSubmit? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fr_link, container, false)
        val linkName = v.findViewById<EditText>(R.id.name)
        val address = v.findViewById<EditText>(R.id.address)
        val sumbitButton = v.findViewById<View>(R.id.ok_btn)
        sumbitButton.setOnClickListener({
            listener?.submit(linkName.text.toString(), address.text.toString())
            dismiss()
        })
        return v
    }

    companion object {
        fun getInstance(): SendLinkDialog {
            return SendLinkDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        view!!.findViewById<EditText>(R.id.name).postDelayed({
            if (view?.requestFocus() == true) {
                view?.showKeyboard()
            }
        }, 500)

    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}

interface OnLinkSubmit {
    fun submit(linkName: String, linkAddress: String)
}