package io.golos.golos.screens.profile

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.SpannableString
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.golos.golos.R
import io.golos.golos.screens.profile.adapters.KeyValueAdapter
import io.golos.golos.screens.profile.viewmodel.UserAccountModel
import io.golos.golos.screens.profile.viewmodel.UserInfoViewModel
import io.golos.golos.screens.widgets.GolosFragment
import io.golos.golos.utils.MyLinearLayoutManager
import java.text.SimpleDateFormat
import java.util.*

class UserInformationFragment : GolosFragment(), Observer<UserAccountModel> {

    override fun onChanged(t: UserAccountModel?) {
        val accInfo = t?.accountInfo ?: return
        val rows = ArrayList<KeyValueRow>(4)
        rows.add(KeyValueRow(getString(R.string.city), accInfo.location))
        rows.add(KeyValueRow(getString(R.string.site), accInfo.website))
        val sdf = SimpleDateFormat("LLLL, yyyy", Locale.getDefault())

        val dateString = sdf.format(accInfo.registrationDate).capitalize()
        rows.add(KeyValueRow(getString(R.string.registrationDate), getString(R.string.yeat_with_dor, dateString)))


        val currnecySpannable = TextAppearanceSpan(activity!!, R.style.ProfileCurrensyStyle)
        val number = String.format("%.3f", accInfo.golosPower)
        val spannableString = SpannableString.valueOf("${number} Golos")
        spannableString.setSpan(currnecySpannable,
                number.length + 1,
                spannableString.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)

        rows.add(KeyValueRow(getString(R.string.golos_power),
                spannableString, R.drawable.ic_chevron_down_gray_12dp_7d))

        (mRecyclerView.adapter as? KeyValueAdapter)?.values = rows
    }

    private lateinit var mRecyclerView: androidx.recyclerview.widget.RecyclerView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.f_information, container, false)
        mRecyclerView = v as androidx.recyclerview.widget.RecyclerView
        mRecyclerView.layoutManager = MyLinearLayoutManager(activity!!)
        mRecyclerView.adapter = KeyValueAdapter(listOf(), {})

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProviders.of(activity!!).get(UserInfoViewModel::class.java)
        viewModel.getLiveData().observe(this, this)
    }
}

data class KeyValueRow(val key: CharSequence,
                       val value: CharSequence,
                       val valueEndDrawable: Int? = null)