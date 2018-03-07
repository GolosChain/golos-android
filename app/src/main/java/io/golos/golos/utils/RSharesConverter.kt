package io.golos.golos.utils

import eu.bittrade.libs.steemj.base.models.GlobalProperties
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

/**
 * Created by yuri on 22.01.18.
 */
object RSharesConverter {
    private val price = 1.00 / 0.115
    private val s = BigDecimal("2000000000000")
    private val squareS = s.pow(2)

    private val s1 = BigInteger("2000000000000")
    private val squareS1 = s1.pow(2)

    private fun convertRSharesToVShares(rshares: BigInteger): BigDecimal {
        val rshares = BigDecimal(rshares)
        return (rshares + s).pow(2) - squareS
    }

    private fun convertRSharesToVShares1(rshares: BigInteger): BigInteger {
        return (rshares + s1).pow(2) - squareS1
    }


    fun convertRSharesToGbg(properties: GlobalProperties, rshares: List<BigInteger>): List<Double> {
        val totalReward2Shares = BigDecimal(properties.totalRewardShares2)
        val totalRewardFund = properties.totalRewardFundSteem

        val pot = totalRewardFund.amount * price
        val out = ArrayList<Double>(rshares.size)
        rshares.forEach {
            var votesRshares2 = convertRSharesToVShares(it).multiply(BigDecimal.valueOf(pot), MathContext.DECIMAL128).divide(totalReward2Shares, MathContext.DECIMAL128)
            out.add(votesRshares2.toDouble())
        }


        return out
    }

    /*   val total_reward_shares2 = "5133376796721014669412091165625"
        // content.active_votes -> voter.rshares
        val vote_rshares = "122177915528"

        val total_reward_fund_steem = 34482.399 // 34482.399 GOLOS
        val medianPrice = getMedianPrice()

        // props.total_reward_fund_steem
        var pot = total_reward_fund_steem
        pot *= medianPrice
        pot = java.lang.Double.parseDouble(String.format("%.3f", pot))

        val total_r2 = BigInteger(total_reward_shares2)

        var r2 = calculate_vshares(BigInteger(vote_rshares))
        r2 = r2.multiply(BigInteger(pot.toString().replace(".", "")))
        r2 = r2.divide(total_r2)

        val result = r2.toString().toLong()
        print((result / 1000).toString() + "." + result % 1000)
        println(" GBG")*/
    fun convertRSharesToGbg1(properties: GlobalProperties, rshares: List<BigInteger>): List<Double> {
        val out = ArrayList<Double>(rshares.size)
        val total_reward_shares2 = "5133376796721014669412091165625"
        // content.active_votes -> voter.rshares

        rshares.forEach {
            val total_reward_fund_steem = properties.totalRewardFundSteem.amount
            val medianPrice = price
            val vote_rshares = it

            // props.total_reward_fund_steem
            var pot = total_reward_fund_steem
            pot *= medianPrice
            pot = java.lang.Double.parseDouble(String.format("%.3f", pot))

            val total_r2 = properties.totalRewardShares2

            var r2 = convertRSharesToVShares1(vote_rshares)
            r2 = r2.multiply(BigInteger(pot.toString().replace(".", "")))
            r2 = r2.divide(total_r2)

            val result = r2.toString().toLong()
            val s = (result / 1000).toString() + "." + result % 1000
            out.add(s.toDouble())
        }
        return out


    }

    // convert only post & comments gbg, otherwise it will be long overflow
    fun convertRSharesToGbg2(postPayoutInGbg: Double, rshares: List<Long>, totalRshares: Long): List<Double> {
        val total = totalRshares.toDouble()
        Timber.e("total = $total")
        if (total == 0.0) {
            return rshares.map { 0.0 }
        }
        return rshares.map { long ->
            val percent = long / total
            postPayoutInGbg * percent
        }
    }
}