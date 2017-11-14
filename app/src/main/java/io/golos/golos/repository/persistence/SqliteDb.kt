package io.golos.golos.repository.persistence

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.golos.golos.utils.getLong
import io.golos.golos.utils.getString
import io.golos.golos.repository.persistence.model.UserAvatar

/**
 * Created by yuri on 06.11.17.
 */
private val dbVersion = 1

class SqliteDb(ctx: Context) : SQLiteOpenHelper(ctx, "mydb.db", null, dbVersion) {
    private val mAvatarsTable = AvatarsTable()

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(mAvatarsTable.createTableString)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

    fun saveAvatar(avatar: UserAvatar) {
        mAvatarsTable.saveAvatarToTable(writableDatabase, avatar)
    }

    fun getAvatar(userName: String): UserAvatar? {
        return mAvatarsTable.getAvatarFromDb(writableDatabase, userName)
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
}