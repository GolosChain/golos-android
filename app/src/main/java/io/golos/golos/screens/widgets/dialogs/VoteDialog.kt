package io.golos.golos.screens.widgets.dialogs

import android.content.res.Resources
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import io.golos.golos.BuildConfig
import io.golos.golos.R
import io.golos.golos.utils.bundleOf
import io.golos.golos.utils.getColorCompat
import io.golos.golos.utils.getVectorDrawable
import io.golos.golos.utils.setViewGone

/**
 * Created by yuri on 13.11.17.
 */
class VoteDialog : GolosDialog() {
    public enum class DialogType {
        UPVOTE, DOWN_VOTE
    }

    interface OnVoteSubmit {
        fun submitVote(id: Long, vote: Short, type: DialogType)
    }

    private val mRepeatingPostHandler = Handler(Looper.getMainLooper())
    private var isAutoIncrement: Boolean = false
    private var isAutoDecrements: Boolean = false
    private lateinit var mSeeker: AppCompatSeekBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fr_vote, container, false)
        val percentTv = v.findViewById<TextView>(R.id.percent_tv)
        if (getDialogType() == DialogType.DOWN_VOTE) percentTv.setTextColor(getColorCompat(R.color.app_red))

        val minusBtn = v.findViewById<ImageView>(R.id.minus_btn)
        val plusBtn = v.findViewById<ImageView>(R.id.plus_btn)
        if (getDialogType() == DialogType.DOWN_VOTE) {
            minusBtn.drawable.setColorFilter(getColorCompat(R.color.app_red), PorterDuff.Mode.SRC_ATOP)
            plusBtn.drawable.setColorFilter(getColorCompat(R.color.app_red), PorterDuff.Mode.SRC_ATOP)
        }
        mSeeker = v.findViewById(R.id.seeker)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT){
            mSeeker.progressDrawable = activity?.getVectorDrawable(R.drawable.ic_line_seeker_vote_progress)
        }
        if (getDialogType() == DialogType.DOWN_VOTE) {
            mSeeker.progressDrawable.setColorFilter(getColorCompat(R.color.app_red), PorterDuff.Mode.SRC_ATOP)
            mSeeker.thumb.setColorFilter(getColorCompat(R.color.app_red), PorterDuff.Mode.SRC_ATOP)
        }
        val voteBtn = v.findViewById<TextView>(R.id.vote_btn)
        if (getDialogType() == DialogType.DOWN_VOTE) {
            voteBtn.setTextColor(getColorCompat(R.color.app_red))
            voteBtn.setText(R.string.vote_against)
        }
        if (getDialogType() == DialogType.UPVOTE) {
            v.findViewById<View>(R.id.p_2).setViewGone()
            v.findViewById<View>(R.id.p_1).setViewGone()
        }

        minusBtn.setOnClickListener {
            val currentProgress = mSeeker.progress
            if (currentProgress > 1) mSeeker.progress -= 1
        }
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
        plusBtn.setOnClickListener { mSeeker.progress += 1 }
        mSeeker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (progress < 1) {
                    mSeeker.progress = 1
                    return
                }
                percentTv.text = "% $progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
        voteBtn.setOnClickListener {
            val progress = mSeeker.progress
            val id = arguments?.getLong("id", Long.MIN_VALUE) ?: return@setOnClickListener
            val type = (arguments?.getSerializable("type") as? VoteDialog.DialogType)
                    ?: return@setOnClickListener
            (parentFragment as? OnVoteSubmit)?.submitVote(id, progress.toShort(), type)
            (activity as? OnVoteSubmit)?.submitVote(id, progress.toShort(), type)
            dismiss()
        }
        return v
    }

    private fun getDialogType(): DialogType = if (arguments?.getSerializable("type") as? DialogType == DialogType.DOWN_VOTE) DialogType.DOWN_VOTE else DialogType.UPVOTE


    override fun onStart() {
        super.onStart()
        val d = dialog
        d?.let {
            d.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    companion object {
        fun getInstance(id: Long, type: DialogType = DialogType.UPVOTE): VoteDialog {
            return VoteDialog().apply {
                arguments = bundleOf("type" to type, "id" to id)
            }
        }
    }

    inner class UpdateRunnable : Runnable {
        override fun run() {
            if (isAutoIncrement) mSeeker.progress += 1
            if (isAutoDecrements && mSeeker.progress > 1) mSeeker.progress -= 1
            mRepeatingPostHandler.postDelayed(UpdateRunnable(), 75)
        }
    }
}

