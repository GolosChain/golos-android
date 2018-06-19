package io.golos.golos.screens.editor

enum class EditorTextModifier {
    INSERT_IMAGE, LINK, LIST_BULLET, LIST_NUMBERED, QUOTATION, QUOTATION_MARKS, TITLE, STYLE_BOLD;

    companion object {
        private val all = EditorTextModifier.values().toSet()
        fun remaining(from: Set<EditorTextModifier>) = all.minus(from)
    }

}
