package io.golos.golos.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.content.Context
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import eu.bittrade.libs.golosj.Golos4J
import io.golos.golos.repository.model.ExchangeValues
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.Executor

/**
 * Created by yuri on 19.03.18.
 */
internal class ExchangesRepository(private val worker: Executor,
                                   private val mainThreadExecutor: Executor) {
    private val rublesTag = "rublespergbg"
    private val dollarsTag = "gollarspergbg"
    private val vestingSharesToGolosPower = "vestingSharesToGolosPower"

    private val mExchangesLiveData: MutableLiveData<ExchangeValues> = MutableLiveData()

    fun getExchangeLiveData(): LiveData<ExchangeValues> {
        return mExchangesLiveData
    }

    fun setUp(ctx: Context) {
        val sp = ctx.getSharedPreferences("ExchangesRepository", Context.MODE_PRIVATE)
        val dollarsPerGbg = sp.getFloat(dollarsTag, 0.042291373f)
        val rublesPerGbg = sp.getFloat(rublesTag, 2.4418256f)
        val vestingSharesToGolosPowerMultiplier = sp.getFloat(vestingSharesToGolosPower, 0.000280892f)

        mExchangesLiveData.value = ExchangeValues(dollarsPerGbg, rublesPerGbg, vestingSharesToGolosPowerMultiplier)
        worker.execute {
            try {
                val resp = OkHttpClient().newCall(Request.Builder()
                        .url("https://golos.io/api/v1/rates/")
                        .method("GET", null).build()).execute()
                val body = JSONObject(resp.body().string()).getJSONObject("rates")
                val rublesPerUsd = body.get("RUB").toString().toFloat()
                val usdPerGbg = (((1 / body.get("XAU").toString().toFloat()) / 31.1034768) / 1000).toFloat()
                val rublePerGbgNew = usdPerGbg * rublesPerUsd

                if (usdPerGbg != 0.0f) {
                    sp.edit().putFloat(dollarsTag, usdPerGbg).apply()
                }
                if (rublePerGbgNew != 0.0f) {
                    sp.edit().putFloat(rublesTag, rublePerGbgNew).apply()
                }

                val globalProprties = Golos4J.getInstance().databaseMethods.dynamicGlobalProperties
                val totalVests = globalProprties.totalVestingShares.amount
                val totalVestsGolos = globalProprties.totalVestingFundSteem.amount
                val vestingGolosF = totalVestsGolos / totalVests

                if (vestingGolosF != 0.0) {
                    sp.edit().putFloat(vestingSharesToGolosPower, vestingGolosF.toFloat()).apply()
                }

                mainThreadExecutor.execute {
                    mExchangesLiveData.value = ExchangeValues(usdPerGbg, rublePerGbgNew, vestingGolosF.toFloat())
                }


            } catch (e: Exception) {
                e.printStackTrace()
                Timber.e(e)
            }
        }
    }
}