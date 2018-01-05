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

/**
 * Created by yuri on 06.11.17.
 */
private val dbVersion = 1

class SqliteDb(ctx: Context) : SQLiteOpenHelper(ctx, "mydb.db", null, dbVersion) {
    private val mAvatarsTable = AvatarsTable()
    private val mTagsTable = TagsTable()

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(mAvatarsTable.createTableString)
        db?.execSQL(mTagsTable.createTableString)
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

    private class AvatarsTable {
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

    private class TagsTable {
        private val databaseName = "tag"
        private val tagName = "name"
        private val payoutInGbg = "payoutInGbg"
        private val votesCount = "votes"
        private val topPostsCount = "topPostsCount"
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

        fun getTagsFromDb(db: SQLiteDatabase): List<Tag> {
            val cursor = db.query(databaseName, arrayOf(tagName, payoutInGbg, votesCount, topPostsCount),
                    "*", null, null, null, "order by $payoutInGbg desc")
            if (cursor.count == 0) return listOf()
            cursor.moveToFirst()
            val out = ArrayList<Tag>()
            while (!cursor.isAfterLast) {
                val tag = Tag(cursor.getString(tagName),
                        cursor.getDouble(payoutInGbg),
                        cursor.getLong(votesCount),
                        cursor.getLong(topPostsCount))
                out.add(tag)
            }
            cursor.close()
            return out
        }
    }
}