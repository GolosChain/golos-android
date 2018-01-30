package io.golos.golos.screens.editor

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import com.fasterxml.jackson.annotation.JsonProperty
import io.golos.golos.App
import io.golos.golos.repository.model.mapper
import io.golos.golos.utils.getString
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * Created by yuri on 29.01.18.
 */
val version = 1

object DraftsPersister : SQLiteOpenHelper(App.context, "drafts.db", null, version) {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(p0: SQLiteDatabase?) {
        p0?.execSQL(DraftsTable.createDefinition)

    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {

    }

    open fun saveDraft(mode: EditorMode, parts: List<EditorPart>, completionHandler: (Unit) -> Unit) {
        executor.execute {
            try {
                DraftsTable.save(mode, parts, writableDatabase)
                handler.post { completionHandler.invoke(Unit) }

            } catch (e: Exception) {
                Timber.e(e)
                e.printStackTrace()
                handler.post { completionHandler.invoke(Unit) }

            }
        }
    }

    open fun getDraft(mode: EditorMode, completionHandler: (List<EditorPart>) -> Unit) {
        executor.execute {
            try {
                val parts = DraftsTable.get(mode, writableDatabase)
                handler.post {
                    completionHandler.invoke(parts)
                }

            } catch (e: Exception) {
                Timber.e(e)
                e.printStackTrace()
                handler.post {
                    completionHandler.invoke(arrayListOf())
                }

            }
        }
    }

    open fun deleteDraft(mode: EditorMode, completionHandler: (Unit) -> Unit) {
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


    private final object DraftsTable {
        private val tableName = "drafts"
        private val modeColumn = "mode"
        private val partsColunm = "parts"
        val createDefinition = "create table  if not exists $tableName ($modeColumn text primary key, $partsColunm text)"

        fun save(mode: EditorMode, parts: List<EditorPart>, db: SQLiteDatabase) {
            Timber.e("save with $mode")
            val parts = parts.map {
                if (it is EditorTextPart) {
                    EditorPartDescriptor("text", it.id, null, null, it.text, it.pointerPosition)
                } else {
                    val imagePart = it as EditorImagePart
                    EditorPartDescriptor("image", it.id, imagePart.imageName, imagePart.imageUrl, null, imagePart.pointerPosition)
                }
            }
            val cv = ContentValues()
            cv.put(modeColumn, mapper.writeValueAsString(mode))
            cv.put(partsColunm, mapper.writeValueAsString(parts))
            db.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }

        fun get(mode: EditorMode, db: SQLiteDatabase): List<EditorPart> {
            val cursor = db.rawQuery("select * from $tableName where $modeColumn = \'${mapper.writeValueAsString(mode)}\'", null)
            val out = ArrayList<EditorPart>(2)

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
                            EditorImagePart(it.id, it.imageName ?: "", it.imageUrl ?: "", it.pointerPosition)
                        }
                    }
                    out.addAll(v)
                }
            }

            cursor.close()
            return out
        }

        fun delete(mode: EditorMode, writableDatabase: SQLiteDatabase?) {
            Timber.e("delete with $mode")
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