package com.example.assignmate

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignmate.adapter.CommentAdapter
import com.example.assignmate.adapter.LabelAdapter
import com.example.assignmate.adapter.SubtaskAdapter
import com.example.assignmate.databinding.ActivityTaskDetailBinding
import com.example.assignmate.model.Label
import com.example.assignmate.model.Subtask
import com.example.assignmate.model.Task
import com.google.android.material.chip.Chip
import yuku.ambilwarna.AmbilWarnaDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var notificationHelper: NotificationHelper
    private var taskId: Long = -1
    private var currentUserId: Int = -1
    private var groupLeaderId: Int = -1
    private var isAssigned: Boolean = false
    private var task: Task? = null
    private var defaultColor: Int = 0
    private var saveMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)
        notificationHelper = NotificationHelper(this)
        taskId = intent.getLongExtra("TASK_ID", -1)
        currentUserId = intent.getIntExtra("USER_ID", -1)

        task = databaseHelper.getTask(taskId)
        if (task == null) {
            finish()
            return
        }

        groupLeaderId = databaseHelper.getGroupLeaderId(task!!.groupId)
        isAssigned = task!!.assignedTo?.contains(currentUserId) == true
        defaultColor = ContextCompat.getColor(this, R.color.purple_200)

        setupToolbar()
        setupViews()
        setupListeners()
        loadComments()
        loadLabels()
        loadSubtasks()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Task Details"
    }

    private fun setupViews() {
        binding.taskTitleInput.setText(task!!.name)
        binding.taskDescriptionInput.setText(task!!.description)

        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, arrayOf("Not Started", "In progress", "Complete"))
        binding.statusDropdown.setAdapter(statusAdapter)
        binding.statusDropdown.setText(task!!.status, false)
        setStatusColor(task!!.status)

        if (!isAssigned) {
            binding.statusDropdown.isEnabled = false
        }

        binding.dueDateInput.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task!!.dueDate))

        updateAssignedMembersChips()
    }

    private fun updateAssignedMembersChips() {
        binding.assignedMembersChipGroup.removeAllViews()
        val assignedMembers = databaseHelper.getGroupMembers(task!!.groupId).filter { task!!.assignedTo?.contains(it.id) == true }
        for (member in assignedMembers) {
            val chip = Chip(this)
            chip.text = member.name
            binding.assignedMembersChipGroup.addView(chip)
        }
    }

    private fun setupListeners() {
        binding.taskTitleInput.addTextChangedListener(textWatcher)
        binding.taskDescriptionInput.addTextChangedListener(textWatcher)

        binding.statusDropdown.setOnItemClickListener { _, _, position, _ ->
            val newStatus = (binding.statusDropdown.adapter.getItem(position)) as String
            task = task?.copy(status = newStatus)
            setStatusColor(newStatus)
            saveMenuItem?.isVisible = true

            task!!.assignedTo?.forEach { userId ->
                if (userId != currentUserId) {
                    notificationHelper.sendNotification(userId, "Task Status Updated", "The status of task \"${task!!.name}\" has been updated to $newStatus.", taskId.toInt())
                }
            }
        }

        binding.dueDateInput.setOnClickListener {
            if (currentUserId == groupLeaderId) {
                showDatePickerDialog()
            }
        }

        binding.editAssignmentsButton.setOnClickListener {
            if (currentUserId == groupLeaderId) {
                showEditAssignmentsDialog()
            }
        }

        binding.addLabelButton.setOnClickListener {
            showAddLabelDialog()
        }

        binding.addSubtaskButton.setOnClickListener {
            showAddSubtaskDialog()
        }

        binding.addCommentButton.setOnClickListener {
            val commentText = binding.commentInput.text.toString()
            if (commentText.isNotEmpty()) {
                val newCommentId = databaseHelper.addComment(taskId, currentUserId, commentText)
                if (newCommentId != -1L) {
                    loadComments()
                    binding.commentInput.text.clear()

                    task!!.assignedTo?.forEach { userId ->
                        if (userId != currentUserId) {
                            notificationHelper.sendNotification(userId, "New Comment on Task", "A new comment has been added to the task \"${task!!.name}\".", taskId.toInt())
                        }
                    }
                } else {
                    Toast.makeText(this, "Failed to add comment", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            saveMenuItem?.isVisible = true
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, {
            _, selectedYear, selectedMonth, selectedDay ->
            val newDueDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            binding.dueDateInput.setText(newDueDate)
            saveMenuItem?.isVisible = true
        }, year, month, day)
        datePickerDialog.show()
    }

    private fun showEditAssignmentsDialog() {
        val members = databaseHelper.getGroupMembers(task!!.groupId)
        val memberNames = members.map { it.name }.toTypedArray()
        val selectedMembers = BooleanArray(memberNames.size) {
            task!!.assignedTo?.contains(members[it].id) == true
        }

        AlertDialog.Builder(this)
            .setTitle("Assign Members")
            .setMultiChoiceItems(memberNames, selectedMembers) { _, which, isChecked ->
                selectedMembers[which] = isChecked
            }
            .setPositiveButton("OK") { _, _ ->
                val newAssignedTo = mutableListOf<Int>()
                for (i in selectedMembers.indices) {
                    if (selectedMembers[i]) {
                        newAssignedTo.add(members[i].id)
                    }
                }
                task = task!!.copy(assignedTo = newAssignedTo)
                updateAssignedMembersChips()
                saveMenuItem?.isVisible = true
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddLabelDialog() {
        val allLabels = databaseHelper.getAllLabels()
        val assignedLabels = databaseHelper.getLabelsForTask(taskId)
        val unassignedLabels = allLabels.filter { it !in assignedLabels }

        if (unassignedLabels.isEmpty()) {
            Toast.makeText(this, "No unassigned labels available", Toast.LENGTH_SHORT).show()
            return
        }

        val labelNames = unassignedLabels.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Add Label")
            .setItems(labelNames) { _, which ->
                val selectedLabel = unassignedLabels[which]
                databaseHelper.addTaskLabel(taskId, selectedLabel.id)
                loadLabels()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddSubtaskDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_subtask, null)
        builder.setView(view)

        val subtaskNameInput = view.findViewById<EditText>(R.id.subtask_name_input)

        builder.setPositiveButton("Add") { _, _ ->
            val subtaskName = subtaskNameInput.text.toString()

            if (subtaskName.isNotEmpty()) {
                val newSubtaskId = databaseHelper.createSubtask(taskId, subtaskName)
                if (newSubtaskId != -1L) {
                    loadSubtasks()
                } else {
                    Toast.makeText(this, "Failed to add subtask", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a subtask name", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun saveChanges() {
        val newTitle = binding.taskTitleInput.text.toString()
        val newDescription = binding.taskDescriptionInput.text.toString()

        if (newTitle.isEmpty() && newDescription.isEmpty()) {
            Toast.makeText(this, "Task title or description cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val newDueDateText = binding.dueDateInput.text.toString()
        val calendar = Calendar.getInstance()
        val dateParts = newDueDateText.split("/")
        calendar.set(dateParts[2].toInt(), dateParts[1].toInt() - 1, dateParts[0].toInt())
        val newDueDateMillis = calendar.timeInMillis

        databaseHelper.updateTask(taskId, newTitle, newDescription, newDueDateMillis, task!!.status, task!!.assignedTo)

        task!!.assignedTo?.forEach { userId ->
            if (userId != currentUserId) {
                notificationHelper.sendNotification(userId, "New Task Assignment", "You have been assigned to the task \"${task!!.name}\".", taskId.toInt())
            }
        }

        Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, SingleGroupActivity::class.java)
        intent.putExtra("GROUP_ID", task!!.groupId)
        intent.putExtra("USER_ID", currentUserId)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun loadComments() {
        val comments = databaseHelper.getCommentsForTask(taskId)
        binding.commentsRecyclerView.adapter = CommentAdapter(comments)
    }

    private fun loadLabels() {
        val labels = databaseHelper.getLabelsForTask(taskId)
        binding.labelsRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.labelsRecyclerView.adapter = LabelAdapter(labels)
    }

    private fun loadSubtasks() {
        val subtasks = databaseHelper.getSubtasksForTask(taskId)
        binding.subtasksRecyclerView.adapter = SubtaskAdapter(subtasks) { subtask, isChecked ->
            databaseHelper.updateSubtaskStatus(subtask.id, isChecked)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.task_detail_menu, menu)
        saveMenuItem = menu?.findItem(R.id.action_save_task)
        saveMenuItem?.isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save_task -> {
                saveChanges()
                true
            }
            R.id.action_delete_task -> {
                showDeleteConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { _, _ ->
                if (databaseHelper.deleteTask(taskId)) {
                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setStatusColor(status: String) {
        val colorRes = when (status) {
            "Not Started" -> R.color.status_not_started
            "In progress" -> R.color.status_in_progress
            "Complete" -> R.color.status_complete
            else -> android.R.color.black
        }
        binding.statusDropdown.setTextColor(ContextCompat.getColor(this, colorRes))
    }
}
