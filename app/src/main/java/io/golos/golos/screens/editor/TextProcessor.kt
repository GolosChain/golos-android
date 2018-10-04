package io.golos.golos.screens.editor

import android.text.Editable
import android.text.SpannableStringBuilder
import java.util.*

/**
 * Created by yuri yurivladdurain@gmail.com on 26/10/2017.
 */
object TextProcessor {

    fun processInput(previousState: List<EditorPart>, action: EditorInputAction): List<EditorPart> {
        val parts = previousState
        val selectedPart = parts.findLast { it.isFocused() }

        val cursorPosition: Int? = selectedPart?.endPointer
        val out = ArrayList<EditorPart>()
        if (selectedPart == null || cursorPosition == null) {
            out.addAll(previousState)
            if (action is EditorInputAction.InsertAction) {
                appendPartToTheEnd(action.part, out)
            } else if (action is EditorInputAction.DeleteAction) {
                if (out.isEmpty()) out.add(EditorTextPart.emptyTextPart())
                else {
                    if (action.fastDelete != null) out.removeAt(action.fastDelete)
                }

            }
        } else {
            if (parts.isEmpty()) out.add(EditorTextPart(text = "".asSpannable(),
                    startPointer = EditorPart.CURSOR_POINTER_BEGIN, endPointer = EditorPart.CURSOR_POINTER_BEGIN))
            else {
                when (action) {
                    is EditorInputAction.InsertAction -> {
                        val index = parts.indexOf(selectedPart)
                        val partToInsert = action.part

                        if (selectedPart is EditorImagePart) {
                            if (index < parts.lastIndex) {//if we insert part somewhere in middle
                                out.addAll(parts)
                                out.add(index + 1, partToInsert)
                            } else {//if we insert at the end
                                out.addAll(parts)
                                out.add(partToInsert)
                            }

                        } else if (selectedPart is EditorTextPart) {
                            out.addAll(parts)
                            val insertResult = insertPartIntoOtherPart(partToInsert, selectedPart, cursorPosition)
                            out.removeAt(index)//deleting old part
                            out.addAll(index, insertResult)//insert result into resulting list
                        }
                    }
                    is EditorInputAction.DeleteAction -> {
                        out.addAll(parts)
                        if (action.fastDelete != null && action.fastDelete < out.size) {
                            out.removeAt(action.fastDelete)
                        }

                    }
                    is EditorInputAction.ReplaceAction -> {
                        out.addAll(parts)
                        if (selectedPart is EditorTextPart) {
                            selectedPart.text.replace(selectedPart.startPointer, selectedPart.endPointer, action.with)
                            selectedPart.endPointer = selectedPart.startPointer + action.with.length
                        }
                    }
                }

                if (out.isEmpty()) out.add(EditorTextPart(text = "".asSpannable(),
                        startPointer = EditorPart.CURSOR_POINTER_BEGIN,
                        endPointer = EditorPart.CURSOR_POINTER_BEGIN))
            }
        }
        addEditTextDelimeters(out)
        removeDoublingTextBlocks(out)
        return out

    }

    private fun addEditTextDelimeters(to: ArrayList<EditorPart>) {
        if (to[0] is EditorImagePart) to.add(0, EditorTextPart.emptyTextPart())
        if (to.last() is EditorImagePart) to.add(EditorTextPart.emptyTextPart())

        val iter = to.listIterator()
        while (iter.hasNext()) {
            val part = iter.next()
            if (part is EditorImagePart) {
                if (to[iter.nextIndex()] !is EditorTextPart) {
                    iter.add(EditorTextPart.emptyTextPart())
                }
            }
        }
    }

    private fun appendPartToTheEnd(part: EditorPart, to: ArrayList<EditorPart>) {
        if (part is EditorTextPart) {
            to.add(part)
        } else if (part is EditorImagePart) {
            to.add(part)
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
                    val outText = currentPart.text.append(next.text)
                    if (currentPart.isFocused()) {
                        focus = currentPart.endPointer
                    } else if (next.isFocused()) {
                        focus = currentPart.text.length + next.endPointer
                    }
                    iter.set(EditorTextPart(currentPart.id, outText,
                            focus ?: EditorPart.CURSOR_POINTER_NOT_SELECTED,
                            focus ?: EditorPart.CURSOR_POINTER_NOT_SELECTED))
                    deleteNext = true
                }
            }
        }
    }

    private fun insertPartIntoOtherPart(partToInsert: EditorPart,
                                        into: EditorTextPart,
                                        position: Int): List<EditorPart> {
        val out = ArrayList<EditorPart>()

        if (partToInsert is EditorImagePart) {
            when {
                position == 0 -> {//first
                    out.add(partToInsert)
                    out.add(into)
                }
                position >= into.text.length -> {//last
                    out.add(into)
                    out.add(partToInsert)
                }
                else -> {//somewhere in the middle
                    val slices = into.text.slice(position)
                    out.add(EditorTextPart(text = slices.first))
                    out.add(partToInsert)
                    out.add(EditorTextPart(into.id,
                            slices.second,
                            EditorPart.CURSOR_POINTER_BEGIN,
                            EditorPart.CURSOR_POINTER_BEGIN))
                }
            }

        } else if (partToInsert is EditorTextPart) {
            when {
                position == 0 -> {//first
                    val resultText = into.text.prepend(partToInsert.text)
                    out.add(EditorTextPart(into.id,
                            resultText,
                            partToInsert.text.length, partToInsert.text.length))
                }
                position >= into.text.length -> {//last
                    val resultText = into.text.append(partToInsert.text)
                    out.add(EditorTextPart(into.id,
                            resultText,
                            resultText.length, resultText.length))
                }
                else -> {//somewhere in the middle
                    val result = into.text.insert(position, partToInsert.text)
                    out.add(EditorTextPart(into.id,
                            result,
                            position + partToInsert.text.length,
                            position + partToInsert.text.length))
                }
            }
        }

        return out
    }

    fun getInitialState(additionToStart: CharSequence? = null): List<EditorPart> {
        val out = ArrayList<EditorPart>()
        val text = SpannableStringBuilder.valueOf("").apply { if (additionToStart != null) this.append(additionToStart) }
        out.add(EditorTextPart(text = text,
                startPointer = text.length, endPointer = text.length))

        return out
    }
}

sealed class EditorInputAction {
    data class InsertAction(val part: EditorPart) : EditorInputAction()
    data class DeleteAction(val fastDelete: Int?) : EditorInputAction()
    data class ReplaceAction(val with: Editable) : EditorInputAction()
}