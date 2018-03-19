package io.golos.golos.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import io.golos.golos.repository.model.ExchangeValues
import java.util.concurrent.Executor

/**
 * Created by yuri on 19.03.18.
 */
internal class ExchangesRepository(private val worker: Executor,
                                   private val mainThreadExecutor: Executor) {
    private val rublesTag = "rublespergbg"
    private val dollarsTag = "gollarspergbg"

    private val mExchangesLiveData: MutableLiveData<ExchangeValues> = MutableLiveData()

    open fun getExchangeLiveData(): LiveData<ExchangeValues> {
        return mExchangesLiveData
    }

    fun setUp(ctx: Context) {
        val sp = ctx.getSharedPreferences("ExchangesRepository", Context.MODE_PRIVATE)
        val dollarsPerGbg = sp.getFloat(dollarsTag, 0.0422f)
        val rublesPerGbg = sp.getFloat(rublesTag, 0.0422f * 57.49f)
        mExchangesLiveData.value = ExchangeValues(dollarsPerGbg, rublesPerGbg)
    }
}