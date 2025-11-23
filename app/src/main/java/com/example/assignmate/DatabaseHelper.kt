package com.example.assignmate

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.assignmate.model.Group

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 3
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

    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)
        // Clean up invalid data from previous bugs where user_id might have been -1
        db?.delete(TABLE_USER_GROUPS, "$KEY_USER_ID = ?", arrayOf("-1"))
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createUsersTable = ("CREATE TABLE " + TABLE_USERS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_USERNAME + " TEXT,"
                + KEY_EMAIL + " TEXT," + KEY_PASSWORD + " TEXT" + ")")
        db?.execSQL(createUsersTable)

        val createGroupsTable = ("CREATE TABLE " + TABLE_GROUPS + "("
                + KEY_GROUP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_GROUP_NAME + " TEXT,"
                + KEY_GROUP_DESCRIPTION + " TEXT," + KEY_GROUP_LEADER_ID + " INTEGER,"
                + KEY_GROUP_CODE + " TEXT" + ")")
        db?.execSQL(createGroupsTable)

        val createUserGroupsTable = ("CREATE TABLE " + TABLE_USER_GROUPS + "("
                + KEY_USER_ID + " INTEGER," + KEY_GROUP_ID + " INTEGER,"
                + "PRIMARY KEY(" + KEY_USER_ID + ", " + KEY_GROUP_ID + "))")
        db?.execSQL(createUserGroupsTable)
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
        var groupId = -1L

        db.beginTransaction()
        try {
            // Insert into groups table
            val groupValues = ContentValues().apply {
                put(KEY_GROUP_NAME, groupName)
                put(KEY_GROUP_DESCRIPTION, groupDescription)
                put(KEY_GROUP_LEADER_ID, leaderId)
                put(KEY_GROUP_CODE, groupCode)
            }
            groupId = db.insert(TABLE_GROUPS, null, groupValues)

            if (groupId == -1L) {
                return -1L
            }

            // Insert leader into user_groups table
            val userGroupValues = ContentValues().apply {
                put(KEY_USER_ID, leaderId)
                put(KEY_GROUP_ID, groupId)
            }
            val success = db.insertWithOnConflict(TABLE_USER_GROUPS, null, userGroupValues, SQLiteDatabase.CONFLICT_IGNORE)

            if (success == -1L) {
                groupId = -1L 
            } else {
                db.setTransactionSuccessful()
            }
            
        } finally {
            db.endTransaction()
        }
        return groupId
    }

    fun joinGroup(userId: Int, groupCode: String): Boolean {
        val db = this.readableDatabase
        val columns = arrayOf(KEY_GROUP_ID)
        val selection = "$KEY_GROUP_CODE = ?"
        val selectionArgs = arrayOf(groupCode)
        val cursor = db.query(TABLE_GROUPS, columns, selection, selectionArgs, null, null, null)

        if (cursor.moveToFirst()) {
            val groupId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_GROUP_ID))
            cursor.close()
            return addUserToGroup(userId, groupId)
        } else {
            cursor.close()
            return false
        }
    }

    private fun addUserToGroup(userId: Int, groupId: Long): Boolean {
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
        
        // This query correctly joins the groups and user_groups table.
        // It ensures that only groups the user is actually a member of are returned.
        val query = "SELECT g.*, u.$KEY_USERNAME as leader_name FROM $TABLE_GROUPS g " +
                    "INNER JOIN $TABLE_USER_GROUPS ug ON g.$KEY_GROUP_ID = ug.$KEY_GROUP_ID " +
                    "INNER JOIN $TABLE_USERS u ON g.$KEY_GROUP_LEADER_ID = u.$KEY_ID " +
                    "WHERE ug.$KEY_USER_ID = ?"
        
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val groupId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_GROUP_ID))
                val group = Group(
                    id = groupId,
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_NAME)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_DESCRIPTION)),
                    leader = cursor.getString(cursor.getColumnIndexOrThrow("leader_name")),
                    members = getGroupMembers(groupId),
                    lastUpdated = System.currentTimeMillis(), // Placeholder
                    progress = 50 // Placeholder
                )
                groups.add(group)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return groups
    }

    private fun getGroupMembers(groupId: Long): List<String> {
        val members = mutableListOf<String>()
        val db = this.readableDatabase
        val query = "SELECT u.$KEY_USERNAME FROM $TABLE_USERS u INNER JOIN $TABLE_USER_GROUPS ug ON u.$KEY_ID = ug.$KEY_USER_ID WHERE ug.$KEY_GROUP_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(groupId.toString()))

        if (cursor.moveToFirst()) {
            do {
                members.add(cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return members
    }
    
    fun getUserDetails(userId: Int): Pair<String, String>? {
        val db = this.readableDatabase
        val columns = arrayOf(KEY_USERNAME, KEY_EMAIL)
        val selection = "$KEY_ID = ?"
        val selectionArgs = arrayOf(userId.toString())
        val cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null)

        var userDetails: Pair<String, String>? = null
        if (cursor.moveToFirst()) {
            val username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME))
            val email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL))
            userDetails = Pair(username, email)
        }
        cursor.close()
        return userDetails
    }
}
