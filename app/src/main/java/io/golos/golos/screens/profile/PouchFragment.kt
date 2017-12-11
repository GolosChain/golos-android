package io.golos.golos.screens.profile

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.golos.golos.R
import io.golos.golos.screens.widgets.GolosFragment

/**
 * Created by yuri on 08.12.17.
 */
class PouchFragment : GolosFragment(), Observer<UserProfileState> {
    private lateinit var mGolosnumTv: TextView
    private lateinit var mGolosPoweNumrTv: TextView
    private lateinit var mGbgNumTv: TextView
    private lateinit var mGolosSafeNumTV: TextView
    private lateinit var mGolosSafeGbgNum: TextView
    private lateinit var mAccWorthNumTv: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.f_pouch, container, false)
        mGolosnumTv = view.findViewById(R.id.golos_num_tv)
        mGolosPoweNumrTv = view.findViewById(R.id.voting_power_num_tv)
        mGbgNumTv = view.findViewById(R.id.gbg_num_tv)
        mGolosSafeNumTV = view.findViewById(R.id.safe_golos_num_tv)
        mGolosSafeGbgNum = view.findViewById(R.id.safe_gbg_num_tv)
        mAccWorthNumTv = view.findViewById(R.id.account_worth_num_tv)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = ViewModelProviders.of(activity!!).get(AuthViewModel::class.java)
        viewModel.userProfileState.observe(activity as LifecycleOwner, this)
    }

    override fun onChanged(t: UserProfileState?) {
        if (t != null) {
            mGolosnumTv.text = setStyleAndCurrency(t.golosAmount, true)
            mGolosPoweNumrTv.text = setStyleAndCurrency(t.golosPower, true)
            mGbgNumTv.text = setStyleAndCurrency(t.gbgAmount, false)
            mGolosSafeNumTV.text = setStyleAndCurrency(t.golosInSafeAmount, true)
            mGolosSafeGbgNum.text = setStyleAndCurrency(t.gbgInSafeAmount, false)
            mAccWorthNumTv.text = setStyleAndCurrency(t.userAccountWorth, false)
        }
    }

    private fun setStyleAndCurrency(to: Double, isGolosCurrency: Boolean): CharSequence {
        val mainSpannable = TextAppearanceSpan(activity!!,R.style.ProfileNumberStyle)
        val currnecySpannable = TextAppearanceSpan(activity!!, R.style.ProfileCurrensyStyle)
        val number = String.format("%.2f", to)
        val currency = if (isGolosCurrency) "  golos" else "  GBG"
        val spannableString = SpannableString.valueOf(number + " " + currency)
        spannableString.setSpan(mainSpannable, 0, number.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(currnecySpannable,
                spannableString.length - currency.length,
                spannableString.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString
    }
}

