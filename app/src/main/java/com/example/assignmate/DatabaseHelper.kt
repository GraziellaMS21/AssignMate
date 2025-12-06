package com.example.assignmate

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.assignmate.model.Comment
import com.example.assignmate.model.Group
import com.example.assignmate.model.Label
import com.example.assignmate.model.Member
import com.example.assignmate.model.Subtask
import com.example.assignmate.model.Task
import com.example.assignmate.model.Notification
import java.util.Calendar

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 11
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
        private const val KEY_IS_FAVOURITE = "is_favourite"

        // User-Group mapping table
        private const val TABLE_USER_GROUPS = "user_groups"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ROLE = "role"

        // Task table
        private const val TABLE_TASKS = "tasks"
        private const val KEY_TASK_ID = "task_id"
        private const val KEY_TASK_NAME = "task_name"
        private const val KEY_TASK_DESCRIPTION = "task_description"
        private const val KEY_TASK_GROUP_ID = "task_group_id"
        private const val KEY_DUE_DATE = "due_date"
        private const val KEY_STATUS = "status"

        // Subtasks table
        private const val TABLE_SUBTASKS = "subtasks"
        private const val KEY_SUBTASK_ID = "subtask_id"
        private const val KEY_SUBTASK_TASK_ID = "subtask_task_id"
        private const val KEY_SUBTASK_NAME = "subtask_name"
        private const val KEY_SUBTASK_IS_COMPLETED = "subtask_is_completed"

        // Labels table
        private const val TABLE_LABELS = "labels"
        private const val KEY_LABEL_ID = "label_id"
        private const val KEY_LABEL_NAME = "label_name"
        private const val KEY_LABEL_COLOR = "label_color"

        // Task-Label mapping table
        private const val TABLE_TASK_LABELS = "task_labels"
        private const val KEY_TASK_LABEL_TASK_ID = "task_label_task_id"
        private const val KEY_TASK_LABEL_LABEL_ID = "task_label_label_id"

        // Task-Assignment table
        private const val TABLE_TASK_ASSIGNMENTS = "task_assignments"
        private const val KEY_ASSIGNMENT_TASK_ID = "assignment_task_id"
        private const val KEY_ASSIGNMENT_USER_ID = "assignment_user_id"

        // Comments table
        private const val TABLE_COMMENTS = "comments"
        private const val KEY_COMMENT_ID = "comment_id"
        private const val KEY_COMMENT_TASK_ID = "comment_task_id"
        private const val KEY_COMMENT_USER_ID = "comment_user_id"
        private const val KEY_COMMENT_TEXT = "comment_text"
        private const val KEY_COMMENT_TIMESTAMP = "comment_timestamp"

        // Notifications table
        private const val TABLE_NOTIFICATIONS = "notifications"
        private const val KEY_NOTIFICATION_ID = "notification_id"
        private const val KEY_NOTIFICATION_USER_ID = "notification_user_id"
        private const val KEY_NOTIFICATION_TITLE = "notification_title"
        private const val KEY_NOTIFICATION_MESSAGE = "notification_message"
        private const val KEY_NOTIFICATION_TIMESTAMP = "notification_timestamp"
        private const val KEY_NOTIFICATION_IS_READ = "notification_is_read"

        // Favourite Group table
        private const val TABLE_FAVOURITE_GROUP = "favourite_group"
        private const val KEY_FAVOURITE_GROUP_ID = "favourite_group_id"
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
                + KEY_GROUP_CODE + " TEXT," + KEY_IS_FAVOURITE + " INTEGER DEFAULT 0" + ")")
        db?.execSQL(createGroupsTable)

        val createUserGroupsTable = ("CREATE TABLE " + TABLE_USER_GROUPS + "("
                + KEY_USER_ID + " INTEGER," + KEY_GROUP_ID + " INTEGER,"
                + KEY_ROLE + " TEXT DEFAULT 'member',"
                + "PRIMARY KEY(" + KEY_USER_ID + ", " + KEY_GROUP_ID + "))")
        db?.execSQL(createUserGroupsTable)

        val createTasksTable = ("CREATE TABLE " + TABLE_TASKS + "("
                + KEY_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_TASK_NAME + " TEXT,"
                + KEY_TASK_DESCRIPTION + " TEXT,"
                + KEY_TASK_GROUP_ID + " INTEGER,"
                + KEY_DUE_DATE + " INTEGER,"
                + KEY_STATUS + " TEXT" + ")")
        db?.execSQL(createTasksTable)

        val createSubtasksTable = ("CREATE TABLE " + TABLE_SUBTASKS + "("
                + KEY_SUBTASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_SUBTASK_TASK_ID + " INTEGER,"
                + KEY_SUBTASK_NAME + " TEXT,"
                + KEY_SUBTASK_IS_COMPLETED + " INTEGER DEFAULT 0" + ")")
        db?.execSQL(createSubtasksTable)

        val createLabelsTable = ("CREATE TABLE " + TABLE_LABELS + "("
                + KEY_LABEL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_LABEL_NAME + " TEXT,"
                + KEY_LABEL_COLOR + " TEXT" + ")")
        db?.execSQL(createLabelsTable)

        val createTaskLabelsTable = ("CREATE TABLE " + TABLE_TASK_LABELS + "("
                + KEY_TASK_LABEL_TASK_ID + " INTEGER,"
                + KEY_TASK_LABEL_LABEL_ID + " INTEGER,"
                + "PRIMARY KEY(" + KEY_TASK_LABEL_TASK_ID + ", " + KEY_TASK_LABEL_LABEL_ID + "))")
        db?.execSQL(createTaskLabelsTable)

        val createTaskAssignmentsTable = ("CREATE TABLE " + TABLE_TASK_ASSIGNMENTS + "("
                + KEY_ASSIGNMENT_TASK_ID + " INTEGER,"
                + KEY_ASSIGNMENT_USER_ID + " INTEGER,"
                + "PRIMARY KEY(" + KEY_ASSIGNMENT_TASK_ID + ", " + KEY_ASSIGNMENT_USER_ID + "))")
        db?.execSQL(createTaskAssignmentsTable)

        val createCommentsTable = ("CREATE TABLE " + TABLE_COMMENTS + "("
                + KEY_COMMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_COMMENT_TASK_ID + " INTEGER,"
                + KEY_COMMENT_USER_ID + " INTEGER,"
                + KEY_COMMENT_TEXT + " TEXT,"
                + KEY_COMMENT_TIMESTAMP + " INTEGER" + ")")
        db?.execSQL(createCommentsTable)

        val createNotificationsTable = ("CREATE TABLE " + TABLE_NOTIFICATIONS + "("
                + KEY_NOTIFICATION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_NOTIFICATION_USER_ID + " INTEGER,"
                + KEY_NOTIFICATION_TITLE + " TEXT,"
                + KEY_NOTIFICATION_MESSAGE + " TEXT,"
                + KEY_NOTIFICATION_TIMESTAMP + " INTEGER,"
                + KEY_NOTIFICATION_IS_READ + " INTEGER DEFAULT 0" + ")")
        db?.execSQL(createNotificationsTable)

        val createFavouriteGroupTable = ("CREATE TABLE " + TABLE_FAVOURITE_GROUP + "("
                + KEY_USER_ID + " INTEGER PRIMARY KEY," + KEY_FAVOURITE_GROUP_ID + " INTEGER" + ")")
        db?.execSQL(createFavouriteGroupTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_GROUPS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USER_GROUPS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_SUBTASKS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_LABELS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_TASK_LABELS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_TASK_ASSIGNMENTS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_COMMENTS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NOTIFICATIONS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_FAVOURITE_GROUP")
        onCreate(db)
    }

    fun getUnreadNotificationCount(userId: Int): Int {
        val db = this.readableDatabase
        val selection = "$KEY_NOTIFICATION_USER_ID = ? AND $KEY_NOTIFICATION_IS_READ = 0"
        val selectionArgs = arrayOf(userId.toString())
        val cursor = db.query(TABLE_NOTIFICATIONS, null, selection, selectionArgs, null, null, null)
        val count = cursor.count
        cursor.close()
        return count
    }

    fun getAssignedTasksCountForGroup(groupId: Long): Int {
        val db = this.readableDatabase
        val query = "SELECT COUNT(DISTINCT $KEY_ASSIGNMENT_TASK_ID) FROM $TABLE_TASK_ASSIGNMENTS ta " +
                "INNER JOIN $TABLE_TASKS t ON ta.$KEY_ASSIGNMENT_TASK_ID = t.$KEY_TASK_ID " +
                "WHERE t.$KEY_TASK_GROUP_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(groupId.toString()))
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun setFavouriteGroup(userId: Int, groupId: Long) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_USER_ID, userId)
        values.put(KEY_FAVOURITE_GROUP_ID, groupId)
        db.insertWithOnConflict(TABLE_FAVOURITE_GROUP, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getFavouriteGroup(userId: Int): Long {
        val db = this.readableDatabase
        val columns = arrayOf(KEY_FAVOURITE_GROUP_ID)
        val selection = "$KEY_USER_ID = ?"
        val selectionArgs = arrayOf(userId.toString())
        val cursor = db.query(TABLE_FAVOURITE_GROUP, columns, selection, selectionArgs, null, null, null)
        var groupId = -1L
        if (cursor.moveToFirst()) {
            groupId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FAVOURITE_GROUP_ID))
        }
        cursor.close()
        return groupId
    }

    fun addNotification(userId: Int, title: String, message: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_NOTIFICATION_USER_ID, userId)
        values.put(KEY_NOTIFICATION_TITLE, title)
        values.put(KEY_NOTIFICATION_MESSAGE, message)
        values.put(KEY_NOTIFICATION_TIMESTAMP, System.currentTimeMillis())
        return db.insert(TABLE_NOTIFICATIONS, null, values)
    }

    fun getNotificationsForUser(userId: Int): List<Notification> {
        val notifications = mutableListOf<Notification>()
        val db = this.readableDatabase
        val selection = "$KEY_NOTIFICATION_USER_ID = ?"
        val selectionArgs = arrayOf(userId.toString())
        val cursor = db.query(TABLE_NOTIFICATIONS, null, selection, selectionArgs, null, null, "$KEY_NOTIFICATION_TIMESTAMP DESC")

        if (cursor.moveToFirst()) {
            do {
                val notification = Notification(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_NOTIFICATION_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NOTIFICATION_USER_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTIFICATION_TITLE)),
                    message = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NOTIFICATION_MESSAGE)),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_NOTIFICATION_TIMESTAMP)),
                    isRead = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_NOTIFICATION_IS_READ)) == 1
                )
                notifications.add(notification)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return notifications
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
                put(KEY_ROLE, "leader")
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
            return addMemberToGroup(userId, groupId)
        } else {
            cursor.close()
            return false
        }
    }

    fun addMemberToGroup(userId: Int, groupId: Long): Boolean {
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

        val favouriteGroupId = getFavouriteGroup(userId)

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
                    code = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_CODE)),
                    leader = cursor.getString(cursor.getColumnIndexOrThrow("leader_name")),
                    members = getGroupMembersAsListOfString(groupId),
                    lastUpdated = System.currentTimeMillis(), // Placeholder
                    progress = getGroupProgress(groupId), // Calculate progress
                    pendingTaskCount = getPendingTaskCountForGroup(groupId),
                    assignedTasksCount = getAssignedTasksCountForGroup(groupId),
                    isFavourite = groupId == favouriteGroupId
                )
                groups.add(group)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return groups
    }

    fun getGroup(groupId: Long): Group? {
        val db = this.readableDatabase
        val query = "SELECT g.*, u.$KEY_USERNAME as leader_name FROM $TABLE_GROUPS g " +
                "INNER JOIN $TABLE_USERS u ON g.$KEY_GROUP_LEADER_ID = u.$KEY_ID " +
                "WHERE g.$KEY_GROUP_ID = ?"

        val cursor = db.rawQuery(query, arrayOf(groupId.toString()))

        var group: Group? = null
        if (cursor.moveToFirst()) {
            val favouriteGroupId = getFavouriteGroup(getGroupLeaderId(groupId))
            group = Group(
                id = groupId,
                name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_NAME)),
                description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_DESCRIPTION)),
                code = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_CODE)),
                leader = cursor.getString(cursor.getColumnIndexOrThrow("leader_name")),
                members = getGroupMembersAsListOfString(groupId),
                lastUpdated = System.currentTimeMillis(), // Placeholder
                progress = getGroupProgress(groupId), // Calculate progress
                pendingTaskCount = getPendingTaskCountForGroup(groupId),
                assignedTasksCount = getAssignedTasksCountForGroup(groupId),
                isFavourite = groupId == favouriteGroupId
            )
        }
        cursor.close()
        return group
    }

    fun getGroupMembers(groupId: Long): List<Member> {
        val members = mutableListOf<Member>()
        val db = this.readableDatabase
        val query = "SELECT u.$KEY_ID, u.$KEY_USERNAME, ug.$KEY_ROLE FROM $TABLE_USERS u INNER JOIN $TABLE_USER_GROUPS ug ON u.$KEY_ID = ug.$KEY_USER_ID WHERE ug.$KEY_GROUP_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(groupId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
                val username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME))
                val role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLE))
                members.add(Member(userId, username, role))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return members
    }

    fun getGroupMembersAsListOfString(groupId: Long): List<String> {
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

    fun getGroupLeaderId(groupId: Long): Int {
        val db = this.readableDatabase
        val columns = arrayOf(KEY_GROUP_LEADER_ID)
        val selection = "$KEY_GROUP_ID = ?"
        val selectionArgs = arrayOf(groupId.toString())
        val cursor = db.query(TABLE_GROUPS, columns, selection, selectionArgs, null, null, null)
        var leaderId = -1
        if (cursor.moveToFirst()) {
            leaderId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_GROUP_LEADER_ID))
        }
        cursor.close()
        return leaderId
    }

    fun createTask(taskName: String, taskDescription: String, groupId: Long, dueDate: Long): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_TASK_NAME, taskName)
        values.put(KEY_TASK_DESCRIPTION, taskDescription)
        values.put(KEY_TASK_GROUP_ID, groupId)
        values.put(KEY_DUE_DATE, dueDate)
        values.put(KEY_STATUS, "Not Started") // Default status
        return db.insert(TABLE_TASKS, null, values)
    }

    fun createSubtask(taskId: Long, subtaskName: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_SUBTASK_TASK_ID, taskId)
        values.put(KEY_SUBTASK_NAME, subtaskName)
        return db.insert(TABLE_SUBTASKS, null, values)
    }

    fun getTasksForGroup(groupId: Long): List<Task> {
        val tasks = mutableListOf<Task>()
        val db = this.readableDatabase
        val query = "SELECT t.*, g.$KEY_GROUP_NAME FROM $TABLE_TASKS t INNER JOIN $TABLE_GROUPS g ON t.$KEY_TASK_GROUP_ID = g.$KEY_GROUP_ID WHERE t.$KEY_TASK_GROUP_ID = ? ORDER BY t.$KEY_DUE_DATE"
        val cursor = db.rawQuery(query, arrayOf(groupId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val taskId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TASK_ID))
                val assignedTo = getAssignedUsersForTask(taskId)
                val task = Task(
                    id = taskId,
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TASK_NAME)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TASK_DESCRIPTION)),
                    groupId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TASK_GROUP_ID)),
                    groupName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_NAME)),
                    dueDate = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DUE_DATE)),
                    status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS)),
                    assignedTo = assignedTo
                )
                tasks.add(task)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return tasks
    }

    fun getUpcomingTasksForUser(userId: Int): List<Task> {
        val tasks = mutableListOf<Task>()
        val db = this.readableDatabase
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 3) // Due within the next 3 days
        val dueDateLimit = calendar.timeInMillis

        val query = "SELECT t.*, g.$KEY_GROUP_NAME FROM $TABLE_TASKS t " +
                "INNER JOIN $TABLE_GROUPS g ON t.$KEY_TASK_GROUP_ID = g.$KEY_GROUP_ID " +
                "INNER JOIN $TABLE_USER_GROUPS ug ON g.$KEY_GROUP_ID = ug.$KEY_GROUP_ID " +
                "WHERE ug.$KEY_USER_ID = ? AND t.$KEY_DUE_DATE <= $dueDateLimit AND t.$KEY_STATUS != 'Complete'"

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val taskId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TASK_ID))
                val assignedTo = getAssignedUsersForTask(taskId)
                val task = Task(
                    id = taskId,
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TASK_NAME)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TASK_DESCRIPTION)),
                    groupId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TASK_GROUP_ID)),
                    groupName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_NAME)),
                    dueDate = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DUE_DATE)),
                    status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS)),
                    assignedTo = assignedTo
                )
                tasks.add(task)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return tasks
    }

    fun assignTaskToUser(taskId: Long, userId: Int): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_ASSIGNMENT_TASK_ID, taskId)
        values.put(KEY_ASSIGNMENT_USER_ID, userId)
        val success = db.insertWithOnConflict(TABLE_TASK_ASSIGNMENTS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        return success != -1L
    }

    private fun getAssignedUsersForTask(taskId: Long): List<Int> {
        val userIds = mutableListOf<Int>()
        val db = this.readableDatabase
        val selection = "$KEY_ASSIGNMENT_TASK_ID = ?"
        val selectionArgs = arrayOf(taskId.toString())
        val cursor = db.query(TABLE_TASK_ASSIGNMENTS, arrayOf(KEY_ASSIGNMENT_USER_ID), selection, selectionArgs, null, null, null)

        if (cursor.moveToFirst()) {
            do {
                val userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ASSIGNMENT_USER_ID))
                userIds.add(userId)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return userIds
    }

    fun getTask(taskId: Long): Task? {
        val db = this.readableDatabase
        val query = "SELECT t.*, g.$KEY_GROUP_NAME FROM $TABLE_TASKS t INNER JOIN $TABLE_GROUPS g ON t.$KEY_TASK_GROUP_ID = g.$KEY_GROUP_ID WHERE t.$KEY_TASK_ID = ?"

        val cursor = db.rawQuery(query, arrayOf(taskId.toString()))

        var task: Task? = null
        if (cursor.moveToFirst()) {
            val assignedTo = getAssignedUsersForTask(taskId)
            task = Task(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TASK_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TASK_NAME)),
                description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TASK_DESCRIPTION)),
                groupId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TASK_GROUP_ID)),
                groupName = cursor.getString(cursor.getColumnIndexOrThrow(KEY_GROUP_NAME)),
                dueDate = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DUE_DATE)),
                status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS)),
                assignedTo = assignedTo
            )
        }
        cursor.close()
        return task
    }

    fun updateTaskStatus(taskId: Long, status: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_STATUS, status)
        val selection = "$KEY_TASK_ID = ?"
        val selectionArgs = arrayOf(taskId.toString())
        val count = db.update(TABLE_TASKS, values, selection, selectionArgs)
        return count > 0
    }

    fun updateSubtaskStatus(subtaskId: Long, isCompleted: Boolean): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_SUBTASK_IS_COMPLETED, if (isCompleted) 1 else 0)
        val selection = "$KEY_SUBTASK_ID = ?"
        val selectionArgs = arrayOf(subtaskId.toString())
        val count = db.update(TABLE_SUBTASKS, values, selection, selectionArgs)
        return count > 0
    }

    fun addComment(taskId: Long, userId: Int, commentText: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_COMMENT_TASK_ID, taskId)
        values.put(KEY_COMMENT_USER_ID, userId)
        values.put(KEY_COMMENT_TEXT, commentText)
        values.put(KEY_COMMENT_TIMESTAMP, System.currentTimeMillis())
        return db.insert(TABLE_COMMENTS, null, values)
    }

    fun getCommentsForTask(taskId: Long): List<Comment> {
        val comments = mutableListOf<Comment>()
        val db = this.readableDatabase
        val query = "SELECT c.*, u.$KEY_USERNAME FROM $TABLE_COMMENTS c INNER JOIN $TABLE_USERS u ON c.$KEY_COMMENT_USER_ID = u.$KEY_ID WHERE c.$KEY_COMMENT_TASK_ID = ? ORDER BY c.$KEY_COMMENT_TIMESTAMP DESC"
        val cursor = db.rawQuery(query, arrayOf(taskId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val comment = Comment(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_COMMENT_ID)),
                    taskId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_COMMENT_TASK_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COMMENT_USER_ID)),
                    username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                    commentText = cursor.getString(cursor.getColumnIndexOrThrow(KEY_COMMENT_TEXT)),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_COMMENT_TIMESTAMP))
                )
                comments.add(comment)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return comments
    }

    fun updateTask(taskId: Long, title: String, description: String, dueDate: Long, status: String, assignedTo: List<Int>?) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(KEY_TASK_NAME, title)
                put(KEY_TASK_DESCRIPTION, description)
                put(KEY_DUE_DATE, dueDate)
                put(KEY_STATUS, status)
            }
            db.update(TABLE_TASKS, values, "$KEY_TASK_ID = ?", arrayOf(taskId.toString()))

            db.delete(TABLE_TASK_ASSIGNMENTS, "$KEY_ASSIGNMENT_TASK_ID = ?", arrayOf(taskId.toString()))
            assignedTo?.forEach {
                assignTaskToUser(taskId, it)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getGroupIdByCode(groupCode: String): Long {
        val db = this.readableDatabase
        val columns = arrayOf(KEY_GROUP_ID)
        val selection = "$KEY_GROUP_CODE = ?"
        val selectionArgs = arrayOf(groupCode)
        val cursor = db.query(TABLE_GROUPS, columns, selection, selectionArgs, null, null, null)
        var groupId = -1L
        if (cursor.moveToFirst()) {
            groupId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_GROUP_ID))
        }
        cursor.close()
        return groupId
    }

    fun updateMemberRole(groupId: Long, userId: Int, role: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_ROLE, role)
        val selection = "$KEY_GROUP_ID = ? AND $KEY_USER_ID = ?"
        val selectionArgs = arrayOf(groupId.toString(), userId.toString())
        val count = db.update(TABLE_USER_GROUPS, values, selection, selectionArgs)
        return count > 0
    }

    fun removeMemberFromGroup(groupId: Long, userId: Int): Boolean {
        val db = this.writableDatabase
        val selection = "$KEY_GROUP_ID = ? AND $KEY_USER_ID = ?"
        val selectionArgs = arrayOf(groupId.toString(), userId.toString())
        val count = db.delete(TABLE_USER_GROUPS, selection, selectionArgs)
        return count > 0
    }

    fun updateGroup(groupId: Long, groupName: String, groupDescription: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_GROUP_NAME, groupName)
        values.put(KEY_GROUP_DESCRIPTION, groupDescription)
        val selection = "$KEY_GROUP_ID = ?"
        val selectionArgs = arrayOf(groupId.toString())
        val count = db.update(TABLE_GROUPS, values, selection, selectionArgs)
        return count > 0
    }

    fun deleteGroup(groupId: Long): Boolean {
        val db = this.writableDatabase
        val selection = "$KEY_GROUP_ID = ?"
        val selectionArgs = arrayOf(groupId.toString())
        val count = db.delete(TABLE_GROUPS, selection, selectionArgs)
        return count > 0
    }

    fun deleteTask(taskId: Long): Boolean {
        val db = this.writableDatabase
        val selection = "$KEY_TASK_ID = ?"
        val selectionArgs = arrayOf(taskId.toString())
        val count = db.delete(TABLE_TASKS, selection, selectionArgs)
        return count > 0
    }

    private fun getGroupProgress(groupId: Long): Int {
        val tasks = getTasksForGroup(groupId)
        if (tasks.isEmpty()) return 0
        val completedTasks = tasks.count { it.status == "Complete" }
        return (completedTasks * 100) / tasks.size
    }

    fun getTotalTasksForUser(userId: Int): Int {
        val db = this.readableDatabase
        val query = "SELECT COUNT(*) FROM $TABLE_TASKS t " +
                "INNER JOIN $TABLE_USER_GROUPS ug ON t.$KEY_TASK_GROUP_ID = ug.$KEY_GROUP_ID " +
                "WHERE ug.$KEY_USER_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun getPendingTasksForUser(userId: Int): Int {
        val db = this.readableDatabase
        val query = "SELECT COUNT(*) FROM $TABLE_TASKS t " +
                "INNER JOIN $TABLE_USER_GROUPS ug ON t.$KEY_TASK_GROUP_ID = ug.$KEY_GROUP_ID " +
                "WHERE ug.$KEY_USER_ID = ? AND t.$KEY_STATUS != 'Complete'"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun getPendingTaskCountForGroup(groupId: Long): Int {
        val db = this.readableDatabase
        val query = "SELECT COUNT(*) FROM $TABLE_TASKS WHERE $KEY_TASK_GROUP_ID = ? AND $KEY_STATUS != 'Complete'"
        val cursor = db.rawQuery(query, arrayOf(groupId.toString()))
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun getDueTasksForUser(userId: Int): Int {
        val db = this.readableDatabase
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 3) // Due within the next 3 days
        val dueDateLimit = calendar.timeInMillis

        val query = "SELECT COUNT(*) FROM $TABLE_TASKS t " +
                "INNER JOIN $TABLE_USER_GROUPS ug ON t.$KEY_TASK_GROUP_ID = ug.$KEY_GROUP_ID " +
                "WHERE ug.$KEY_USER_ID = ? AND t.$KEY_DUE_DATE <= $dueDateLimit AND t.$KEY_STATUS != 'Complete'"

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun addLabel(labelName: String, labelColor: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_LABEL_NAME, labelName)
        values.put(KEY_LABEL_COLOR, labelColor)
        return db.insert(TABLE_LABELS, null, values)
    }

    fun addTaskLabel(taskId: Long, labelId: Long): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_TASK_LABEL_TASK_ID, taskId)
        values.put(KEY_TASK_LABEL_LABEL_ID, labelId)
        val success = db.insertWithOnConflict(TABLE_TASK_LABELS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        return success != -1L
    }

    fun getLabelsForTask(taskId: Long): List<Label> {
        val labels = mutableListOf<Label>()
        val db = this.readableDatabase
        val query = "SELECT l.* FROM $TABLE_LABELS l " +
                "INNER JOIN $TABLE_TASK_LABELS tl ON l.$KEY_LABEL_ID = tl.$KEY_TASK_LABEL_LABEL_ID " +
                "WHERE tl.$KEY_TASK_LABEL_TASK_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(taskId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val label = Label(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_LABEL_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LABEL_NAME)),
                    color = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LABEL_COLOR))
                )
                labels.add(label)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return labels
    }

    fun getAllLabels(): List<Label> {
        val labels = mutableListOf<Label>()
        val db = this.readableDatabase
        val cursor = db.query(TABLE_LABELS, null, null, null, null, null, null)

        if (cursor.moveToFirst()) {
            do {
                val label = Label(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_LABEL_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LABEL_NAME)),
                    color = cursor.getString(cursor.getColumnIndexOrThrow(KEY_LABEL_COLOR))
                )
                labels.add(label)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return labels
    }

    fun updateLabel(labelId: Long, newName: String, newColor: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_LABEL_NAME, newName)
        values.put(KEY_LABEL_COLOR, newColor)
        val selection = "$KEY_LABEL_ID = ?"
        val selectionArgs = arrayOf(labelId.toString())
        val count = db.update(TABLE_LABELS, values, selection, selectionArgs)
        return count > 0
    }

    fun getSubtasksForTask(taskId: Long): List<Subtask> {
        val subtasks = mutableListOf<Subtask>()
        val db = this.readableDatabase
        val selection = "$KEY_SUBTASK_TASK_ID = ?"
        val selectionArgs = arrayOf(taskId.toString())
        val cursor = db.query(TABLE_SUBTASKS, null, selection, selectionArgs, null, null, null)

        if (cursor.moveToFirst()) {
            do {
                val subtask = Subtask(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SUBTASK_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SUBTASK_NAME)),
                    isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SUBTASK_IS_COMPLETED)) == 1,
                    taskId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_SUBTASK_TASK_ID))
                )
                subtasks.add(subtask)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return subtasks
    }
}
