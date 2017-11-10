package io.golos.golos.screens.editor

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

/**
 * Created by yuri yurivladdurain@gmail.com on 25/10/2017.
 */
class EditorViewModel : ViewModel() {
    private var mPreviousEditorState = EditorState(ArrayList())
    val editorLiveData = MutableLiveData<EditorState>()
    val titleMaxLength = 255
    val postMaxLength = 100 * 1024
    val commentMaxLength = 16 * 1024
    private val mTextProcessor = TextProcessor()

    init {
        mPreviousEditorState = EditorState(mTextProcessor.getInitialState())
    }

    fun onUserInput(action: EditorInputAction) {
        val parts = mPreviousEditorState.parts
        editorLiveData.value = EditorState(mTextProcessor.processInput(parts, action))
    }
}

class EditorState(val parts: List<Part>)

