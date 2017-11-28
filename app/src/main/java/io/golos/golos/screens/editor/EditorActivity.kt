package io.golos.golos.screens.editor

import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.support.v7.widget.Toolbar
import android.view.View
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity
import io.golos.golos.repository.model.GolosDiscussionItem
import io.golos.golos.screens.story.model.StoryTree
import io.golos.golos.screens.widgets.OnLinkSubmit
import io.golos.golos.screens.widgets.SendLinkFragment
import io.golos.golos.utils.*
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream


/**
 *
 * **/
data class EditorMode(val title: String = "",
                      val subtitle: String = "",
                      val isPostEditor: Boolean,
                      val rootStory: StoryTree? = null,
                      val commentToAnswerOn: GolosDiscussionItem? = null)

class EditorActivity : GolosActivity(), EditorAdapterInteractions, EditorFooter.TagsListener {

    private lateinit var mRecycler: RecyclerView
    private lateinit var mToolbar: Toolbar
    private lateinit var mAdapter: EditorAdapter
    private lateinit var mTitle: EditorTitle
    private lateinit var mFooter: EditorFooter
    private lateinit var mRequestImageButton: View
    private lateinit var mViewModel: EditorViewModel
    private var mMode: EditorMode? = null
    private val PICK_IMAGE_ID = nextInt()
    private val READ_EXTERNAL_PERMISSION = nextInt()
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
        mAdapter = EditorAdapter(interactor = this)
        mRecycler.adapter = mAdapter
        mRecycler.isNestedScrollingEnabled = false
        (mRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        mViewModel = ViewModelProviders.of(this)[EditorViewModel::class.java]
        val mapper = ObjectMapper()
        mapper.registerModule(KotlinModule())
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        mMode = mapper.readValue(intent.getStringExtra(MODE_TAG), EditorMode::class.java)
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
            Timber.e(it.toString())
            it?.completeMessage?.let {
                mRecycler.showSnackbar(it)
                Handler().postDelayed({
                    finish()
                }, 3000)
            }
        })
        mTitle.state = EditorTitleState(mMode?.title ?: "", mMode?.isPostEditor ?: false, {
            mViewModel.onTitileChanges(it)
        }, mMode?.subtitle ?: "")
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
                val intent = Intent();
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(intent, PICK_IMAGE_ID)
            } else {
                ActivityCompat.requestPermissions(this, Array(1, { android.Manifest.permission.READ_EXTERNAL_STORAGE }), READ_EXTERNAL_PERMISSION)
            }
        })
        findViewById<View>(R.id.btn_link).setOnClickListener({
            val fr = SendLinkFragment.getInstance()
            fr.listener = object : OnLinkSubmit {
                override fun submit(linkName: String, linkAddress: String) {
                    val text = " [$linkName]($linkAddress)"
                    mViewModel.onUserInput(EditorInputAction.InsertAction(
                            EditorTextPart(text = text, pointerPosition = text.length)))
                }
            }
            fr.show(supportFragmentManager, null)
        })
        findViewById<View>(R.id.submit_btn).setOnClickListener({
            mViewModel.onTagsChanged(mFooter.state.tags)
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
            if (data != null) {
                try {
                    val inputStream = contentResolver.openInputStream(data.data)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap == null) mToolbar.showSnackbar(R.string.wrong_image)
                    val f = File(cacheDir, System.currentTimeMillis().toString() + ".jpg")
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(f))
                    mViewModel
                            .onUserInput(EditorInputAction.InsertAction(EditorImagePart(imageName = f.name,
                                    imageUrl = "file://${f.absolutePath}", pointerPosition = null)))
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                    mToolbar.showSnackbar(R.string.wrong_image)
                }
            }
        }
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

    companion object {
        val MODE_TAG = "MODE_TAG"
        fun startRootCommentEditor(ctx: Context,
                                   rootStory: StoryTree) {
            val mapper = ObjectMapper()
            mapper.registerModule(KotlinModule())
            val intent = Intent(ctx, EditorActivity::class.java)
            intent.putExtra(MODE_TAG, mapper.writeValueAsString(EditorMode(rootStory.rootStory()!!.title,
                    rootStory.rootStory()!!.author,
                    false,
                    rootStory,
                    rootStory.rootStory()!!)))
            ctx.startActivity(intent)
        }

        fun startAnswerOnCommentEditor(ctx: Context,
                                       rootStory: StoryTree,
                                       commentToAnswer: GolosDiscussionItem) {
            val mapper = ObjectMapper()
            mapper.registerModule(KotlinModule())
            val intent = Intent(ctx, EditorActivity::class.java)
            intent.putExtra(MODE_TAG, mapper.writeValueAsString(EditorMode(rootStory.rootStory()!!.title,
                    rootStory.rootStory()!!.author,
                    false,
                    rootStory,
                    commentToAnswer)))
            ctx.startActivity(intent)
        }

        fun startPostEditor(ctx: Context, title: String) {
            val mapper = ObjectMapper()
            mapper.registerModule(KotlinModule())
            val intent = Intent(ctx, EditorActivity::class.java)
            intent.putExtra(MODE_TAG, mapper.writeValueAsString(EditorMode(title, isPostEditor = true)))
            ctx.startActivity(intent)
        }
    }
}
