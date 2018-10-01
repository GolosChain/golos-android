package io.golos.golos.screens.tags

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.screens.tags.model.LocalizedTag

/**
 * Created by yuri on 15.01.18.
 */

class TagSubscriptionCancelDialogFr : DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.f_subscription_cancel, container, false)
        if (arguments?.get(TAG_TAG) != null) {
            val localizedTag = arguments!!.getParcelable<LocalizedTag>(TAG_TAG)
            v.findViewById<TextView>(R.id.cancel_lbl).text = getString(R.string.cancel_sub_onTag, localizedTag.getLocalizedName())
            v.findViewById<View>(R.id.cancel_tv).setOnClickListener {
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_CANCELED, null)
                (activity as? ResultListener)?.onCancelConfirm()
                dismiss()
            }
            v.findViewById<View>(R.id.dismiss_tv).setOnClickListener {
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, null)
                (activity as? ResultListener)?.onCancelCancel()
                dismiss()
            }
        }
        return v
    }

    companion object {
        private val TAG_TAG = "TAG_TAG"
        fun getInstance(tag: LocalizedTag): TagSubscriptionCancelDialogFr {
            val f = TagSubscriptionCancelDialogFr()
            val b = Bundle()
            b.putParcelable(TAG_TAG, tag)
            f.arguments = b
            return f
        }
    }

    interface ResultListener {
        fun onCancelConfirm()
        fun onCancelCancel()
    }
}

