package io.golos.golos.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import io.golos.golos.BuildConfig
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.StoryActivity

/**
 * Created by yuri on 12.12.17.
 */
class SettingActivity : GolosActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.a_settings)
        findViewById<View>(R.id.golos_wiki_tv).setOnClickListener({
            startActivity(createIntentForShowUrl("https://wiki.golos.io/"))
        })
        findViewById<View>(R.id.about_golos_tv).setOnClickListener({
            //https://golos.io/ru--golos/@golos/golos-russkoyazychnaya-socialno-mediinaya-blokchein-platforma
            StoryActivity.start(this,
                    "golos", "ru--golo",
                    "golos-russkoyazychnaya-socialno-mediinaya-blokchein-platforma",
                    FeedType.UNCLASSIFIED,
                    null)
        })
        findViewById<View>(R.id.privacy_policy_tv).setOnClickListener({
            StoryActivity.start(this,
                    "golos", "ru--konfidenczialxnostx",
                    "politika-konfidencialnosti",
                    FeedType.UNCLASSIFIED,
                    null)
        })
        findViewById<View>(R.id.exit).setOnClickListener({
            Repository.get.deleteUserdata()
            Toast.makeText(this, R.string.user_made_logout, Toast.LENGTH_SHORT).show()
        })
        val versionTv = findViewById<TextView>(R.id.version_tv)
        versionTv.text = getString(R.string.golos_android_v, BuildConfig.VERSION_NAME)
    }

    private fun createIntentForShowUrl(url: String): Intent {
        val i = Intent(Intent.ACTION_VIEW)
        i.setData(Uri.parse(url));
        return i
    }
}