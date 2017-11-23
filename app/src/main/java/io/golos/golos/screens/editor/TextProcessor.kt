package io.golos.golos.screens.editor

import timber.log.Timber

/**
 * Created by yuri yurivladdurain@gmail.com on 26/10/2017.
 */
class TextProcessor {

    fun processInput(previousState: List<EditorPart>, action: EditorInputAction): List<EditorPart> {
        Timber.e("process input $previousState")
        val parts = previousState
        val selectedPart = parts.findLast { it.isFocused() }

        val cursorPosition: Int? = selectedPart?.pointerPosition
        val out = ArrayList<EditorPart>()
        if (selectedPart == null || cursorPosition == null) {
            out.addAll(previousState)
            if (action is EditorInputAction.InsertAction) {
                appendPartToTheEnd(action.part, out)
            } else if (action is EditorInputAction.DeleteAction) {
                if (out.isEmpty()) out.add(EditorTextPart(text = "", pointerPosition = EditorPart.CURSOR_POINTER_BEGIN))
            }
        } else if (parts.isEmpty()) {
            out.add(EditorTextPart(text = "", pointerPosition = EditorPart.CURSOR_POINTER_BEGIN))
        } else if (parts.size == 1) {
            when (action) {
                is EditorInputAction.InsertAction -> {
                    if (action.part is EditorImagePart) {
                        if (selectedPart is EditorTextPart && cursorPosition != selectedPart.text.length) {
                            out.addAll(insertPartIntoOtherPart(action.part, selectedPart, cursorPosition))
                        } else {
                            out.addAll(copyClearingCursor(parts))
                            appendPartToTheEnd(action.part, out)
                        }
                    } else if (action.part is EditorTextPart) {
                        out.addAll(insertPartIntoOtherPart(action.part, selectedPart, cursorPosition))
                    }

                }
                is EditorInputAction.DeleteAction -> {
                    if (parts[0] is EditorImagePart) {
                        out.add(EditorTextPart(text = "", pointerPosition = EditorPart.CURSOR_POINTER_BEGIN))
                    } else if (parts[0] is EditorTextPart) {
                        val textPart = parts[0] as EditorTextPart
                        if (action.fastDelete != null) {
                            out.add(EditorTextPart(textPart.id, "", EditorPart.CURSOR_POINTER_BEGIN))
                        } else {
                            val currentText = textPart.text
                            if (cursorPosition == 0) {
                                out.add(textPart)
                            } else if (cursorPosition == currentText.length) {
                                out.add(EditorTextPart(textPart.id, currentText.substring(0, currentText.length - 1), textPart.text.length - 1))
                            } else {
                                if (currentText.length > 1) {
                                    out.add(EditorTextPart(textPart.id, currentText.substring(0, cursorPosition - 1) + currentText.substring(cursorPosition + 1, currentText.length),
                                            cursorPosition - 1))
                                } else {
                                    out.add(EditorTextPart(textPart.id, "", EditorPart.CURSOR_POINTER_BEGIN))
                                }
                            }
                        }
                    }
                }

            }
        } else {
            val position = parts.lastIndexOf(selectedPart)
            val isFirst = position == 0
            val isLast = position == parts.lastIndex
            when (action) {
                is EditorInputAction.InsertAction -> {
                    if (selectedPart is EditorImagePart) {
                        if (isFirst) {
                            out.addAll(insertPartIntoOtherPart(action.part, selectedPart, cursorPosition))
                            parts.subList(1, parts.lastIndex).forEach { out.add(it.clearCursor()) }
                        } else if (isLast) {
                            parts.subList(0, parts.lastIndex - 1).forEach { out.add(it.clearCursor()) }
                            out.addAll(insertPartIntoOtherPart(action.part, selectedPart, cursorPosition))
                        } else {
                            parts.subList(0, position).forEach { out.add(it.clearCursor()) }
                            out.addAll(insertPartIntoOtherPart(action.part, selectedPart, cursorPosition))
                            parts.subList(position, parts.size).forEach { out.add(it.clearCursor()) }
                        }
                    } else if (selectedPart is EditorTextPart) {
                        if (isFirst) {
                            out.addAll(insertPartIntoOtherPart(action.part, selectedPart, cursorPosition))
                            parts.subList(1, parts.lastIndex).forEach { out.add(it.clearCursor()) }
                        } else if (isLast) {
                            parts.subList(0, parts.lastIndex).forEach { out.add(it.clearCursor()) }
                            out.addAll(insertPartIntoOtherPart(action.part, selectedPart, cursorPosition))
                        } else {
                            parts.subList(0, position).forEach { out.add(it.clearCursor()) }
                            out.addAll(insertPartIntoOtherPart(action.part, selectedPart, cursorPosition))
                            parts.subList(position, parts.size).forEach { out.add(it.clearCursor()) }
                        }
                    }
                }
                is EditorInputAction.DeleteAction -> {
                    if (action.fastDelete != null) {
                        out.addAll(deletePosition(action.fastDelete, parts))
                    } else {
                        if (selectedPart is EditorImagePart) {
                            out.addAll(deletePosition(position, parts))
                        } else if (selectedPart is EditorTextPart) {
                            val currentText = selectedPart.text
                            if (cursorPosition == 0) {
                                out.addAll(parts)
                            } else if (cursorPosition == currentText.length) {
                                out.addAll(copyClearingCursor(parts))

                                out[position] = EditorTextPart(selectedPart.id, currentText.substring(0, currentText.length - 1), currentText.length - 1)
                            } else {
                                if (currentText.length > 1) {
                                    out.addAll(copyClearingCursor(parts))
                                    var part = out[position] as EditorTextPart
                                    out[position] = (EditorTextPart(part.id,
                                            currentText.substring(0, cursorPosition - 1) + currentText.substring(cursorPosition + 1,
                                                    currentText.length), cursorPosition - 1))
                                } else {
                                    out.add(EditorTextPart(text = "", pointerPosition = EditorPart.CURSOR_POINTER_BEGIN))
                                }
                            }
                        }
                    }

                }
            }

        }
        addEditTextDelimeters(out)
        removeDoublingTextBlocks(out)
        return out
    }

    private fun deletePosition(position: Int, from: List<EditorPart>): ArrayList<EditorPart> {
        val out = ArrayList<EditorPart>()
        if (position == 0) {
            out.addAll(from.subList(1, from.size))
        } else if (position == from.lastIndex) {
            out.addAll(from.subList(0, from.lastIndex - 1))
        } else {
            out.addAll(from.subList(0, position))
            out.addAll(from.subList(position + 1, from.size))
        }
        return out
    }

    private fun addEditTextDelimeters(to: ArrayList<EditorPart>) {
        if (to[0] is EditorImagePart) to.add(0, EditorTextPart(text = "", pointerPosition = null))
        if (to.last() is EditorImagePart) to.add(EditorTextPart(text = "", pointerPosition = null))
        val iter = to.listIterator()
        while (iter.hasNext()) {
            val part = iter.next()
            if (part is EditorImagePart) {
                if (to[iter.nextIndex()] !is EditorTextPart) {
                    iter.add(EditorTextPart(text = "", pointerPosition = null))
                }
            }
        }
    }

    private fun removeDoublingTextBlocks(from: ArrayList<EditorPart>) {
        if (from.size < 1) return
        val iter = from.listIterator()
        var deleteNext = false
        while (iter.hasNext()) {
            val currentPart = iter.next()
            if (deleteNext) {
                iter.remove()
                deleteNext = false
            }
            if (iter.hasNext()) {
                val next = from[iter.nextIndex()]
                if (currentPart is EditorTextPart && next is EditorTextPart) {
                    var focus: Int? = null
                    val outText = (currentPart.text + "\n" + next.text).trim()
                    if (currentPart.isFocused()) {
                        focus = currentPart.pointerPosition
                    } else if (next.isFocused()) {
                        focus = currentPart.text.length + next.pointerPosition!!
                    }
                    iter.set(EditorTextPart(currentPart.id, outText, focus))
                    deleteNext = true
                }
            }
        }
    }


    private fun appendImagePartToTheEnd(part: EditorImagePart, to: ArrayList<EditorPart>) = to.apply {
        add(EditorImagePart(part.id, part.imageName, part.imageUrl, null))
        add(EditorTextPart(text = "", pointerPosition = EditorPart.CURSOR_POINTER_BEGIN))
    }

    private fun appendTextPartToTheEnd(part: EditorTextPart, to: ArrayList<EditorPart>) = to.apply {
        if (!to.isEmpty() && to.last() is EditorTextPart) {
            val last = to.last() as EditorTextPart
            val outText = last.text + part.text
            to[lastIndex] = EditorTextPart(last.id, outText, outText.length)
        } else {
            to.add(EditorTextPart(part.id, text = part.text, pointerPosition = part.text.length))
        }
    }

    private fun appendPartToTheEnd(part: EditorPart, to: ArrayList<EditorPart>) {
        if (part is EditorTextPart) {
            appendTextPartToTheEnd(part, to)
            if (to.size > 2) {
                if (to.last() is EditorTextPart && to[to.lastIndex - 1] is EditorTextPart) {
                    val pre = (to[to.lastIndex - 1] as EditorTextPart)
                    val last = (to.last() as EditorTextPart).text
                    val preLast = (to[to.lastIndex - 1] as EditorTextPart).text
                    to[to.lastIndex - 1] = EditorTextPart(pre.id, preLast + last, (preLast + last).length)
                    to.removeAt(to.lastIndex)
                }
            }
        } else if (part is EditorImagePart) {
            appendImagePartToTheEnd(part, to)
        }
    }

    private fun prependPart(part: EditorPart, to: ArrayList<EditorPart>) {
        if (part is EditorTextPart) {
            prependText(part.text, to)
        } else if (part is EditorImagePart) {
            prependImage(part, to)
        }
    }

    private fun prependText(text: String, to: ArrayList<EditorPart>) {
        if (!to.isEmpty() && to.first() is EditorTextPart) {
            val first = to.first() as EditorTextPart
            val outText = first.text + text
            to[0] = EditorTextPart(first.id, outText, text.length)
        } else {
            to.add(0, EditorTextPart(text = text, pointerPosition = text.length))
        }
    }

    private fun prependImage(part: EditorImagePart, to: ArrayList<EditorPart>) {
        to.add(0, EditorImagePart(part.id, part.imageName, part.imageUrl, null))
    }

    private fun insertPartIntoOtherPart(partToInsert: EditorPart, into: EditorPart, position: Int): List<EditorPart> {
        val out = ArrayList<EditorPart>()
        if (position == 0) {//first
            out.add(into)
            prependPart(partToInsert, out)
        } else if ((into is EditorTextPart && into.text.length <= position) || (into is EditorImagePart)) {//last
            out.add(into.clearCursor())
            appendPartToTheEnd(partToInsert, out)
            if (partToInsert is EditorImagePart) {
                out[out.lastIndex] = EditorTextPart(text = "", pointerPosition = EditorPart.CURSOR_POINTER_BEGIN)
            }
        } else {
            if (into is EditorTextPart) {
                if (partToInsert is EditorTextPart) {
                    var textPart = into.text.take(position) + partToInsert.text
                    val cursor = textPart.length
                    textPart += into.text.substring(position)
                    out.add(EditorTextPart(into.id, textPart, cursor))
                } else if (partToInsert is EditorImagePart) {
                    val textPart1 = into.text.take(position)
                    out.add(EditorTextPart(into.id, textPart1, null))
                    out.add(partToInsert)
                    out.add(EditorTextPart(text = into.text.substring(position), pointerPosition = 0))
                }
            } else if (into is EditorImagePart) {
                out.add(into.clearCursor())
                appendPartToTheEnd(partToInsert, out)
            }
        }
        if (partToInsert is EditorTextPart && into is EditorTextPart) {
            out[out.lastIndex] = out[out.lastIndex].setCursor(position + partToInsert.text.length)
        } else if (into is EditorTextPart && partToInsert is EditorImagePart) {
            out[out.lastIndex] = out[out.lastIndex].setCursor(EditorPart.CURSOR_POINTER_BEGIN)
        } else if (partToInsert is EditorImagePart) {
            out[out.indexOf(partToInsert)] = partToInsert.setCursor(null)
        }
        return out
    }

    private fun copyClearingCursor(from: List<EditorPart>): List<EditorPart> {
        val out = ArrayList<EditorPart>(from.size)
        from.forEach { out.add(it.clearCursor()) }
        return out
    }

    fun getInitialState(): List<EditorPart> {
        val out = ArrayList<EditorPart>()
        out.add(EditorTextPart(text = "", pointerPosition = EditorPart.CURSOR_POINTER_BEGIN))
        return out
    }
}

sealed class EditorInputAction {
    class InsertAction(val part: EditorPart) : EditorInputAction()
    class DeleteAction(val fastDelete: Int?) : EditorInputAction()
}