package io.golos.golos.screens.editor

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.golos.golos.R
import io.golos.golos.screens.GolosActivity
import io.golos.golos.utils.StringSupplier
import timber.log.Timber.i
import java.util.*
import kotlin.collections.ArrayList

/**
 *
 * **/
data class EditorMode(val title: String = "", val subtitle: String = "", val isPostEditor: Boolean = true)

class EditorActivity : GolosActivity(), Observer<EditorState>, EditorAdapterInteractions {

    private lateinit var mRecycler: RecyclerView
    private lateinit var mToolbar: Toolbar
    private lateinit var mAdapter: EditorAdapter
    private var mMode: EditorMode = EditorMode()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_editor)
        mRecycler = findViewById(R.id.recycler)
        mRecycler.layoutManager = LinearLayoutManager(this)
        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener({ finish() })
        mToolbar = findViewById(R.id.toolbar)
        val viewModel = ViewModelProviders.of(this)[EditorViewModel::class.java]
        viewModel.editorLiveData.observe(this, this)

        if (intent.hasExtra(MODE_TAG)) {
            val mapper = ObjectMapper()
            mapper.registerModule(KotlinModule())
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            mMode = mapper.readValue(intent.getStringExtra(MODE_TAG), EditorMode::class.java)
        }
        mAdapter = EditorAdapter(items = ArrayList(Collections.singletonList(EditorTextPart("", 0))),
                titleText = mMode.title,
                subititleText = mMode.subtitle,
                showTagsEditor = mMode.isPostEditor,
                isTitleEditable = mMode.isPostEditor,
                tagsValidator = TagsStringValidator(object : StringSupplier {
                    override fun get(id: Int): String {
                        return resources.getString(id)
                    }
                }),
                interactor = this)
        mToolbar.title = if (mMode.isPostEditor) resources.getString(R.string.text) else resources.getString(R.string.comment)
        mRecycler.adapter = mAdapter
    }

    override fun onChanged(t: EditorState?) {
        i(t.toString())
    }

    override fun onEdit(parts: List<Part>, title: String, tags: List<String>) {

    }

    override fun onSubmit(parts: List<Part>, title: String, tags: List<String>) {
    }

    override fun onLinkRequest(parts: List<Part>, title: String, tags: List<String>) {
    }

    override fun onPhotoRequest(parts: List<Part>, title: String, tags: List<String>) {
    }

    override fun onPhotoDelete(image: EditorImagePart, parts: List<Part>, title: String, tags: List<String>) {

    }

    companion object {
        val MODE_TAG = "MODE_TAG"
        fun startCommentEditor(ctx: Context, title: String, subtitle: String) {
            val mapper = ObjectMapper()
            mapper.registerModule(KotlinModule())
            val intent = Intent(ctx, EditorActivity::class.java)
            intent.putExtra(MODE_TAG, mapper.writeValueAsString(EditorMode(title, subtitle, false)))
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
