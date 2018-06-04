package io.golos.golos.screens.editor

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import com.fasterxml.jackson.annotation.JsonProperty
import io.golos.golos.App
import io.golos.golos.utils.mapper
import io.golos.golos.utils.getString
import io.golos.golos.utils.toArrayList
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * Created by yuri on 29.01.18.
 */
val version = 3

object DraftsPersister : SQLiteOpenHelper(App.context, "drafts.db", null, version) {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(p0: SQLiteDatabase?) {
        p0?.execSQL(DraftsTable.createDefinition)

    }

    override fun onUpgrade(p0: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1 && newVersion == 2) {
            p0?.execSQL("alter table ${DraftsTable.tableName} ADD COLUMN ${DraftsTable.titleColumn} text")
            p0?.execSQL("alter table ${DraftsTable.tableName} ADD COLUMN ${DraftsTable.tagsColumn} text")
        }
        if (newVersion == 3) {
            p0?.delete(DraftsTable.tableName, null, null)
        }
    }

    fun saveDraft(mode: EditorMode,
                       parts: List<EditorPart>,
                       title: String,
                       tags: List<String>,
                       completionHandler: (Unit) -> Unit) {
        executor.execute {
            try {
                DraftsTable.save(mode,
                        parts,
                        title,
                        tags,
                        writableDatabase)
                handler.post { completionHandler.invoke(Unit) }

            } catch (e: Exception) {
                Timber.e(e)
                e.printStackTrace()
                handler.post { completionHandler.invoke(Unit) }

            }
        }
    }

    fun getDraft(mode: EditorMode,
                      completionHandler: (List<EditorPart>, String, List<String>) -> Unit) {
        executor.execute {
            try {
                val parts = DraftsTable.get(mode, writableDatabase)
                handler.post {
                    completionHandler.invoke(parts.first, parts.second, parts.third)
                }

            } catch (e: Exception) {
                Timber.e(e)
                e.printStackTrace()
                handler.post {
                    completionHandler.invoke(arrayListOf(), "", listOf())
                }
            }
        }
    }

    fun deleteDraft(mode: EditorMode, completionHandler: (Unit) -> Unit) {
        executor.execute {
            try {
                DraftsTable.delete(mode, writableDatabase)
                handler.post {
                    completionHandler.invoke(Unit)
                }

            } catch (e: Exception) {
                Timber.e(e)
                e.printStackTrace()
                handler.post {
                    completionHandler.invoke(Unit)
                }

            }
        }
    }


    final object DraftsTable {
        val tableName = "drafts"
        val modeColumn = "mode"
        val partsColunm = "parts"
        val titleColumn = "titleColumn"
        val tagsColumn = "tagsColumn"
        val createDefinition = "create table  if not exists $tableName ($modeColumn text primary key," +
                " $partsColunm text, $titleColumn text, $tagsColumn text)"

        fun save(mode: EditorMode,
                 parts: List<EditorPart>,
                 title: String,
                 tags: List<String>,
                 db: SQLiteDatabase) {
            val parts = parts.map {
                if (it is EditorTextPart) {
                    EditorPartDescriptor("text", it.id, null, null, it.text, it.pointerPosition)
                } else {
                    val imagePart = it as EditorImagePart
                    EditorPartDescriptor("image", it.id, imagePart.imageName, imagePart.imageUrl, null, imagePart.pointerPosition)
                }
            }.toArrayList()


            val cv = ContentValues()
            cv.put(modeColumn, mapper.writeValueAsString(mode))
            cv.put(partsColunm, mapper.writeValueAsString(parts))
            cv.put(titleColumn, title)
            cv.put(tagsColumn, mapper.writeValueAsString(tags))
            db.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }

        fun get(mode: EditorMode, db: SQLiteDatabase): Triple<List<EditorPart>, String, List<String>> {
            val cursor = db.rawQuery("select * from $tableName where $modeColumn = \'${mapper.writeValueAsString(mode)}\'", null)
            val out = ArrayList<EditorPart>(2)
            var title = ""
            var tags = listOf("")
            if (cursor.count != 0) {
                cursor.moveToFirst()
                val parts = cursor.getString(partsColunm)
                parts?.let {
                    val type = mapper.typeFactory.constructCollectionType(List::class.java, EditorPartDescriptor::class.java)
                    val descriptors = mapper.readValue<List<EditorPartDescriptor>>(it, type)
                    val v = descriptors.map {
                        if (it.type == "text") {
                            EditorTextPart(it.id, it.text ?: "", it.pointerPosition)
                        } else {
                            EditorImagePart(it.id, it.imageName ?: "", it.imageUrl
                                    ?: "", it.pointerPosition)
                        }
                    }

                    out.addAll(v)
                }
                title = cursor.getString(titleColumn) ?: ""
                val tagsString = cursor.getString(tagsColumn)
                if (!tagsString.isNullOrEmpty()) {
                    val type = mapper.typeFactory.constructCollectionType(List::class.java, String::class.java)
                    tags = mapper.readValue<List<String>>(tagsString, type)
                }
            }

            cursor.close()
            return Triple(out, title, tags)
        }

        fun delete(mode: EditorMode, writableDatabase: SQLiteDatabase?) {
            writableDatabase?.delete(tableName, " $modeColumn = \'${mapper.writeValueAsString(mode)}\'", null)
        }

        data class EditorPartDescriptor constructor(
                @JsonProperty("type")
                val type: String,
                @JsonProperty("id")
                val id: String,
                @JsonProperty("imageName")
                val imageName: String?,
                @JsonProperty("imageUrl")
                val imageUrl: String?,
                @JsonProperty("text")
                val text: String?,
                @JsonProperty("pointerPosition")
                val pointerPosition: Int?)


    }
}