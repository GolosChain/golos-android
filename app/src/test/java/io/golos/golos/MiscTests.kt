package io.golos.golos

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.readValues
import eu.bittrade.libs.steemj.Golos4J
import io.golos.golos.repository.ImageLoadRunnable
import io.golos.golos.repository.model.mapper
import io.golos.golos.repository.persistence.SqliteDb
import io.golos.golos.utils.getString
import io.golos.golos.utils.toArrayList
import org.junit.Test
import java.util.*
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Created by yuri on 06.12.17.
 */
class MiscTests {

    @Test
    fun testTimeConvertion() {
        val timeCurrent = Calendar.getInstance(TimeZone.getTimeZone("GMT+1")).get(Calendar.HOUR)
        val timeMoscow = Calendar.getInstance(TimeZone.getTimeZone("GMT+5")).get(Calendar.HOUR)
        var offset = TimeZone.getTimeZone("GMT+5").getOffset(System.currentTimeMillis())
        println(offset)
        val diff = (timeCurrent - timeMoscow) / (1000 * 60 * 60)

        println("diffence is ${18000000 / (1000 * 60 * 60)} hours ")
    }

    @Test
    fun tesThreadCall() {
        val sharedExecutor: ThreadPoolExecutor by lazy {
            val queu = PriorityBlockingQueue<Runnable>(15, Comparator<Runnable> { o1, o2 ->
                if (o1 is ImageLoadRunnable) Int.MAX_VALUE
                else if (o1 is Runnable) Int.MIN_VALUE
                else 0
            })
            ThreadPoolExecutor(1, 1,
                    Long.MAX_VALUE, TimeUnit.MILLISECONDS, queu)
        }
        (0 until 10)
                .forEach {
                    sharedExecutor.execute(object : ImageLoadRunnable {
                        override fun run() {
                            Golos4J.getInstance().databaseMethods.accountCount
                        }
                    })
                }
        Thread.sleep(10_000)
    }

    @Test
    fun getTags() {
        val v = Golos4J.getInstance().databaseMethods.getTrendingTags("", Integer.MAX_VALUE)
        println(v)
    }
    @Test
    fun testConvert(){
        val list = listOf("ru--fotografiya", "vpp", "vpp-newbie")
        val s = mapper.writeValueAsString(list)
        println(s)

        val tagsString = "{[\"ru--fotografiya\",\"vpp\",\"vpp-newbie\"]}"
      //  val stringListType = mapper.typeFactory.constructCollectionType(List::class.java, String::class.java)
        val tags = mapper.readValue<List<String>>(s)
        println(tags)
    }

}