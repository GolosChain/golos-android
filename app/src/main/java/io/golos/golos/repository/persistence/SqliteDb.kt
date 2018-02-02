package io.golos.golos.repository.persistence

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import eu.bittrade.libs.steemj.base.models.VoteLight
import io.golos.golos.repository.model.*
import io.golos.golos.repository.persistence.model.UserAvatar
import io.golos.golos.screens.story.model.StoryWithComments
import io.golos.golos.screens.story.model.StoryWrapper
import io.golos.golos.utils.*

/**
 * Created by yuri on 06.11.17.
 */
private val dbVersion = 3

class SqliteDb(ctx: Context) : SQLiteOpenHelper(ctx, "mydb.db", null, dbVersion) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(TagsTable.createTableString)
        db?.execSQL(AvatarsTable.createTableString)
        db?.execSQL(UserFilterTable.createTableString)
        db?.execSQL(VotesTable.createTableString)
        db?.execSQL(StoriesRequestsTable.createTableString)
        db?.execSQL(DiscussionItemsTable.createTableString)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(TagsTable.createTableString)
        db?.execSQL(AvatarsTable.createTableString)
        db?.execSQL(UserFilterTable.createTableString)
        db?.execSQL(VotesTable.createTableString)
        db?.execSQL(StoriesRequestsTable.createTableString)
        db?.execSQL(DiscussionItemsTable.createTableString)
    }

    fun saveAvatar(avatar: UserAvatar) {
        AvatarsTable.saveAvatarToTable(writableDatabase, avatar)
    }

    fun saveAvatars(avatars: List<UserAvatar>) {
        AvatarsTable.save(avatars, writableDatabase)
    }

    fun getAvatar(userName: String): UserAvatar? {
        return AvatarsTable.get(userName, writableDatabase)
    }

    fun getAvatars(userName: List<String>): Map<String, UserAvatar?> {
        return AvatarsTable.getAvatarsFromDb(writableDatabase, userName)
    }

    fun saveTags(list: List<Tag>) {
        TagsTable.save(list, writableDatabase)
    }


    fun getTags(): List<Tag> {
        return TagsTable.get(writableDatabase)
    }

    fun saveTagsForFilter(tags: List<Tag>, filter: String) {
        UserFilterTable.save(tags, filter, writableDatabase)
    }

    fun getTagsForFilter(filter: String): List<Tag> {
        return UserFilterTable.get(filter, TagsTable, writableDatabase)
    }


    fun saveStories(stories: Map<StoryRequest, StoriesFeed>) {
        val discussionItem = stories
                .map { it.value.items }
                .flatMap { it }
                .filter { it.rootStory() != null }
                .map { it.rootStory()!! }
        DiscussionItemsTable.save(discussionItem, VotesTable, AvatarsTable, writableDatabase)
        val storiesIds = stories
                .map {
                    FilteredStoriesIds(it.key, it.value.items
                            .map { it.rootStory() }
                            .filter { it != null }
                            .map { it!! }
                            .map { it.id })
                }
        StoriesRequestsTable.save(storiesIds, writableDatabase)
    }

    fun getStories(): Map<StoryRequest, StoriesFeed> {
        val storiesIds = StoriesRequestsTable.get(writableDatabase)
        return storiesIds
                .groupBy { it }
                .mapValues {
                    StoriesFeed(DiscussionItemsTable.get(it.key.storyIds, VotesTable, AvatarsTable, writableDatabase)
                            .map { StoryWithComments(StoryWrapper(it, UpdatingState.DONE), arrayListOf()) }.toArrayList(),
                            it.key.request.feedType,
                            it.key.request.filter,
                            null)
                }
                .mapKeys { it.key.request }
    }

    fun deleteAllStories() {
        VotesTable.deleteAll(writableDatabase)
        StoriesRequestsTable.deleteAll(writableDatabase)
        DiscussionItemsTable.deleteAll(writableDatabase)
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

        fun save(avatars: List<UserAvatar>, db: SQLiteDatabase) {
            if (avatars.isEmpty()) return
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

        fun get(forUser: String, db: SQLiteDatabase): UserAvatar? {
            val cursor = db.query(databaseName, arrayOf(avatarPathColumn, userNameColumn, dateColumn),
                    "$userNameColumn = \'$forUser\'", null, null, null, null)
            if (cursor.count == 0) return null
            cursor.moveToFirst()
            val avatar = UserAvatar(cursor.getString(userNameColumn) ?: return null,
                    cursor.getString(avatarPathColumn),
                    cursor.getLong(dateColumn))
            cursor.close()
            return avatar
        }

        fun getAvatarsFromDb(db: SQLiteDatabase, usernames: List<String>): Map<String, UserAvatar?> {
            if (usernames.isEmpty()) return hashMapOf()

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


    private object DiscussionItemsTable {
        private val databaseName = "discussion_item_table"
        private val url = "url"
        private val title = "title"
        private val id = "id"
        private val categoryName = "categoryName"
        private val tags = "tags"
        private val images = "images"
        private val links = "links"
        private val votesNum = "votesNum"
        private val votesRshares = "votesRshares"
        private val commentsCount = "commentsCount"
        private val permlink = "permlink"
        private val gbgAmount = "gbgAmount"
        private val body = "body"
        private val author = "author"
        private val format = "format"
        private val parentPermlink = "parentPermlink"
        private val level = "level"
        private val gbgCostInDollars = "gbgCostInDollars"
        private val reputation = "reputation"
        private val lastUpdated = "lastUpdated"
        private val created = "created"
        private val isUserUpvotedOnThis = "isUserUpvotedOnThis"
        private val type = "type"
        private val firstRebloggedBy = "firstRebloggedBy"
        private val cleanedFromImages = "cleanedFromImages"
        private val childrenCount = "childrenCount"


        val createTableString = "create table if not exists $databaseName ( $id integer primary key ," +
                "$url text, $title text, $categoryName text, $tags text, $images text, $links text," +
                "$votesNum integer, $votesRshares integer, $commentsCount integer, " +
                "$permlink text, $gbgAmount real, $body text, $author text, $format text, $parentPermlink text," +
                "$level integer, $gbgCostInDollars real, $reputation integer, $lastUpdated integer, " +
                "$created integer, $isUserUpvotedOnThis integer, $type text, $firstRebloggedBy text," +
                "$cleanedFromImages text, $childrenCount integer)"

        fun save(items: List<GolosDiscussionItem>,
                 voteTable: VotesTable,
                 avatarTable: AvatarsTable,
                 db: SQLiteDatabase) {

            if (items.isEmpty()) return

            val values = ContentValues()
            db.beginTransaction()
            items.forEach {
                values.put(this.id, it.id)
                values.put(this.url, it.url)
                values.put(this.title, it.title)
                values.put(this.categoryName, it.categoryName)
                values.put(this.tags, mapper.writeValueAsString(it.tags))
                values.put(this.images, mapper.writeValueAsString(it.images))
                values.put(this.links, mapper.writeValueAsString(it.links))
                values.put(this.permlink, it.permlink)
                values.put(this.gbgAmount, it.gbgAmount)
                values.put(this.body, it.body)
                values.put(this.author, it.author)
                values.put(this.format, it.format.name)
                values.put(this.parentPermlink, it.parentPermlink)
                values.put(this.level, it.level)
                values.put(this.gbgCostInDollars, it.gbgCostInDollars)
                values.put(this.reputation, it.reputation)
                values.put(this.lastUpdated, it.lastUpdated)
                values.put(this.created, it.created)
                values.put(this.isUserUpvotedOnThis, it.isUserUpvotedOnThis)
                values.put(this.type, it.type.name)
                values.put(this.firstRebloggedBy, it.firstRebloggedBy)
                values.put(this.cleanedFromImages, it.cleanedFromImages)
                values.put(this.childrenCount, it.childrenCount)
                values.put(this.votesNum, it.votesNum)
                values.put(this.votesRshares, it.votesRshares)
                values.put(this.commentsCount, it.commentsCount)

                voteTable.save(it.activeVotes, it.id, db)

                db.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            val avatars = items.filter { it.avatarPath != null }.map { UserAvatar(it.author, it.avatarPath, System.currentTimeMillis()) }

            avatarTable.save(avatars, db)

            db.setTransactionSuccessful()
            db.endTransaction()
        }

        fun get(ids: List<Long>,
                voteTable: VotesTable,
                avatarTable: AvatarsTable,
                db: SQLiteDatabase): List<GolosDiscussionItem> {
            if (ids.isEmpty()) return listOf()

            val workingIds = if (ids.size > 100) ids.subList(0, 100) else ids

            val idBuilder = StringBuilder("( ")
            workingIds.forEachIndexed { index, item ->
                if (index != workingIds.lastIndex)
                    idBuilder.append(" $id = $item or ")
                else idBuilder.append("$id = $item ) ")
            }
            val cursor = db.rawQuery("select * from $databaseName where $idBuilder", null)
            val out = ArrayList<GolosDiscussionItem>(cursor.count)

            if (cursor.count != 0) {
                cursor.moveToFirst()
                val temp = ArrayList<GolosDiscussionItem>(cursor.count)
                val stringListType = mapper.typeFactory.constructCollectionType(List::class.java, String::class.java)
                while (!cursor.isAfterLast) {
                    val tags = mapper.readValue<List<String>>(cursor.getString(tags) ?: "", stringListType).toArrayList()
                    val images = mapper.readValue<List<String>>(cursor.getString(images) ?: "", stringListType).toArrayList()
                    val links = mapper.readValue<List<String>>(cursor.getString(links) ?: "", stringListType).toArrayList()
                    val format = cursor.getString(format)
                    val itemType = cursor.getString(type)

                    val discussionItem = GolosDiscussionItem(cursor.getString(url) ?: "",
                            cursor.getLong(id),
                            cursor.getString(title) ?: "",
                            cursor.getString(categoryName) ?: "",
                            tags,
                            images,
                            links,
                            cursor.getInt(votesNum),
                            cursor.getLong(votesRshares),
                            cursor.getInt(commentsCount),
                            cursor.getString(permlink) ?: "",
                            cursor.getDouble(gbgAmount),
                            cursor.getString(body) ?: "",
                            cursor.getString(author) ?: "",
                            if (format != null) Format.valueOf(format) else Format.HTML,
                            null,
                            arrayListOf(),
                            cursor.getString(parentPermlink) ?: "",
                            cursor.getInt(childrenCount),
                            cursor.getInt(level),
                            cursor.getDouble(gbgCostInDollars),
                            cursor.getLong(reputation),
                            cursor.getLong(lastUpdated),
                            cursor.getLong(created),
                            cursor.getBool(isUserUpvotedOnThis),
                            arrayListOf(),
                            if (itemType != null) ItemType.valueOf(itemType) else ItemType.PLAIN,
                            cursor.getString(firstRebloggedBy) ?: "",
                            cursor.getString(cleanedFromImages) ?: "",
                            arrayListOf())

                    //   discussionItem.parts = rowsParser.parse(discussionItem)
                    discussionItem.avatarPath = avatarTable.get(discussionItem.author, db)?.avatarPath
                    discussionItem.activeVotes.addAll(voteTable.get(discussionItem.id, db))
                    temp.add(discussionItem)

                    cursor.moveToNext()
                }
                workingIds.forEach { id ->
                    val item = temp.find { it.id == id }
                    item?.let { out.add(item) }
                }
            }
            cursor.close()
            return out
        }

        fun deleteAll(db: SQLiteDatabase) {
            db.delete(databaseName, null, null)
        }
    }

    private object StoriesRequestsTable {
        private val databaseName = "stories_filter_table"
        private val serializedRequest = "json"

        val createTableString = "create table if not exists $databaseName ( $serializedRequest text primary key)"

        fun save(filters: List<FilteredStoriesIds>, db: SQLiteDatabase) {
            if (filters.isEmpty()) return
            val values = ContentValues()
            db.beginTransaction()
            filters.forEach {
                values.put(serializedRequest, mapper.writeValueAsString(it))

                db.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            db.endTransaction()
        }

        fun get(db: SQLiteDatabase): List<FilteredStoriesIds> {
            val cursor = db.rawQuery("select * from $databaseName", null)
            val out = ArrayList<FilteredStoriesIds>(cursor.count)
            if (cursor.count != 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    out.add(mapper.readValue(cursor.getString(serializedRequest) ?: "",
                            FilteredStoriesIds::class.java))
                    cursor.moveToNext()
                }
            }
            cursor.close()
            return out

        }

        fun deleteAll(db: SQLiteDatabase) {
            db.delete(databaseName, null, null)
        }
    }


    private object VotesTable {
        private val databaseName = "votes_table"
        private val userName = "name"
        private val rshares = "rshares"
        private val percent = "percent"
        private val storyId = "storyId"
        val createTableString = "create table if not exists $databaseName ( $userName text ," +
                "$rshares integer, $percent integer, $storyId integer )"

        fun save(votes: List<VoteLight>, storyId: Long, db: SQLiteDatabase) {
            if (votes.isEmpty()) return
            val values = ContentValues()
            db.beginTransaction()
            votes.forEach {
                values.put(userName, it.name)
                values.put(rshares, it.rshares)
                values.put(percent, it.percent)
                values.put(this.storyId, storyId)
                db.insertWithOnConflict(databaseName, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            db.endTransaction()
        }


        fun get(storyId: Long, db: SQLiteDatabase): List<VoteLight> {

            val statement = "select * from $databaseName where ${this.storyId} = $storyId order by $rshares desc"
            val cursor = db.rawQuery(statement, null)
            val out = ArrayList<VoteLight>(cursor.count)

            if (cursor.count != 0) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    out.add(VoteLight(
                            cursor.getString(userName) ?: "",
                            cursor.getLong(rshares),
                            cursor.getInt(percent)
                    ))
                    cursor.moveToNext()
                }
            }
            cursor.close()
            return out
        }

        fun deleteAll(db: SQLiteDatabase) {
            db.delete(databaseName, null, null)
        }
    }

    private object UserFilterTable {
        private val databaseName = "filter_table"
        private val tagName = "name"
        private val filterName = "filter"
        val createTableString = "create table if not exists $databaseName ( $tagName text ," +
                "$filterName text )"

        fun save(tags: List<Tag>, filterName: String, db: SQLiteDatabase) {

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

        fun get(filterName: String,
                tagsTable: TagsTable,
                db: SQLiteDatabase): List<Tag> {
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

        fun getAll(db: SQLiteDatabase): List<String> {
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

        fun save(tags: List<Tag>, db: SQLiteDatabase) {
            if (tags.isEmpty()) return

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

        fun get(db: SQLiteDatabase): List<Tag> {
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