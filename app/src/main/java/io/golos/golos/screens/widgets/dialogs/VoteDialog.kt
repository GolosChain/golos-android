package io.golos.golos.screens.widgets.dialogs

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import io.golos.golos.R
import io.golos.golos.utils.bundleOf

/**
 * Created by yuri on 13.11.17.
 */
class VoteDialog : GolosDialog() {
    var selectPowerListener: OnVoteSubmit? = null
    private val mRepeatingPostHandler = Handler(Looper.getMainLooper())
    private var isAutoIncrement: Boolean = false
    private var isAutoDecrements: Boolean = false
    private lateinit var mSeeker: AppCompatSeekBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fr_vote, container, false)
        val percentTv = v.findViewById<TextView>(R.id.percent_tv)
        val minusBtn = v.findViewById<View>(R.id.minus_btn)
        val plusBtn = v.findViewById<View>(R.id.plus_btn)
        mSeeker = v.findViewById(R.id.seeker)
        val voteBtn = v.findViewById<Button>(R.id.vote_btn)
        minusBtn.setOnClickListener({ mSeeker.progress -= 1 })
        minusBtn.setOnLongClickListener {
            this@VoteDialog.isAutoDecrements = true

            false
        }
        minusBtn.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL) isAutoDecrements = false
            false
        }
        plusBtn.setOnLongClickListener {
            this@VoteDialog.isAutoIncrement = true
            false
        }
        plusBtn.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP || e.action == MotionEvent.ACTION_CANCEL) isAutoIncrement = false
            false
        }
        mRepeatingPostHandler.post(UpdateRunnable())
        plusBtn.setOnClickListener({ mSeeker.progress += 1 })
        mSeeker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                percentTv.text = "% $progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        voteBtn.setOnClickListener {
            selectPowerListener?.submitVote(mSeeker.progress.toShort())
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
        fun getInstance(type: DialogType = DialogType.UPVOTE): VoteDialog {
            return VoteDialog().apply {
                arguments = bundleOf("type" to type)
            }
        }
    }

    inner class UpdateRunnable : Runnable {
        override fun run() {
            if (isAutoIncrement) mSeeker.progress += 1
            if (isAutoDecrements) mSeeker.progress -= 1
            mRepeatingPostHandler.postDelayed(UpdateRunnable(), 75)
        }
    }
}

public enum class DialogType {
    UPVOTE, DOWN_VOTE
}

interface OnVoteSubmit {
    fun submitVote(vote: Short)
}