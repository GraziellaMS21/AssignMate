package com.example.assignmate

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.model.Comment
import com.example.assignmate.model.Task

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private var currentTaskId: Long = -1
    private var currentUserId: Int = -1

    private lateinit var backButton: ImageButton
    private lateinit var taskNameTextView: TextView
    private lateinit var taskDescriptionTextView: TextView
    private lateinit var taskStatusTextView: TextView
    private lateinit var completeTaskButton: Button
    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentInput: EditText
    private lateinit var addCommentButton: Button

    private lateinit var commentAdapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        databaseHelper = DatabaseHelper(this)
        currentTaskId = intent.getLongExtra("TASK_ID", -1)
        currentUserId = intent.getIntExtra("USER_ID", -1)

        if (currentTaskId == -1L || currentUserId == -1) {
            Toast.makeText(this, "Error: Task or User not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        backButton = findViewById(R.id.back_button)
        taskNameTextView = findViewById(R.id.task_detail_name)
        taskDescriptionTextView = findViewById(R.id.task_detail_description)
        taskStatusTextView = findViewById(R.id.task_detail_status)
        completeTaskButton = findViewById(R.id.complete_task_button)
        commentsRecyclerView = findViewById(R.id.comments_recycler_view)
        commentInput = findViewById(R.id.comment_input)
        addCommentButton = findViewById(R.id.add_comment_button)

        loadTaskDetails()

        backButton.setOnClickListener {
            finish()
        }

        completeTaskButton.setOnClickListener {
            updateTaskStatus("Completed")
        }

        addCommentButton.setOnClickListener {
            val commentText = commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(commentText)
            }
        }
    }

    private fun loadTaskDetails() {
        val task = databaseHelper.getTask(currentTaskId)
        if (task != null) {
            taskNameTextView.text = task.name
            taskDescriptionTextView.text = task.description
            taskStatusTextView.text = "Status: ${task.status}"

            if (task.status == "Completed") {
                completeTaskButton.isEnabled = false
                completeTaskButton.text = "Task is Already Completed"
            }

            loadComments()
        } else {
            Toast.makeText(this, "Task not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadComments() {
        val comments = databaseHelper.getCommentsForTask(currentTaskId)
        commentAdapter = CommentAdapter(comments)
        commentsRecyclerView.adapter = commentAdapter
    }

    private fun updateTaskStatus(newStatus: String) {
        if (databaseHelper.updateTaskStatus(currentTaskId, newStatus)) {
            Toast.makeText(this, "Task marked as complete!", Toast.LENGTH_SHORT).show()
            loadTaskDetails() // Refresh the task details to show the new status
        } else {
            Toast.makeText(this, "Failed to update task status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addComment(commentText: String) {
        val newCommentId = databaseHelper.addComment(currentTaskId, currentUserId, commentText)
        if (newCommentId != -1L) {
            commentInput.text.clear()
            Toast.makeText(this, "Comment added", Toast.LENGTH_SHORT).show()
            loadComments() // Refresh the comments list
        } else {
            Toast.makeText(this, "Failed to add comment", Toast.LENGTH_SHORT).show()
        }
    }
}
