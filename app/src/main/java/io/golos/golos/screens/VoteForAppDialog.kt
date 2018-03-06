package io.golos.golos.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.repository.UserSettingsImpl
import io.golos.golos.screens.widgets.dialogs.GolosDialog


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
            Repository.get.userSettingsRepository.setUserVotedForApp(true)
            dismiss()
        }
        v.findViewById<View>(R.id.never).setOnClickListener {
            Repository.get.userSettingsRepository.setUserVotedForApp(true)
            dismiss()
        }
        Repository.get.userSettingsRepository.setVoteQuestionMade(true)
        return v
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(android.support.v4.app.DialogFragment.STYLE_NO_FRAME, R.style.Theme_AppCompat_Dialog)
    }

    companion object {
        fun getInstance() = VoteForAppDialog()
    }
}