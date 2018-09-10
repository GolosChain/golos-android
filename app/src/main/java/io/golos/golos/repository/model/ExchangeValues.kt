package io.golos.golos.repository.model

/**
 * Created by yuri on 22.01.18.
 */
data class ExchangeValues(val dollarsPerGbg: Float,
                          val rublesPerGbg: Float,
                          val vSharesToGolosPowerMultiplier: Float) {
    companion object {
        @JvmStatic
        val nullValues: ExchangeValues = ExchangeValues(0.0f, 0.0f, 0.0f)
    }
}