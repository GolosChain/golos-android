package io.golos.golos.repository

import io.golos.golos.R
import io.golos.golos.repository.api.GolosApi
import io.golos.golos.repository.model.CreatePostResult
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.screens.editor.EditorImagePart
import io.golos.golos.screens.editor.EditorPart
import io.golos.golos.utils.*
import timber.log.Timber
import java.io.File

/**
 * Created by yuri on 09.03.18.
 */
internal class Poster(private val mRepository: Repository,
                      private val mGolosApi: GolosApi,
                      private val mLogger: ExceptionLogger?) {

    private enum class EditType {
        CREATE_POST, EDIT_POST, CREATE_COMMENT, EDIT_COMMENT
    }

    fun createPost(title: String,
                   content: List<EditorPart>,
                   tags: List<String>): Pair<CreatePostResult?, GolosError?> {
        return createOrEditPostInternal(title = title,
                content = content,
                type = EditType.CREATE_POST,
                tags = tags)
    }

    fun editPost(originalPostPermlink: String,
                 title: String,
                 content: List<EditorPart>,
                 tags: List<String>): Pair<CreatePostResult?, GolosError?> {

        return createOrEditPostInternal(title,
                content,
                originalPostPermlink,
                type = EditType.EDIT_POST,
                tags = tags)
    }

    fun createComment(toItem: GolosDiscussionItem,
                      content: List<EditorPart>): Pair<CreatePostResult?, GolosError?> {
        return createOrEditPostInternal(content = content,
                parentPermlink = toItem.permlink,
                parentAuthor = toItem.author,
                categoryName = toItem.categoryName,
                type = EditType.CREATE_COMMENT)
    }

    fun editComment(originalComment: GolosDiscussionItem,
                    content: List<EditorPart>): Pair<CreatePostResult?, GolosError?> {
        return createOrEditPostInternal(content = content,
                parentPermlink = originalComment.parentPermlink,
                parentAuthor = originalComment.parentAuthor,
                categoryName = originalComment.categoryName,
                type = EditType.EDIT_COMMENT,
                originalPostOrCommentPermlink = originalComment.permlink)
    }

    private fun createOrEditPostInternal(title: String? = null,
                                         content: List<EditorPart>,
                                         originalPostOrCommentPermlink: String? = null,
                                         parentPermlink: String? = null,
                                         parentAuthor: String? = null,
                                         categoryName: String? = null,
                                         type: EditType,
                                         tags: List<String> = listOf()): Pair<CreatePostResult?, GolosError?> {
        if (!mRepository.isUserLoggedIn()) {
            return Pair(null, GolosError(ErrorCode.ERROR_AUTH, null, R.string.wrong_credentials))
        }
        val tags = ArrayList(tags)
        val content = ArrayList(content)

        try {
            (0 until tags.size)
                    .forEach {
                        if (tags[it].contains(Regex("[а-яА-Я]"))) {
                            tags[it] = "ru--${Translit.ru2lat(tags[it])}"
                        }
                        tags[it] = tags[it].toLowerCase().replace(" ", "")
                    }
            (0 until content.size)
                    .forEach {
                        val part = content[it]
                        if (part is EditorImagePart) {
                            val newUrl = if (part.imageUrl.startsWith("/") || part.imageUrl.startsWith("file")) mGolosApi.uploadImage(mRepository.appUserData.value?.userName!!,
                                    File(part.imageUrl))
                            else part.imageUrl
                            content[it] = EditorImagePart(part.id, part.imageName, newUrl)
                        }
                    }
            val content = content.joinToString(separator = "\n") { it.htmlRepresentation }

            val result = when (type) {

                EditType.CREATE_POST -> {
                    if (title == null)
                        throw IllegalArgumentException("for $type title  must be provided")
                    mGolosApi.sendPost(mRepository.appUserData.value?.userName!!,
                            title, content, tags.toArrayList().toArray(Array(tags.size, { "" })))
                }
                EditType.EDIT_POST -> {

                    if (originalPostOrCommentPermlink == null || title == null) throw IllegalArgumentException("for $type original post and title" +
                            " argument must be provided")
                    mGolosApi.editPost(originalPostOrCommentPermlink, mRepository.appUserData.value?.userName!!,
                            title, content, tags.toArrayList().toArray(Array(tags.size, { "" })))
                }
                EditType.CREATE_COMMENT -> {
                    if (parentAuthor == null || parentPermlink == null || categoryName == null)
                        throw IllegalArgumentException("for $type parentPermlink and " +
                                " parentAuthor and categoryName  must be provided")
                    mGolosApi.sendComment(mRepository.appUserData.value?.userName!!,
                            parentAuthor, parentPermlink, content, categoryName)
                }
                EditType.EDIT_COMMENT -> {
                    if (parentAuthor == null || parentPermlink == null || originalPostOrCommentPermlink == null || categoryName == null)
                        throw IllegalArgumentException("for $type parentPermlink," +
                                " parentAuthor,categoryName and originalPostOrCommentPermlink must be provided")
                    mGolosApi.editComment(mRepository.appUserData.value?.userName!!,
                            parentAuthor, parentPermlink, originalPostOrCommentPermlink, content, categoryName)
                }
            }
            if (type == EditType.CREATE_POST || type == EditType.EDIT_POST) result.isPost = true
            return Pair(result, null)

        } catch (e: Exception) {
            Timber.e(e)
            e.printStackTrace()
            mLogger?.log(e)
            return Pair(null, GolosErrorParser.parse(e))
        }
    }
}