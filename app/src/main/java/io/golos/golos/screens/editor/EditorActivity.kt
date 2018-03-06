package io.golos.golos.screens.editor

import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Button
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.golos.golos.R
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.repository.model.StoryFilter
import io.golos.golos.screens.GolosActivity
import io.golos.golos.screens.stories.model.FeedType
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.screens.widgets.dialogs.OnLinkSubmit
import io.golos.golos.screens.widgets.dialogs.SendLinkDialog
import io.golos.golos.utils.*
import java.io.File
import java.io.FileOutputStream


/**
 *
 * **/
data class EditorMode(@JsonProperty("title")
                      val title: String = "",
                      @JsonProperty("subtitle")
                      val subtitle: String = "",
                      @JsonProperty("postEditor")
                      val isPostEditor: Boolean,
                      @JsonProperty("rootStoryId")
                      val rootStoryId: Long? = null,
                      @JsonProperty("commentToAnswerOnId")
                      val commentToAnswerOnId: Long? = null,
                      @JsonProperty("storyFilter")
                      val storyFilter: StoryFilter? = null,
                      @JsonProperty("feedType")
                      val feedType: FeedType? = null)

class EditorActivity : GolosActivity(), EditorAdapterInteractions, EditorFooter.TagsListener {

    private lateinit var mRecycler: RecyclerView
    private lateinit var mToolbar: Toolbar
    private lateinit var mAdapter: EditorAdapter
    private lateinit var mTitle: EditorTitle
    private lateinit var mFooter: EditorFooter
    private lateinit var mRequestImageButton: View
    private lateinit var mViewModel: EditorViewModel
    private lateinit var mSubmitBtn: Button
    private var mMode: EditorMode? = null
    private var mProgressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_editor)
        mRecycler = findViewById(R.id.recycler)
        mRecycler.layoutManager = LinearLayoutManager(this)
        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener({ finish() })
        mToolbar = findViewById(R.id.toolbar)
        mTitle = findViewById(R.id.title)
        mFooter = findViewById(R.id.footer)
        mSubmitBtn = findViewById(R.id.submit_btn)
        mAdapter = EditorAdapter(interactor = this)
        mRecycler.adapter = mAdapter
        mRecycler.isNestedScrollingEnabled = false
        (mRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        mViewModel = ViewModelProviders.of(this)[EditorViewModel::class.java]
        val mapper = ObjectMapper()
        mSubmitBtn.setCompoundDrawablesWithIntrinsicBounds(null, null, getVectorDrawable(R.drawable.ic_send_blue_white_24dp), null)
        mapper.registerModule(KotlinModule())
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        val modeString = intent.getStringExtra(MODE_TAG)
        mMode = jacksonObjectMapper().readValue(modeString, EditorMode::class.java)
        if (mMode?.isPostEditor == true) {
            mSubmitBtn.text = getString(R.string.publish)
        } else {
            if (mMode?.rootStoryId == mMode?.commentToAnswerOnId) {
                mSubmitBtn.text = getString(R.string.to_comment)
            } else {
                mSubmitBtn.text = getString(R.string.answer)
            }
        }
        mViewModel.mode = mMode
        mViewModel.editorLiveData.observe(this, android.arch.lifecycle.Observer {
            mAdapter.parts = ArrayList(it?.parts ?: ArrayList())
            it?.error?.let {
                if (it.localizedMessage != null) mRecycler.showSnackbar(it.localizedMessage)
                else if (it.nativeMessage != null) mRecycler.showSnackbar(it.nativeMessage)
            }
            if (it?.isLoading == true) {
                if (mProgressDialog == null) mProgressDialog = showProgressDialog()
            } else {
                mProgressDialog?.let {
                    it.dismiss()
                    mProgressDialog = null
                }
            }
            if (mMode?.isPostEditor == true) {
                mTitle.state = EditorTitleState(it?.title ?: "",
                        mTitle.state.isTitleEditable,
                        mTitle.state.onTitleChanges,
                        mTitle.state.subtitle,
                        mTitle.state.isHidden)
            }
            mFooter.state = EditorFooterState(mMode?.isPostEditor == true,
                    mFooter.state.tagsValidator,
                    it?.tags?.toArrayList() ?: arrayListOf(),
                    mFooter.state.tagsListener)
            it?.completeMessage?.let {
                mRecycler.showSnackbar(it)
                Handler().postDelayed({
                    finish()
                }, 30)
            }
        })
        mTitle.state = EditorTitleState(mMode?.title ?: "", mMode?.isPostEditor ?: false, {
            mViewModel.onTitleChanged(it)
        }, mMode?.subtitle ?: "",
                mMode?.isPostEditor == false
                        && mMode?.rootStoryId == mMode?.commentToAnswerOnId)
        mFooter.state = EditorFooterState(mMode?.isPostEditor == true,
                TagsStringValidator(object : StringSupplier {
                    override fun get(id: Int): String {
                        return getString(id)
                    }
                }),
                ArrayList(),
                this)
        mToolbar.title = if (mMode?.isPostEditor == true) resources.getString(R.string.text) else resources.getString(R.string.comment)
        mRequestImageButton = findViewById(R.id.btn_insert_image)
        mRequestImageButton.setOnClickListener({
            val readExternalPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
            if (readExternalPermission) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "image/jpeg"
                    startActivityForResult(intent, PICK_IMAGE_ID)
                } else {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "image/*"
                    startActivityForResult(intent, PICK_IMAGE_ID)
                }

            } else {
                ActivityCompat.requestPermissions(this, Array(1, { android.Manifest.permission.READ_EXTERNAL_STORAGE }), READ_EXTERNAL_PERMISSION)
            }
        })
        findViewById<View>(R.id.btn_link).setOnClickListener({
            val fr = SendLinkDialog.getInstance()
            fr.listener = object : OnLinkSubmit {
                override fun submit(linkName: String, linkAddress: String) {
                    val text = " [$linkName]($linkAddress)"
                    mViewModel.onUserInput(EditorInputAction.InsertAction(
                            EditorTextPart(text = text, pointerPosition = text.length)))
                }
            }
            fr.show(supportFragmentManager, null)
        })
        mSubmitBtn.setOnClickListener({
            mViewModel.onSubmit()
            mRecycler.hideKeyboard()
        })
    }

    override fun onTagsSubmit(tags: List<String>) {
        mViewModel.onTagsChanged(tags)
    }

    override fun onEdit(parts: List<EditorPart>) {
        mViewModel.onTextChanged(parts)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_ID) {
            val handler = Handler(Looper.getMainLooper())
            if (data != null) {
               Thread(Runnable {
                    try {
                        val inputStream = contentResolver.openInputStream(data.data)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap == null) handler.post {
                            mToolbar.showSnackbar(R.string.wrong_image)
                            return@post
                        }
                        val f = File(cacheDir, System.currentTimeMillis().toString() + ".jpg")
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(f))
                        resizeToSize(f)
                        handler.post {
                            mViewModel
                                    .onUserInput(EditorInputAction.InsertAction(EditorImagePart(imageName = f.name,
                                            imageUrl = "file://${f.absolutePath}", pointerPosition = null)))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        handler.post {
                            mToolbar.showSnackbar(R.string.wrong_image)
                        }
                    }
                }).start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_EXTERNAL_PERMISSION &&
                grantResults.size > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) mRequestImageButton.performClick()
    }


    override fun onPhotoDelete(image: EditorImagePart, parts: List<EditorPart>) {
        mViewModel.onUserInput(EditorInputAction.DeleteAction(parts.indexOf(image)))
    }

    private fun resizeToSize(imageFile: File) {

        if (imageFile.sizeInKb() < 800) return
        val optns = BitmapFactory.Options()
        optns.inSampleSize = 2
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath, optns)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(imageFile))
        var step = 1
        while (imageFile.sizeInKb() > 800) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90 - (step * 10), FileOutputStream(imageFile))
            step++
        }
    }

    companion object {
        @JvmStatic
        private val PICK_IMAGE_ID = nextInt()
        @JvmStatic
        private val READ_EXTERNAL_PERMISSION = nextInt()
        val MODE_TAG = "MODE_TAG"
        fun startRootCommentEditor(ctx: Context,
                                   rootStory: StoryWithComments,
                                   feedType: FeedType,
                                   storyFilter: StoryFilter?) {
            val mapper = jacksonObjectMapper()
            val intent = Intent(ctx, EditorActivity::class.java)
            intent.putExtra(MODE_TAG, mapper.writeValueAsString(EditorMode(rootStory.rootStory()!!.title,
                    rootStory.rootStory()!!.author,
                    false,
                    rootStory.rootStory()!!.id,
                    rootStory.rootStory()!!.id,
                    storyFilter,
                    feedType)))
            ctx.startActivity(intent)
        }

        fun startAnswerOnCommentEditor(ctx: Context,
                                       rootStory: StoryWithComments,
                                       commentToAnswer: GolosDiscussionItem,
                                       feedType: FeedType,
                                       storyFilter: StoryFilter?) {
            val mapper = jacksonObjectMapper()
            val intent = Intent(ctx, EditorActivity::class.java)
            intent.putExtra(MODE_TAG, mapper.writeValueAsString(EditorMode(rootStory.rootStory()!!.title,
                    rootStory.rootStory()!!.author,
                    false,
                    rootStory.rootStory()!!.id,
                    commentToAnswer.id,
                    storyFilter,
                    feedType)))
            ctx.startActivity(intent)
        }

        fun startPostEditor(ctx: Context, title: String) {
            val intent = Intent(ctx, EditorActivity::class.java)
            val mode = EditorMode(title, isPostEditor = true)
            val string = jacksonObjectMapper().writeValueAsString(mode)
            intent.putExtra(MODE_TAG, string)
            ctx.startActivity(intent)
        }
    }
}
