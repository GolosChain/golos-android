package io.golos.golos.utils

import junit.framework.Assert
import org.junit.Test

/**
 * Created by yuri on 13.12.17.
 */
class GolosLinkMatcherTest {

    @Test
    fun matchGoldVoice() {//https://goldvoice.club/@vood.one/istoriya-o-tom-kak-ryzhevolosye-varvary-okno-v-yaponiyu-prorubali-chast-2-davlenie-ameriki-i-okonchatelnyi-krakh-izolyacii
        var result = GolosLinkMatcher.match("https://goldvoice.club/@sinte/o-socialnykh-psikhopatakh-chast-3-o-tikhonyakh-mechtatelyakh-stesnitelnykh/")
        Assert.assertTrue(result is StoryLinkMatch)
        Assert.assertEquals(StoryLinkMatch("sinte", null,
                "o-socialnykh-psikhopatakh-chast-3-o-tikhonyakh-mechtatelyakh-stesnitelnykh"), result)

        result = GolosLinkMatcher.match("https://goldvoice.club/@vood.one/istoriya-o-tom-kak-ryzhevolosye-varvary-okno-v-yaponiyu-prorubali-chast-2-davlenie-ameriki-i-okonchatelnyi-krakh-izolyacii")
        Assert.assertTrue(result is StoryLinkMatch)
        Assert.assertEquals(StoryLinkMatch("vood.one", null,
                "istoriya-o-tom-kak-ryzhevolosye-varvary-okno-v-yaponiyu-prorubali-chast-2-davlenie-ameriki-i-okonchatelnyi-krakh-izolyacii"), result)
    }
}