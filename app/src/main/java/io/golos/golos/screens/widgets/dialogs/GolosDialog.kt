package io.golos.golos.screens.widgets.dialogs

import android.os.Bundle
import android.support.v4.app.DialogFragment
import io.golos.golos.R

/**
 * Created by yuri on 22.02.18.
 */
open class GolosDialog : DialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(android.support.v4.app.DialogFragment.STYLE_NO_FRAME, R.style.GolosDialogTheme)
    }
}