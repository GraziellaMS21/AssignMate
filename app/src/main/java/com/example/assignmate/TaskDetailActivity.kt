package com.example.assignmate

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.adapter.CommentAdapter
import com.example.assignmate.adapter.SelectableLabelAdapter
import com.example.assignmate.adapter.SubtaskAdapter
import com.example.assignmate.databinding.ActivityTaskDetailBinding
import com.example.assignmate.model.Task
import com.google.android.material.chip.Chip
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
    private var originalTask: Task? = null
    private var modifiedTask: Task? = null
    private var hasUnsavedChanges = false
    private var currentUserRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)
        notificationHelper = NotificationHelper(this)
        taskId = intent.getLongExtra("TASK_ID", -1)
        currentUserId = intent.getIntExtra("USER_ID", -1)

        originalTask = databaseHelper.getTask(taskId)
        if (originalTask == null) {
            finish()
            return
        }
        modifiedTask = originalTask?.copy(
            assignedTo = originalTask?.assignedTo?.toMutableList()
        )

        groupLeaderId = databaseHelper.getGroupLeaderId(originalTask!!.groupId)
        isAssigned = originalTask!!.assignedTo?.contains(currentUserId) == true
        currentUserRole = databaseHelper.getRoleForUserInGroup(currentUserId, originalTask!!.groupId)

        setupToolbar()
        setupViews()
        setupListeners()
        loadComments()
        loadSubtasks()
        updateLabelChips()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Task Details"
    }

    private fun setupViews() {
        binding.taskTitleInput.setText(originalTask!!.name)
        binding.taskDescriptionInput.setText(originalTask!!.description)

        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, arrayOf("Not Started", "In progress", "Complete"))
        binding.statusDropdown.setAdapter(statusAdapter)
        binding.statusDropdown.setText(originalTask!!.status, false)
        setStatusColor(originalTask!!.status)

        val canManageTask = currentUserRole == "leader" || currentUserRole == "co-leader"

        binding.statusDropdown.isEnabled = canManageTask || isAssigned
        binding.taskTitleInput.isEnabled = canManageTask
        binding.taskDescriptionInput.isEnabled = canManageTask
        binding.dueDateInput.isEnabled = canManageTask
        binding.addAssigneeIcon.isEnabled = canManageTask
        binding.addLabelIcon.isEnabled = canManageTask
        binding.addSubtaskButton.isEnabled = canManageTask

        binding.dueDateInput.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(originalTask!!.dueDate))

        updateAssignedMembersChips()
    }

    private fun updateAssignedMembersChips() {
        binding.assignedMembersChipGroup.removeAllViews()
        val assignedMembers = databaseHelper.getGroupMembers(originalTask!!.groupId).filter { modifiedTask!!.assignedTo?.contains(it.id) == true }
        for (member in assignedMembers) {
            val chip = Chip(this)
            chip.text = member.name
            chip.isCloseIconVisible = true
            chip.setOnCloseIconClickListener {
                (modifiedTask?.assignedTo as? MutableList)?.remove(member.id)
                updateAssignedMembersChips()
                checkForChanges()
            }
            binding.assignedMembersChipGroup.addView(chip)
        }
    }

    private fun updateLabelChips() {
        binding.labelsChipGroup.removeAllViews()
        val assignedLabels = databaseHelper.getLabelsForTask(taskId)

        for (label in assignedLabels) {
            val chip = Chip(this)
            chip.text = label.name
            chip.isCloseIconVisible = true

            try {
                val color = Color.parseColor(label.color)
                chip.chipBackgroundColor = ColorStateList.valueOf(color)

                val luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
                if (luminance > 0.5) {
                    chip.setTextColor(Color.BLACK)
                } else {
                    chip.setTextColor(Color.WHITE)
                }
            } catch (e: IllegalArgumentException) {
                chip.chipBackgroundColor = ColorStateList.valueOf(Color.LTGRAY)
                chip.setTextColor(Color.BLACK)
            }

            chip.setOnCloseIconClickListener {
                databaseHelper.removeTaskLabel(taskId, label.id)
                updateLabelChips()
                checkForChanges()
            }
            binding.labelsChipGroup.addView(chip)
        }
    }

    private fun setupListeners() {
        binding.taskTitleInput.addTextChangedListener(textWatcher)
        binding.taskDescriptionInput.addTextChangedListener(textWatcher)

        binding.statusDropdown.setOnItemClickListener { _, _, position, _ ->
            val newStatus = (binding.statusDropdown.adapter.getItem(position)) as String
            modifiedTask = modifiedTask?.copy(status = newStatus)
            setStatusColor(newStatus)
            checkForChanges()
        }

        binding.dueDateInput.setOnClickListener {
            if (currentUserRole == "leader" || currentUserRole == "co-leader") {
                showDatePickerDialog()
            }
        }

        binding.addAssigneeIcon.setOnClickListener {
            if (currentUserRole == "leader" || currentUserRole == "co-leader") {
                showEditAssignmentsDialog()
            }
        }

        binding.addLabelIcon.setOnClickListener {
            showSelectLabelsDialog()
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
                    binding.commentInput.text?.clear()
                    notifyUsersOfComment()
                } else {
                    Toast.makeText(this, "Failed to add comment", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            checkForChanges()
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun checkForChanges() {
        val currentTitle = binding.taskTitleInput.text.toString()
        val currentDescription = binding.taskDescriptionInput.text.toString()

        val originalLabelIds = originalTask?.let { databaseHelper.getLabelsForTask(it.id).map { l -> l.id }.toSet() } ?: emptySet<Long>()
        val currentLabelIds = databaseHelper.getLabelsForTask(taskId).map { it.id }.toSet()
        val labelsChanged = originalLabelIds != currentLabelIds

        hasUnsavedChanges = originalTask?.name != currentTitle ||
                originalTask?.description != currentDescription ||
                originalTask?.status != modifiedTask?.status ||
                originalTask?.dueDate != modifiedTask?.dueDate ||
                originalTask?.assignedTo?.toSet() != modifiedTask?.assignedTo?.toSet() ||
                labelsChanged

        invalidateOptionsMenu()
    }


    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = modifiedTask!!.dueDate
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, {
            _, selectedYear, selectedMonth, selectedDay ->
            val newDueDateCalendar = Calendar.getInstance()
            newDueDateCalendar.set(selectedYear, selectedMonth, selectedDay)
            modifiedTask = modifiedTask?.copy(dueDate = newDueDateCalendar.timeInMillis)
            binding.dueDateInput.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(modifiedTask!!.dueDate))
            checkForChanges()
        }, year, month, day)
        datePickerDialog.show()
    }

    private fun showEditAssignmentsDialog() {
        val members = databaseHelper.getGroupMembers(originalTask!!.groupId)
        val memberNames = members.map { it.name }.toTypedArray()
        val selectedMembers = BooleanArray(memberNames.size) {
            modifiedTask!!.assignedTo?.contains(members[it].id) == true
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
                modifiedTask = modifiedTask!!.copy(assignedTo = newAssignedTo)
                updateAssignedMembersChips()
                checkForChanges()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSelectLabelsDialog() {
        val allLabels = databaseHelper.getAllLabels()
        val assignedLabelIds = databaseHelper.getLabelsForTask(taskId).map { it.id }.toMutableSet()

        val dialogView = layoutInflater.inflate(R.layout.dialog_select_labels, null)
        val labelsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.labels_recycler_view)
        labelsRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = SelectableLabelAdapter(allLabels, assignedLabelIds)
        labelsRecyclerView.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Select Labels")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newLabelIds = adapter.getSelectedLabelIds()
                databaseHelper.updateTaskLabels(taskId, newLabelIds)
                updateLabelChips()
                checkForChanges()
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

        if (newTitle.isEmpty()) {
            Toast.makeText(this, "Task title cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        databaseHelper.updateTask(taskId, newTitle, newDescription, modifiedTask!!.dueDate, modifiedTask!!.status, modifiedTask!!.assignedTo)
        notifyUsersOfChanges()
        hasUnsavedChanges = false
        Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun notifyUsersOfChanges(){
        originalTask?.assignedTo?.forEach { userId ->
            if (userId != currentUserId) {
                notificationHelper.sendNotification(userId, "Task Updated", "The task \"${originalTask!!.name}\" has been updated.", taskId.toInt())
            }
        }
    }

    private fun notifyUsersOfComment(){
        originalTask?.assignedTo?.forEach { userId ->
            if (userId != currentUserId) {
                notificationHelper.sendNotification(userId, "New Comment", "A new comment was added to \"${originalTask!!.name}\".", taskId.toInt())
            }
        }
    }

    private fun loadComments() {
        val comments = databaseHelper.getCommentsForTask(taskId)
        binding.commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.commentsRecyclerView.adapter = CommentAdapter(comments)
    }

    private fun loadSubtasks() {
        val subtasks = databaseHelper.getSubtasksForTask(taskId)
        binding.subtasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.subtasksRecyclerView.adapter = SubtaskAdapter(subtasks) { subtask, isChecked ->
            databaseHelper.updateSubtaskStatus(subtask.id, isChecked)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.task_detail_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val saveMenuItem = menu?.findItem(R.id.action_save_task)
        saveMenuItem?.isVisible = hasUnsavedChanges
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save_task -> {
                saveChanges()
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (hasUnsavedChanges) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("Do you want to save the changes?")
                .setPositiveButton("Save") { _, _ -> saveChanges() }
                .setNegativeButton("Discard") { _, _ -> finish() }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            super.onBackPressed()
        }
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
