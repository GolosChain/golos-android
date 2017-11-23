package io.golos.golos.screens.editor

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.golos.golos.R
import io.golos.golos.repository.Repository
import io.golos.golos.utils.ErrorCode
import io.golos.golos.utils.GolosError

/**
 * Created by yuri yurivladdurain@gmail.com on 25/10/2017.
 */
data class EditorState(val error: GolosError? = null,
                       val isLoading: Boolean = false,
                       var completeMessage: Int? = null,
                       val parts: List<EditorPart>)

class EditorViewModel : ViewModel() {
    val editorLiveData = MutableLiveData<EditorState>()
    val titleMaxLength = 255
    val postMaxLength = 100 * 1024
    val commentMaxLength = 16 * 1024
    private val mTextProcessor = TextProcessor()
    private var mTitleText: String = ""
    private var mTags = ArrayList<String>()
    private val mRepository = Repository.get
    var mode: EditorMode? = null

    init {
        editorLiveData.value = EditorState(parts = mTextProcessor.getInitialState())
    }


    fun onUserInput(action: EditorInputAction) {
        val parts = editorLiveData.value?.parts ?: ArrayList()
        editorLiveData.value = EditorState(parts = mTextProcessor.processInput(parts, action))
    }

    fun onTitileChanges(it: CharSequence) {
        mTitleText = it.toString()
    }

    fun onTagsChanged(it: List<String>) {
        mTags = ArrayList(it)
    }

    fun onTextChanged(parts: List<EditorPart>) {
        editorLiveData.value = EditorState(parts = parts)
    }

    fun onSubmit() {
        if (mode == null) throw IllegalStateException("mode must be not null")
        if (mode!!.isPostEditor) {
            if (mTitleText.isEmpty()) {
                editorLiveData.value = EditorState(parts = editorLiveData.value?.parts ?: ArrayList(),
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null, localizedMessage = R.string.enter_title))
                return
            }
            if (editorLiveData.value?.parts?.size ?: 0 == 0 ||
                    (editorLiveData.value?.parts?.size == 1
                            && editorLiveData.value!!.parts[0].markdownRepresentation.isEmpty())) {
                editorLiveData.value = EditorState(parts = editorLiveData.value?.parts ?: ArrayList(),
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null, localizedMessage = R.string.must_be_not_empty))
                return
            }
            if (mTags.size < 1) {
                editorLiveData.value = EditorState(parts = editorLiveData.value?.parts ?: ArrayList(),
                        error = GolosError(ErrorCode.WRONG_STATE, nativeMessage = null, localizedMessage = R.string.at_least_one_tag))
                return
            }
            editorLiveData.value = EditorState(isLoading = true, parts = editorLiveData.value?.parts ?: ArrayList())
            mRepository.createPost(mTitleText, editorLiveData.value?.parts ?: ArrayList(), mTags, { result, error ->
                editorLiveData.value = EditorState(error = error,
                        isLoading = false,
                        parts = editorLiveData.value?.parts ?: ArrayList(),
                        completeMessage = if (error == null) R.string.send_success else null)
            })
        }
    }
}



