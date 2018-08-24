package io.golos.golos.repository

import io.golos.golos.App
import io.golos.golos.repository.model.StoriesFeed
import io.golos.golos.screens.editor.knife.KnifeParser
import io.golos.golos.screens.editor.knife.SpanFactory
import io.golos.golos.utils.GolosError
import io.golos.golos.utils.Htmlizer
import io.golos.golos.utils.createGolosSpan

fun StoriesFeed.setNewError(error: GolosError?): StoriesFeed {
    return StoriesFeed(items, type, filter, null, isFeedActual)
}

object KnifeHtmlizer : Htmlizer {
    override fun toHtml(input: String) = KnifeParser.fromHtml(input, object : SpanFactory {
        override fun <T : Any?> produceOfType(type: Class<*>): T {
            return App.context.createGolosSpan(type)
        }
    })
}