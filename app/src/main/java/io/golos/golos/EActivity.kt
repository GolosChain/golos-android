package io.golos.golos

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import io.golos.golos.repository.services.GolosServicesImpl

class EActivity : AppCompatActivity() {
    private lateinit var mTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_e)
        mTextView = findViewById<TextView>(R.id.text)
        val renew = findViewById<View>(R.id.renew)
        val copy = findViewById<View>(R.id.copy)

        renew.setOnClickListener {
            mTextView.text = GolosServicesImpl.instance?.toString()
        }
        copy.setOnClickListener {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip =
                    ClipData.newPlainText("", PreferenceManager.getDefaultSharedPreferences(this).getString("token", ""))
        }
    }

    override fun onResume() {
        super.onResume()
        mTextView.text = GolosServicesImpl.instance?.toString()
    }

}
