package io.golos.golos.screens.editor

/**
 * Created by yuri yurivladdurain@gmail.com on 26/10/2017.
 */
class TextProcessor {

    fun processInput(previousState: List<Part>, action: EditorInputAction): List<Part> {
        val parts = previousState
        val selectedPart = parts.findLast { it.isFocused() }

        val cursorPosition: Int? = selectedPart?.pointerPosition
        val out = ArrayList<Part>()
        if (selectedPart == null || cursorPosition == null) {
            out.addAll(previousState)
            if (action is EditorInputAction.InsertAction) {
                appendPartToTheEnd(action.part, out)
            } else if (action is EditorInputAction.DeleteAction) {
                if (out.isEmpty()) out.add(EditorTextPart("", Part.CURSOR_POINTER_BEGIN))
            }
        } else if (parts.isEmpty()) {
            out.add(EditorTextPart("", Part.CURSOR_POINTER_BEGIN))
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
                        out.add(EditorTextPart("", Part.CURSOR_POINTER_BEGIN))
                    } else if (parts[0] is EditorTextPart) {
                        val textPart = parts[0] as EditorTextPart
                        if (action.fastDelete != null) {
                            out.add(EditorTextPart("", Part.CURSOR_POINTER_BEGIN))
                        } else {
                            val currentText = textPart.text
                            if (cursorPosition == 0) {
                                out.add(textPart)
                            } else if (cursorPosition == currentText.length) {
                                out.add(EditorTextPart(currentText.substring(0, currentText.length - 1), textPart.text.length - 1))
                            } else {
                                if (currentText.length > 1) {
                                    out.add(EditorTextPart(currentText.substring(0, cursorPosition - 1) + currentText.substring(cursorPosition + 1, currentText.length), cursorPosition - 1))
                                } else {
                                    out.add(EditorTextPart("", Part.CURSOR_POINTER_BEGIN))
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
                                out[position] = EditorTextPart(currentText.substring(0, currentText.length - 1), currentText.length - 1)
                            } else {
                                if (currentText.length > 1) {
                                    out.addAll(copyClearingCursor(parts))
                                    out[position] = (EditorTextPart(currentText.substring(0, cursorPosition - 1) + currentText.substring(cursorPosition + 1, currentText.length), cursorPosition - 1))
                                } else {
                                    out.add(EditorTextPart("", Part.CURSOR_POINTER_BEGIN))
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

    private fun deletePosition(position: Int, from: List<Part>): ArrayList<Part> {
        val out = ArrayList<Part>()
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

    private fun addEditTextDelimeters(to: ArrayList<Part>) {
        if (to[0] is EditorImagePart) to.add(0, EditorTextPart("", null))
        if (to.last() is EditorImagePart) to.add(EditorTextPart("", null))
        val iter = to.listIterator()
        while (iter.hasNext()) {
            val part = iter.next()
            if (part is EditorImagePart) {
                if (to[iter.nextIndex()] !is EditorTextPart) {
                    iter.add(EditorTextPart("", null))
                }
            }
        }
    }

    private fun removeDoublingTextBlocks(from: ArrayList<Part>) {
        if (from.size < 1) return
        val iter = from.listIterator()
        var deleteNext = false
        while (iter.hasNext()) {
            val currentPart = iter.next()
            if (iter.hasNext()) {
                val next = from[iter.nextIndex()]
                if (deleteNext) {
                    iter.remove()
                    deleteNext = false
                }
                if (currentPart is EditorTextPart && next is EditorTextPart) {
                    var focus: Int? = null
                    val outText = currentPart.text + next.text
                    if (currentPart.isFocused()) {
                        focus = currentPart.pointerPosition
                    } else if (next.isFocused()) {
                        focus = currentPart.text.length + next.pointerPosition!!
                    }
                    iter.set(EditorTextPart(outText, focus))
                    deleteNext = true
                }
            }
        }
    }


    private fun appendImagePartToTheEnd(name: String, url: String, to: ArrayList<Part>) = to.apply {
        add(EditorImagePart(name, url, null))
        add(EditorTextPart("", Part.CURSOR_POINTER_BEGIN))
    }

    private fun appendTextPartToTheEnd(text: String, to: ArrayList<Part>) = to.apply {
        if (!to.isEmpty() && to.last() is EditorTextPart) {
            val outText = (to[lastIndex] as EditorTextPart).text + text
            to[lastIndex] = EditorTextPart(outText, outText.length)
        } else {
            to.add(EditorTextPart(text, text.length))
        }
    }

    private fun appendPartToTheEnd(part: Part, to: ArrayList<Part>) {
        if (part is EditorTextPart) {
            appendTextPartToTheEnd(part.text, to)
            if (to.size > 2) {
                if (to.last() is EditorTextPart && to[to.lastIndex - 1] is EditorTextPart) {
                    val last = (to.last() as EditorTextPart).text
                    val preLast = (to[to.lastIndex - 1] as EditorTextPart).text
                    to[to.lastIndex - 1] = EditorTextPart(preLast + last, (preLast + last).length)
                    to.removeAt(to.lastIndex)
                }
            }
        } else if (part is EditorImagePart) {
            appendImagePartToTheEnd(part.imageName, part.imageUrl, to)
        }
    }

    private fun prependPart(part: Part, to: ArrayList<Part>) {
        if (part is EditorTextPart) {
            prependText(part.text, to)
        } else if (part is EditorImagePart) {
            prependImage(part.imageName, part.imageUrl, to)
        }
    }

    private fun prependText(text: String, to: ArrayList<Part>) {
        if (!to.isEmpty() && to.first() is EditorTextPart) {
            val outText = (to.first() as EditorTextPart).text + text
            to[0] = EditorTextPart(outText, text.length)
        } else {
            to.add(0, EditorTextPart(text, text.length))
        }
    }

    private fun prependImage(name: String, url: String, to: ArrayList<Part>) {
        to.add(0, EditorImagePart(name, url, null))
    }

    private fun insertPartIntoOtherPart(partToInsert: Part, into: Part, position: Int): List<Part> {
        val out = ArrayList<Part>()
        if (position == 0) {//first
            out.add(into)
            prependPart(partToInsert, out)
        } else if ((into is EditorTextPart && into.text.length <= position) || (into is EditorImagePart)) {//last
            out.add(into.clearCursor())
            appendPartToTheEnd(partToInsert, out)
            if (partToInsert is EditorImagePart) {
                out[out.lastIndex] = EditorTextPart("", Part.CURSOR_POINTER_BEGIN)
            }
        } else {
            if (into is EditorTextPart) {
                if (partToInsert is EditorTextPart) {
                    var textPart = into.text.take(position) + partToInsert.text
                    val cursor = textPart.length
                    textPart += into.text.substring(position)
                    out.add(EditorTextPart(textPart, cursor))
                } else if (partToInsert is EditorImagePart) {
                    val textPart1 = into.text.take(position)
                    out.add(EditorTextPart(textPart1, null))
                    out.add(EditorImagePart(partToInsert.imageName, partToInsert.imageUrl, null))
                    out.add(EditorTextPart(into.text.substring(position), 0))
                }
            } else if (into is EditorImagePart) {
                out.add(into.clearCursor())
                appendPartToTheEnd(partToInsert, out)
            }
        }
        if (partToInsert is EditorTextPart && into is EditorTextPart) {
            out[out.lastIndex] = out[out.lastIndex].setCursor(position + partToInsert.text.length)
        } else if (into is EditorTextPart && partToInsert is EditorImagePart) {
            out[out.lastIndex] = out[out.lastIndex].setCursor(Part.CURSOR_POINTER_BEGIN)
        } else if (partToInsert is EditorImagePart) {
            out[out.indexOf(partToInsert)] = partToInsert.setCursor(null)
        }
        return out
    }

    private fun copyClearingCursor(from: List<Part>): List<Part> {
        val out = ArrayList<Part>(from.size)
        from.forEach { out.add(it.clearCursor()) }
        return out
    }

    fun getInitialState(): List<Part> {
        val out = ArrayList<Part>()
        out.add(EditorTextPart("", Part.CURSOR_POINTER_BEGIN))
        return out
    }
}

sealed class EditorInputAction {
    class InsertAction(val part: Part) : EditorInputAction()
    class DeleteAction(val fastDelete: Int?) : EditorInputAction()
}