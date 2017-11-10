package io.golos.golos.screens.editor

import java.util.*
import java.util.regex.Pattern


/**
 * Created by yuri yurivladdurain@gmail.com on 25/10/2017.
 */


abstract class Part {
    abstract val markdownRepresentation: CharSequence
    abstract val pointerPosition: Int?
    abstract fun clearCursor(): Part
    fun isFocused() = pointerPosition != null
    abstract fun setCursor(position: Int?): Part
    val id = UUID.randomUUID().toString()

    companion object {
        val CURSOR_POINTER_BEGIN = 0
    }
}

data class EditorImagePart(val imageName: String,
                           val imageUrl: String,
                           override val pointerPosition: Int?) : Part() {
    override val markdownRepresentation
        get() = "![$imageName]($imageUrl)"

    override fun clearCursor() = EditorImagePart(imageName, imageUrl, null)
    override fun setCursor(position: Int?): Part = EditorImagePart(imageName, imageUrl, null)
}

data class EditorTextPart(val text: String,
                          override val pointerPosition: Int?) : Part() {

    override val markdownRepresentation: CharSequence
        get() {
            var out = text
            val scriptRegex = "<(/)?[ ]*script[^>]*>"
            out = out.replace(Regex(scriptRegex), "")

            val linkregexp = "\\b((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])"
            val pattern = Pattern.compile(linkregexp, Pattern.CASE_INSENSITIVE)
            val urlMatcher = pattern.matcher(out)

            while (urlMatcher.find()) {
                val foundString = urlMatcher.group()
                //val foundString = out.substring(urlMatcher.start(0), urlMatcher.end(0))
                if (foundString.endsWith("zip", true)
                        || foundString.endsWith("rar", true)
                        || foundString.endsWith("exe", true)
                        || foundString.endsWith("bat", true)
                        || foundString.endsWith("7z", true)) {
                    out = out.replace(foundString, "*[link removed]*")
                }
            }
            return out
        }

    override fun clearCursor() = EditorTextPart(text, null)
    override fun setCursor(position: Int?): Part = EditorTextPart(text, position)
}