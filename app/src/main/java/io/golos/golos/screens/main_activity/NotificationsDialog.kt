package io.golos.golos.screens.main_activity

import androidx.lifecycle.Observer
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.DialogFragment
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.notifications.GolosNotifications
import io.golos.golos.notifications.PostLinkable
import io.golos.golos.screens.main_activity.adapters.DissmissTouchHelper
import io.golos.golos.screens.main_activity.adapters.NotificationsAdapter
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.DiscussionActivity

class NotificationsDialog : DialogFragment(), Observer<GolosNotifications> {
    private lateinit var mRecyclerView: androidx.recyclerview.widget.RecyclerView
    private val mHandler = Handler()

    override fun onChanged(t: GolosNotifications?) {
        if (t?.notifications?.size ?: 0 == 0) dismiss()
        else {
            (mRecyclerView.adapter as? NotificationsAdapter)?.notification = t?.notifications ?: listOf()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.NotificationsDialog)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.d_notifications, container, false)
        mRecyclerView = v.findViewById(R.id.recycler)
        val decor = androidx.recyclerview.widget.DividerItemDecoration(context
                ?: return null, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL)
        decor.setDrawable(ContextCompat.getDrawable(context!!, R.drawable.shape_gray_line)!!)
        mRecyclerView.addItemDecoration(decor)
        mRecyclerView.adapter = NotificationsAdapter(listOf(),
                {
                    if (it is PostLinkable) {
                        it.getLink()?.let {
                            DiscussionActivity.start(context
                                    ?: return@NotificationsAdapter, it.author, it.blog, it.permlink, FeedType.UNCLASSIFIED, null)
                        }
                        mHandler.postDelayed({ Repository.get.notificationsRepository.dismissNotification(it) }, 1000)
                    } else {
                        mHandler.post { Repository.get.notificationsRepository.dismissNotification(it) }
                    }

                },
                { Repository.get.notificationsRepository.dismissNotification(it) }, false)

        val adapter = mRecyclerView.adapter as? NotificationsAdapter
        adapter?.let {
            DissmissTouchHelper(it).attachToRecyclerView(mRecyclerView)
        }
        v.setOnClickListener { dismiss() }

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Repository.get.notificationsRepository.notifications.observe(this, this)
    }

}