package io.golos.golos.screens.editor

import java.util.*
import java.util.regex.Pattern


/**
 * Created by yuri yurivladdurain@gmail.com on 25/10/2017.
 */


abstract class EditorPart(open val id: String = UUID.randomUUID().toString()) {
    abstract val markdownRepresentation: CharSequence
    abstract var pointerPosition: Int?
    abstract fun clearCursor(): EditorPart
    fun isFocused() = pointerPosition != null
    abstract fun setCursor(position: Int?): EditorPart

    companion object {
        val CURSOR_POINTER_BEGIN = 0
    }
}

data class EditorImagePart(override val id: String = UUID.randomUUID().toString(),
                           val imageName: String,
                           val imageUrl: String,
                           override var pointerPosition: Int?) : EditorPart(id) {
    override val markdownRepresentation
        get() = "<center>![$imageName]($imageUrl)</center>"

    override fun clearCursor() = EditorImagePart(id, imageName, imageUrl, null)
    override fun setCursor(position: Int?): EditorPart = EditorImagePart(id, imageName, imageUrl, null)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EditorImagePart) return false

        if (imageName != other.imageName) return false
        if (imageUrl != other.imageUrl) return false
        if (pointerPosition != other.pointerPosition) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageName.hashCode()
        result = 31 * result + imageUrl.hashCode()
        result = 31 * result + (pointerPosition ?: 0)
        return result
    }


}

data class EditorTextPart(override val id: String = UUID.randomUUID().toString(),
                          var text: String,
                          override var pointerPosition: Int?) : EditorPart(id) {

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

    override fun clearCursor() = EditorTextPart(id = this.id, text = text, pointerPosition = null)
    override fun setCursor(position: Int?): EditorPart = EditorTextPart(id, text, position)
}