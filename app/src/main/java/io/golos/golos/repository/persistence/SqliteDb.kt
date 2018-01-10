package io.golos.golos.repository.persistence

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.golos.golos.repository.model.Tag
import io.golos.golos.repository.persistence.model.UserAvatar
import io.golos.golos.utils.getDouble
import io.golos.golos.utils.getLong
import io.golos.golos.utils.getString
import timber.log.Timber

/**
 * Created by yuri on 06.11.17.
 */
private val dbVersion = 1

class SqliteDb(ctx: Context) : SQLiteOpenHelper(ctx, "mydb.db", null, dbVersion) {
    private val mAvatarsTable = AvatarsTable
    private val mTagsTable = TagsTable
    private val mUserFilterTable = UserFilterTable

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(mAvatarsTable.createTableString)
        db?.execSQL(mTagsTable.createTableString)
        db?.execSQL(mUserFilterTable.createTableString)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

    fun saveAvatar(avatar: UserAvatar) {
        mAvatarsTable.saveAvatarToTable(writableDatabase, avatar)
    }

    fun getAvatar(userName: String): UserAvatar? {
        return mAvatarsTable.getAvatarFromDb(writableDatabase, userName)
    }

    fun saveTags(list: List<Tag>) {
        mTagsTable.saveTagsToTable(writableDatabase, list)
    }


    fun getTags(): List<Tag> {
        return mTagsTable.getTagsFromDb(writableDatabase)
    }

    fun saveTagsForFilter(tags: List<Tag>, filter: String) {
        mUserFilterTable.saveTagsToTable(writableDatabase, tags, filter)
    }

    fun getTagsForFilter(filter: String): List<Tag> {
        return mUserFilterTable.getAllTagsWithFilter(writableDatabase, mTagsTable, filter)
    }

    private object AvatarsTable {
        private val databaseName = "avatars"
        private val avatarPathColumn = "avatar_path"
        private val userNameColumn = "user_name"
        private val dateColumn = "date"
        val createTableString = "create table if not exists $databaseName ( $userNameColumn text primary key," +
                "$avatarPathColumn text, $dateColumn integer)"

        fun saveAvatarToTable(db: SQLiteDatabase, avatar: UserAvatar) {
            val values = ContentValues()
            values.put(avatarPathColumn, avatar.avatarPath)
            values.put(userNameColumn, avatar.userName)
            values.put(dateColumn, avatar.dateUpdated)
            db.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }

        fun getAvatarFromDb(db: SQLiteDatabase, username: String): UserAvatar? {
            val cursor = db.query(databaseName, arrayOf(avatarPathColumn, userNameColumn, dateColumn),
                    "$userNameColumn = \'$username\'", null, null, null, null)
            if (cursor.count == 0) return null
            cursor.moveToFirst()
            val avatar = UserAvatar(cursor.getString(userNameColumn),
                    cursor.getString(avatarPathColumn),
                    cursor.getLong(dateColumn))
            cursor.close()
            return avatar
        }
    }

    private object UserFilterTable {
        private val databaseName = "filter_table"
        private val tagName = "name"
        private val filterName = "filter"
        val createTableString = "create table if not exists $databaseName ( $tagName text ," +
                "$filterName text )"

        fun saveTagsToTable(db: SQLiteDatabase, tags: List<Tag>, filterName: String) {
            Timber.e("saveTagsToTable $tags")
            db.delete(databaseName, "${UserFilterTable.filterName} = \'$filterName\'", null)
            val values = ContentValues()
            db.beginTransaction()
            tags.forEach {
                values.put(tagName, it.name)
                values.put(this.filterName, filterName)
                db.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            db.endTransaction()
        }

        fun getAllTagsWithFilter(db: SQLiteDatabase,
                                 tagsTable: TagsTable,
                                 filterName: String): List<Tag> {
            val cursor =
                    db.rawQuery(" select ${tagsTable.databaseName}.${tagsTable.tagName}, ${tagsTable.payoutInGbg}, ${tagsTable.votesCount},  ${tagsTable.topPostsCount}" +
                            " from ${tagsTable.databaseName} " +
                            " inner join $databaseName on $databaseName.$tagName = ${tagsTable.databaseName}.${tagsTable.tagName} " +
                            " where ${this.filterName} = \'$filterName\'", null)
            if (cursor.count == 0) return listOf()
            cursor.moveToFirst()
            val out = ArrayList<Tag>()
            while (!cursor.isAfterLast) {
                val tag = Tag(cursor.getString(TagsTable.tagName),
                        cursor.getDouble(TagsTable.payoutInGbg),
                        cursor.getLong(TagsTable.votesCount),
                        cursor.getLong(TagsTable.topPostsCount))
                out.add(tag)
                cursor.moveToNext()
            }
            cursor.close()
            return out
        }

        fun getAllTags(db: SQLiteDatabase): List<String> {
            val cursor = db.query(databaseName, arrayOf(tagName),
                    "distinct", null, null, null, null)
            val out = ArrayList<String>()
            while (!cursor.isAfterLast) {
                out.add(cursor.getString(filterName))
                cursor.moveToNext()
            }
            cursor.close()
            return out
        }
    }

    private object TagsTable {
        val databaseName = "tag"
        val tagName = "name"
        val payoutInGbg = "payoutInGbg"
        val votesCount = "votes"
        val topPostsCount = "topPostsCount"
        val createTableString = "create table if not exists $databaseName ( $tagName text primary key," +
                "$payoutInGbg real, $votesCount integer, $topPostsCount integer)"

        fun saveTagsToTable(db: SQLiteDatabase, tags: List<Tag>) {
            val values = ContentValues()
            db.beginTransaction()
            tags.forEach {
                values.put(tagName, it.name)
                values.put(payoutInGbg, it.payoutInGbg)
                values.put(votesCount, it.votes)
                values.put(topPostsCount, it.topPostsCount)
                db.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            db.endTransaction()
        }

        fun getTagsFromDb(db: SQLiteDatabase, withName: List<String>) {
            var selectionArg = ""
            selectionArg.forEach {
                selectionArg += " $tagName = \"$it\""
            }
            val cursor = db.query(databaseName, arrayOf(tagName, payoutInGbg, votesCount, topPostsCount),
                    selectionArg, null, null, null, "$payoutInGbg desc")
            cursor.moveToFirst()
            val out = ArrayList<Tag>()
            while (!cursor.isAfterLast) {
                val tag = Tag(cursor.getString(tagName),
                        cursor.getDouble(payoutInGbg),
                        cursor.getLong(votesCount),
                        cursor.getLong(topPostsCount))
                out.add(tag)
                cursor.moveToNext()
            }
            cursor.close()
        }

        fun getTagsFromDb(db: SQLiteDatabase): List<Tag> {
            val cursor = db.query(databaseName, arrayOf(tagName, payoutInGbg, votesCount, topPostsCount),
                    null, null, null, null, "$payoutInGbg desc")
            if (cursor.count == 0) return listOf()
            cursor.moveToFirst()
            val out = ArrayList<Tag>()
            while (!cursor.isAfterLast) {
                val tag = Tag(cursor.getString(tagName),
                        cursor.getDouble(payoutInGbg),
                        cursor.getLong(votesCount),
                        cursor.getLong(topPostsCount))
                out.add(tag)
                cursor.moveToNext()
            }
            cursor.close()
            return out
        }
    }
}