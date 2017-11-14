package io.golos.golos.screens.widgets

import android.text.TextWatcher

/**
 * Created by yuri on 30.10.17.
 */
abstract class LateTextWatcher: TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }
}