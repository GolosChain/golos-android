package io.golos.golos.screens.widgets.dialogs

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethod.SHOW_FORCED
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import io.golos.golos.R

/**
 * Created by yuri on 22.11.17.
 */
class SendLinkDialog : android.support.v4.app.DialogFragment(), TextWatcher, DialogInterface {
    override fun cancel() {
        listener?.onDismissLinkDialog()
    }

    override fun afterTextChanged(s: Editable?) {
        sumbitButton.isEnabled = !(linkName.text.isNullOrEmpty() || address.text.isNullOrEmpty())
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    var listener: LinkDialogInterface? = null
    lateinit var linkName: EditText
    lateinit var address: EditText
    lateinit var sumbitButton: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fr_link, container, false)
        linkName = v.findViewById<EditText>(R.id.name)
        linkName.addTextChangedListener(this)
        address = v.findViewById<EditText>(R.id.address)
        address.addTextChangedListener(this)
        sumbitButton = v.findViewById<View>(R.id.ok_btn)
        listener = activity as? LinkDialogInterface
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
        linkName.requestFocus()
        linkName.postDelayed({
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    ?: return@postDelayed

            imm.showSoftInput(linkName, SHOW_FORCED)
        }, 300)

    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        listener?.onDismissLinkDialog()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        dialog?.setOnDismissListener(this)
    }
}

interface LinkDialogInterface {
    fun submit(linkName: String, linkAddress: String)
    fun onDismissLinkDialog()
}