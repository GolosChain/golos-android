package io.golos.golos.screens.widgets.dialogs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import io.golos.golos.R
import io.golos.golos.utils.getVectorDrawable

/**
 * Created by yuri yurivladdurain@gmail.com on 19/12/2018.
 */
abstract class GolosDialogRoundedCorners : GolosDialog() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.background = activity!!.getVectorDrawable(R.drawable.background_vote_for_app_dialog)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.VoteForAppDialogStyle)

    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}