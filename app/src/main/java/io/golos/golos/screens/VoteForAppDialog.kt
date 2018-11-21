package io.golos.golos.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.widget.LinearLayoutCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import io.golos.golos.R
import io.golos.golos.screens.widgets.dialogs.GolosDialog
import io.golos.golos.utils.getLayoutInflater
import io.golos.golos.utils.userVotedForApp
import io.golos.golos.utils.isVoteQuestionMade


/**
 * Created by yuri on 07.02.18.
 */
class VoteForAppDialog : GolosDialog() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fr_vote_for_app, container, false)
        v.findViewById<View>(R.id.not_now).setOnClickListener {
            dismiss()
        }
        v.findViewById<View>(R.id.vote).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=io.golos.golos")))
            } catch (anfe: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=io.golos.golos")))
            }
            activity?.userVotedForApp = true
            dismiss()
        }
        v.findViewById<View>(R.id.never).setOnClickListener {
            activity?.userVotedForApp = true
            dismiss()
        }
        activity?.isVoteQuestionMade  = true
        return v
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.VoteForAppDialogStyle)

    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    companion object {
        fun getInstance() = VoteForAppDialog()
    }
}

public class StarsRow @JvmOverloads constructor(context: Context,
                                         attributeSet: AttributeSet? = null,
                                         defStyleAttr: Int = 0) : LinearLayoutCompat(context, attributeSet, defStyleAttr) {

    init {
        getLayoutInflater().inflate(R.layout.v_stars, this, true)
        orientation = HORIZONTAL
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        return super.onTouchEvent(event)
    }
}