package com.example.assignmate

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.assignmate.model.Group
import java.util.UUID

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "AssignMate.db"

        // User table
        private const val TABLE_USERS = "users"
        private const val KEY_ID = "id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"

        // Group table
        private const val TABLE_GROUPS = "groups"
        private const val KEY_GROUP_ID = "group_id"
        private const val KEY_GROUP_NAME = "group_name"
        private const val KEY_GROUP_DESCRIPTION = "group_description"
        private const val KEY_GROUP_LEADER_ID = "group_leader_id"
        private const val KEY_GROUP_CODE = "group_code"

        // User-Group mapping table
        private const val TABLE_USER_GROUPS = "user_groups"
        private const val KEY_USER_ID = "user_id"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createUsersTable = ("CREATE TABLE " + TABLE_USERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_USERNAME + " TEXT,"
                + KEY_EMAIL + " TEXT," + KEY_PASSWORD + " TEXT" + ")")
        db?.execSQL(createUsersTable)

        val createGroupsTable = ("CREATE TABLE " + TABLE_GROUPS + "("
                + KEY_GROUP_ID + " TEXT PRIMARY KEY," + KEY_GROUP_NAME + " TEXT,"
                + KEY_GROUP_DESCRIPTION + " TEXT," + KEY_GROUP_LEADER_ID + " INTEGER,"
                + KEY_GROUP_CODE + " TEXT" + ")")
        db?.execSQL(createGroupsTable)

        val createUserGroupsTable = ("CREATE TABLE " + TABLE_USER_GROUPS + "("
                + KEY_USER_ID + " INTEGER," + KEY_GROUP_ID + " TEXT,"
                + "PRIMARY KEY(" + KEY_USER_ID + ", " + KEY_GROUP_ID + "))")
        db?.execSQL(createUserGroupsTable)

        // Pre-populate with mock data
        addUser(db, "testuser", "test@example.com", "password123")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_GROUPS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USER_GROUPS")
        onCreate(db)
    }

    fun addUser(username: String, email: String, password: String): Boolean {
        val db = this.writableDatabase
        if (checkUser(email)) {
            return false // User already exists
        }
        val values = ContentValues()
        values.put(KEY_USERNAME, username)
        values.put(KEY_EMAIL, email)
        values.put(KEY_PASSWORD, password) // In a real app, hash the password
        val success = db.insert(TABLE_USERS, null, values)
        return success != -1L
    }

    private fun addUser(db: SQLiteDatabase?, username: String, email: String, password: String) {
        val values = ContentValues()
        values.put(KEY_USERNAME, username)
        values.put(KEY_EMAIL, email)
        values.put(KEY_PASSWORD, password) // In a real app, hash the password
        db?.insert(TABLE_USERS, null, values)
    }

    fun checkUser(email: String, password: String): Boolean {
        val db = this.readableDatabase
        val columns = arrayOf(KEY_ID)
        val selection = "$KEY_EMAIL = ? AND $KEY_PASSWORD = ?"
        val selectionArgs = arrayOf(email, password)
        val cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null)
        val count = cursor.count
        cursor.close()
        return count > 0
    }
    
    fun getUserId(email: String): Int {
        val db = this.readableDatabase
        val columns = arrayOf(KEY_ID)
        val selection = "$KEY_EMAIL = ?"
        val selectionArgs = arrayOf(email)
        val cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null)
        var userId = -1
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
        }
        cursor.close()
        return userId
    }

    fun checkUser(email: String): Boolean {
        val db = this.readableDatabase
        val columns = arrayOf(KEY_ID)
        val selection = "$KEY_EMAIL = ?"
        val selectionArgs = arrayOf(email)
        val cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null)
        val count = cursor.count
        cursor.close()
        return count > 0
    }

    fun createGroup(groupName: String, groupDescription: String, leaderId: Int, groupCode: String): Long {
        val db = this.writableDatabase
        val groupId = UUID.randomUUID().toString()

        val values = ContentValues()
        values.put(KEY_GROUP_ID, groupId)
        values.put(KEY_GROUP_NAME, groupName)
        values.put(KEY_GROUP_DESCRIPTION, groupDescription)
        values.put(KEY_GROUP_LEADER_ID, leaderId)
        values.put(KEY_GROUP_CODE, groupCode)
        val success = db.insert(TABLE_GROUPS, null, values)

        if (success != -1L) {
            addUserToGroup(leaderId, groupId)
        }
        return success
    }

    fun joinGroup(userId: Int, groupCode: String): Boolean {
        val db = this.readableDatabase
        val columns = arrayOf(KEY_GROUP_ID)
        val selection = "$KEY_GROUP_CODE = ?"
        val selectionArgs = arrayOf(groupCode)
        val cursor = db.query(TABLE_GROUPS, columns, selection, selectionArgs, null, null, null)

        if (cursor.moveToFirst()) {
            val groupId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_ID))
            cursor.close()
            return addUserToGroup(userId, groupId)
        } else {
            cursor.close()
            return false
        }
    }

    private fun addUserToGroup(userId: Int, groupId: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_USER_ID, userId)
        values.put(KEY_GROUP_ID, groupId)
        val success = db.insertWithOnConflict(TABLE_USER_GROUPS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        return success != -1L
    }

    fun getGroupsForUser(userId: Int): List<Group> {
        val groups = mutableListOf<Group>()
        val db = this.readableDatabase
        val query = "SELECT g.*, u.$KEY_USERNAME as leader_name FROM $TABLE_GROUPS g " +
                    "INNER JOIN $TABLE_USER_GROUPS ug ON g.$KEY_GROUP_ID = ug.$KEY_GROUP_ID " +
                    "INNER JOIN $TABLE_USERS u ON g.$KEY_GROUP_LEADER_ID = u.$KEY_ID " +
                    "WHERE ug.$KEY_USER_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val groupId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_ID))
                val group = Group(
                    id = groupId,
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_NAME)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_DESCRIPTION)),
                    leader = cursor.getString(cursor.getColumnIndexOrThrow("leader_name")),
                    members = getGroupMembers(groupId), // This is still N+1, but let's fix the crash first.
                    lastUpdated = System.currentTimeMillis(), // Placeholder
                    progress = 50 // Placeholder
                )
                groups.add(group)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return groups
    }

    private fun getGroupMembers(groupId: String): List<String> {
        val members = mutableListOf<String>()
        val db = this.readableDatabase
        val query = "SELECT u.$KEY_USERNAME FROM $TABLE_USERS u INNER JOIN $TABLE_USER_GROUPS ug ON u.$KEY_ID = ug.$KEY_USER_ID WHERE ug.$KEY_GROUP_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(groupId))

        if (cursor.moveToFirst()) {
            do {
                members.add(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return members
    }

    private fun getUsername(userId: Int): String {
        val db = this.readableDatabase
        val columns = arrayOf(KEY_USERNAME)
        val selection = "$KEY_ID = ?"
        val selectionArgs = arrayOf(userId.toString())
        val cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null)

        var username = ""
        if (cursor.moveToFirst()) {
            username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME))
        }
        cursor.close()
        return username
    }
}
