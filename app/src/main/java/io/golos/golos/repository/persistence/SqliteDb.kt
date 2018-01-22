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
private val dbVersion = 2

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
        db?.execSQL(mTagsTable.createTableString)
        db?.execSQL(mUserFilterTable.createTableString)
    }

    fun saveAvatar(avatar: UserAvatar) {
        mAvatarsTable.saveAvatarToTable(writableDatabase, avatar)
    }

    fun saveAvatars(avatars: List<UserAvatar>) {
        mAvatarsTable.saveAvatarsToTable(writableDatabase, avatars)
    }

    fun getAvatar(userName: String): UserAvatar? {
        return mAvatarsTable.getAvatarFromDb(writableDatabase, userName)
    }

    fun getAvatars(userName: List<String>): Map<String, UserAvatar?> {
        return mAvatarsTable.getAvatarsFromDb(writableDatabase, userName)
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

        fun saveAvatarsToTable(db: SQLiteDatabase, avatars: List<UserAvatar>) {
            db.beginTransaction()
            val values = ContentValues()
            avatars.forEach {
                values.put(avatarPathColumn, it.avatarPath)
                values.put(userNameColumn, it.userName)
                values.put(dateColumn, it.dateUpdated)
                db.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            db.endTransaction()
        }

        fun getAvatarFromDb(db: SQLiteDatabase, username: String): UserAvatar? {
            val cursor = db.query(databaseName, arrayOf(avatarPathColumn, userNameColumn, dateColumn),
                    "$userNameColumn = \'$username\'", null, null, null, null)
            if (cursor.count == 0) return null
            cursor.moveToFirst()
            val avatar = UserAvatar(cursor.getString(userNameColumn) ?: return null,
                    cursor.getString(avatarPathColumn),
                    cursor.getLong(dateColumn))
            cursor.close()
            return avatar
        }

        fun getAvatarsFromDb(db: SQLiteDatabase, usernames: List<String>): Map<String, UserAvatar?> {

            val users = usernames.distinct()
            val map = HashMap<String, UserAvatar?>(users.size)

            val numberOfLists = users.size / 500 + 1

            val listOfUsers = ArrayList<List<String>>(numberOfLists)


            (0 until numberOfLists).forEach {
                var upperBound = 500 * (it + 1)
                upperBound = if (upperBound > users.size) users.size else upperBound
                listOfUsers.add(users.subList(500 * it, upperBound))
            }

            listOfUsers.forEach {
                val selection = StringBuilder()
                selection.append("( ")
                var disabled = false
                it.forEachIndexed { index, username ->
                    if (disabled) {
                        selection.append(" or ")
                    } else {
                        disabled = true
                    }
                    selection.append("$userNameColumn = \'$username\'")
                }
                selection.append(" )")
                val cursor = db.query(databaseName, arrayOf(avatarPathColumn, userNameColumn, dateColumn),
                        selection.toString(), null, null, null, null)
                if (cursor.count == 0) return map
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val name = cursor.getString(userNameColumn)
                    if (name != null) {
                        val avatar = UserAvatar(name,
                                cursor.getString(avatarPathColumn),
                                cursor.getLong(dateColumn))
                        map.put(name, avatar)
                    }

                    cursor.moveToNext()
                }
                cursor.close()
            }

            return map
        }
    }

    private object UserFilterTable {
        private val databaseName = "filter_table"
        private val tagName = "name"
        private val filterName = "filter"
        val createTableString = "create table if not exists $databaseName ( $tagName text ," +
                "$filterName text )"

        fun saveTagsToTable(db: SQLiteDatabase, tags: List<Tag>, filterName: String) {

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
                val tagName = cursor.getString(TagsTable.tagName)
                if (tagName != null) {
                    val tag = Tag(tagName,
                            cursor.getDouble(TagsTable.payoutInGbg),
                            cursor.getLong(TagsTable.votesCount),
                            cursor.getLong(TagsTable.topPostsCount))
                    out.add(tag)
                }

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
                val tagName = cursor.getString(filterName)
                if (tagName != null) {
                    out.add(tagName)
                }
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
                val tagName = cursor.getString(tagName)
                if (tagName != null) {
                    val tag = Tag(tagName,
                            cursor.getDouble(payoutInGbg),
                            cursor.getLong(votesCount),
                            cursor.getLong(topPostsCount))
                    out.add(tag)
                }

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
                val tagName = cursor.getString(tagName)
                if (tagName != null) {
                    val tag = Tag(tagName,
                            cursor.getDouble(payoutInGbg),
                            cursor.getLong(votesCount),
                            cursor.getLong(topPostsCount))
                    out.add(tag)
                }
                cursor.moveToNext()
            }
            cursor.close()
            return out
        }
    }
}