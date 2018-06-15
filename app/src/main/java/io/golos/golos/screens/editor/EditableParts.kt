package io.golos.golos.screens.editor

import android.text.Editable
import io.golos.golos.screens.editor.knife.KnifeParser
import java.util.*


/**
 * Created by yuri yurivladdurain@gmail.com on 25/10/2017.
 */

abstract class EditorPart(open val id: String = UUID.randomUUID().toString()) {
    abstract val htmlRepresentation: String
    open var startPointer: Int = CURSOR_POINTER_NOT_SELECTED
    open var endPointer: Int = CURSOR_POINTER_NOT_SELECTED


    fun isFocused() = endPointer > CURSOR_POINTER_NOT_SELECTED


    companion object {
        val CURSOR_POINTER_BEGIN = 0
        val CURSOR_POINTER_NOT_SELECTED = -1
    }
}

data class EditorImagePart(override val id: String = UUID.randomUUID().toString(),
                           val imageName: String,
                           val imageUrl: String) : EditorPart(id) {
    override val htmlRepresentation
        get() = "<center> <a href =\"$imageUrl\">$imageName</a> </center>"


    override var startPointer = CURSOR_POINTER_NOT_SELECTED
        set(value) {
            throw IllegalStateException("not supported")
        }

    override var endPointer = CURSOR_POINTER_NOT_SELECTED
        set(value) {
            throw IllegalStateException("not supported")
        }

}


data class EditorTextPart(override val id: String = UUID.randomUUID().toString(),
                          var text: Editable,
                          override var startPointer: Int = CURSOR_POINTER_NOT_SELECTED,
                          override var endPointer: Int = CURSOR_POINTER_NOT_SELECTED) : EditorPart(id) {

    override val htmlRepresentation: String
        get() {
            var out = KnifeParser.toHtml(text)
            val scriptRegex = "<(/)?[ ]*script[^>]*>"
            out = out.replace(Regex(scriptRegex), "")

            val linkregexp = "\\b((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])".toRegex()


            out.replace(linkregexp, { string ->
                val foundString = string.value
                if (foundString.endsWith("zip", true)
                        || foundString.endsWith("rar", true)
                        || foundString.endsWith("exe", true)
                        || foundString.endsWith("bat", true)
                        || foundString.endsWith("7z", true))
                    "*[link removed]*"
                else foundString
            })

            return out
        }

    fun setText(newText: Editable): EditorTextPart {
        return EditorTextPart(id, newText, startPointer, endPointer)
    }

    companion object {
        fun emptyTextPart() = EditorTextPart(UUID.randomUUID().toString(), emptySpan)
    }

}