package io.golos.golos.screens.widgets.dialogs

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import io.golos.golos.R

/**
 * Created by yuri on 22.02.18.
 */
open class GolosDialog : DialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.GolosDialogTheme)
    }
}