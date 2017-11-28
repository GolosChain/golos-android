package io.golos.golos

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.golos.golos.screens.GolosActivity

class MainTestActivity : GolosActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vh_stripe_full_size)
    }

}
