package com.example.assignmate

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignmate.adapter.CommentAdapter
import com.example.assignmate.databinding.ActivityTaskDetailBinding
import com.example.assignmate.model.Comment
import com.example.assignmate.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding

    // Firebase References
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private var taskId: String = ""
    private var currentTask: Task? = null
    private var hasUnsavedChanges = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get Task ID (String for Firebase)
        taskId = intent.getStringExtra("TASK_ID") ?: ""
        if (taskId.isEmpty()) {
            Toast.makeText(this, "Error: Task ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupListeners()

        // Load Data from Firebase
        loadTaskDetails()
        loadComments()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Task Details"
    }

    // --- FIREBASE LOADING LOGIC ---

    private fun loadTaskDetails() {
        db.child("Tasks").child(taskId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val task = snapshot.getValue(Task::class.java)
                if (task != null) {
                    currentTask = task
                    updateUI(task)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateUI(task: Task) {
        // Prevent overwriting user input if they are typing
        if (!binding.taskTitleInput.hasFocus()) {
            binding.taskTitleInput.setText(task.name)
        }
        if (!binding.taskDescriptionInput.hasFocus()) {
            binding.taskDescriptionInput.setText(task.description)
        }

        // Setup Status Dropdown
        val statuses = arrayOf("Not Started", "In Progress", "Completed")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statuses)
        binding.statusDropdown.setAdapter(adapter)
        binding.statusDropdown.setText(task.status, false)
        setStatusColor(task.status)

        // Due Date
        binding.dueDateInput.setText(task.dueDate)

        // Assigned To Chip
        binding.assignedMembersChipGroup.removeAllViews()
        if (task.assignedToName.isNotEmpty() && task.assignedToName != "Unassigned") {
            val chip = com.google.android.material.chip.Chip(this)
            chip.text = task.assignedToName
            binding.assignedMembersChipGroup.addView(chip)
        }
    }

    private fun loadComments() {
        db.child("Comments").child(taskId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comments = mutableListOf<Comment>()
                for (child in snapshot.children) {
                    val comment = child.getValue(Comment::class.java)
                    if (comment != null) comments.add(comment)
                }
                // Sort by timestamp (newest at bottom)
                comments.sortBy { it.timestamp }

                binding.commentsRecyclerView.layoutManager = LinearLayoutManager(this@TaskDetailActivity)
                binding.commentsRecyclerView.adapter = CommentAdapter(comments)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // --- LISTENERS & ACTIONS ---

    private fun setupListeners() {
        binding.taskTitleInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { hasUnsavedChanges = true }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.statusDropdown.setOnItemClickListener { _, _, _, _ ->
            val newStatus = binding.statusDropdown.text.toString()
            setStatusColor(newStatus)
            saveTaskField("status", newStatus)
        }

        binding.dueDateInput.setOnClickListener {
            showDatePicker()
        }

        binding.addCommentButton.setOnClickListener {
            postComment()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            calendar.set(year, month, day)
            val dateStr = fmt.format(calendar.time)

            binding.dueDateInput.setText(dateStr)
            saveTaskField("dueDate", dateStr)

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun postComment() {
        // FIX: Added safe call operator (?.) to prevent crash on null Editable
        val text = binding.commentInput.text?.toString()?.trim() ?: ""

        if (text.isEmpty()) return

        val userId = auth.currentUser?.uid ?: return

        // Get username first
        db.child("Users").child(userId).get().addOnSuccessListener { snap ->
            val username = snap.child("username").value.toString()

            val commentId = db.child("Comments").child(taskId).push().key ?: return@addOnSuccessListener
            val timestamp = System.currentTimeMillis()

            // FIX: Using named arguments to ensure correct mapping to the new Comment model
            val comment = Comment(
                id = commentId,
                userId = userId,
                username = username,
                commentText = text,
                timestamp = timestamp
            )

            db.child("Comments").child(taskId).child(commentId).setValue(comment)

            // FIX: Added safe call (?.) for clear
            binding.commentInput.text?.clear()
            Toast.makeText(this, "Comment posted", Toast.LENGTH_SHORT).show()
        }
    }

    // --- HELPER FUNCTIONS ---

    private fun saveTaskField(field: String, value: Any) {
        db.child("Tasks").child(taskId).child(field).setValue(value)
    }

    override fun onPause() {
        super.onPause()
        if (hasUnsavedChanges && currentTask != null) {
            val updates = mapOf(
                "name" to binding.taskTitleInput.text.toString(),
                "description" to binding.taskDescriptionInput.text.toString()
            )
            db.child("Tasks").child(taskId).updateChildren(updates)
        }
    }

    private fun setStatusColor(status: String) {
        val color = when (status) {
            "Not Started" -> Color.LTGRAY
            "In Progress" -> Color.parseColor("#FF9800")
            "Completed" -> Color.parseColor("#4CAF50")
            else -> Color.LTGRAY
        }
        binding.statusDropdown.backgroundTintList = ColorStateList.valueOf(color)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}