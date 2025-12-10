package com.example.assignmate

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.assignmate.adapter.ManageLabelsAdapter
import com.example.assignmate.adapter.NewSubtaskAdapter
import com.example.assignmate.adapter.SelectableLabelAdapter
import com.example.assignmate.databinding.ActivitySingleGroupBinding
import com.example.assignmate.model.Label
import com.example.assignmate.model.NewSubtask
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayoutMediator
import yuku.ambilwarna.AmbilWarnaDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SingleGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySingleGroupBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var notificationHelper: NotificationHelper
    private var groupId: Long = -1
    private var currentUserId: Int = -1
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private var defaultColor: Int = 0
    private var currentUserRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)
        notificationHelper = NotificationHelper(this)
        groupId = intent.getLongExtra("GROUP_ID", -1)
        currentUserId = intent.getIntExtra("USER_ID", -1)
        val groupName = intent.getStringExtra("GROUP_NAME")
        currentUserRole = databaseHelper.getRoleForUserInGroup(currentUserId, groupId)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = groupName

        if (currentUserRole == "leader" || currentUserRole == "co-leader") {
            binding.fabAddTaskButton.visibility = View.VISIBLE
        }

        binding.fabAddTaskButton.setOnClickListener {
            showCreateTaskDialog()
        }

        viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Group Tasks"
                1 -> "Members"
                else -> null
            }
        }.attach()

        setupFilter()
    }

    private fun setupFilter() {
        val filterOptions = arrayOf("All", "By Assignee", "By Label")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, filterOptions)
        binding.filterDropdown.setAdapter(adapter)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTasks()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.searchInput.addTextChangedListener(textWatcher)
        binding.filterDropdown.setOnItemClickListener { _, _, _, _ -> filterTasks() }
    }

    private fun filterTasks() {
        val fragment = supportFragmentManager.findFragmentByTag("f0")
        if (fragment is GroupTasksFragment) {
            val filterType = binding.filterDropdown.text.toString()
            val searchQuery = binding.searchInput.text.toString()
            fragment.filterTasks(filterType, searchQuery)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.single_group_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val favouriteMenuItem = menu?.findItem(R.id.action_add_to_favourite)
        if (databaseHelper.isGroupFavourite(currentUserId, groupId)) {
            favouriteMenuItem?.title = "Remove from Favourites"
        } else {
            favouriteMenuItem?.title = "Add to Favourites"
        }

        val canManageGroup = currentUserRole == "leader" || currentUserRole == "co-leader"
        menu?.findItem(R.id.action_edit_group)?.isVisible = canManageGroup
        menu?.findItem(R.id.action_delete_group)?.isVisible = canManageGroup
        menu?.findItem(R.id.action_manage_labels)?.isVisible = canManageGroup
        menu?.findItem(R.id.action_add_members)?.isVisible = canManageGroup

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_add_to_favourite -> toggleFavourite()
            R.id.action_edit_group -> showEditGroupDialog()
            R.id.action_delete_group -> showDeleteGroupDialog()
            R.id.action_manage_labels -> showManageLabelsDialog()
            R.id.action_add_members -> showAddMembersDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun toggleFavourite() {
        if (databaseHelper.isGroupFavourite(currentUserId, groupId)) {
            databaseHelper.removeFavouriteGroup(currentUserId, groupId)
            Toast.makeText(this, "Group removed from favorites", Toast.LENGTH_SHORT).show()
        } else {
            databaseHelper.addFavouriteGroup(currentUserId, groupId)
            Toast.makeText(this, "Group added to favorites", Toast.LENGTH_SHORT).show()
        }
        invalidateOptionsMenu()
    }

    private fun showEditGroupDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Group")

        val view = layoutInflater.inflate(R.layout.dialog_create_group, null)
        builder.setView(view)

        val groupNameInput = view.findViewById<EditText>(R.id.group_name_input)
        val groupDescriptionInput = view.findViewById<EditText>(R.id.group_description_input)

        val group = databaseHelper.getGroup(groupId)
        groupNameInput.setText(group?.name)
        groupDescriptionInput.setText(group?.description)

        builder.setPositiveButton("Save") { dialog, _ ->
            val newGroupName = groupNameInput.text.toString()
            val newGroupDescription = groupDescriptionInput.text.toString()
            if (newGroupName.isNotEmpty()) {
                if (databaseHelper.updateGroup(groupId, newGroupName, newGroupDescription)) {
                    Toast.makeText(this, "Group updated successfully", Toast.LENGTH_SHORT).show()
                    supportActionBar?.title = newGroupName
                } else {
                    Toast.makeText(this, "Failed to update group", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun showDeleteGroupDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Group")
            .setMessage("Are you sure you want to delete this group?")
            .setPositiveButton("Delete") { _, _ ->
                if (databaseHelper.deleteGroup(groupId)) {
                    Toast.makeText(this, "Group deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to delete group", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showManageLabelsDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_manage_labels, null)
        builder.setView(view)

        val labelsRecyclerView = view.findViewById<RecyclerView>(R.id.labels_recycler_view)
        val createNewLabelButton = view.findViewById<View>(R.id.create_new_label_button)

        val labels = databaseHelper.getAllLabels().toMutableList()
        val adapter = ManageLabelsAdapter(labels) { label ->
            showEditLabelDialog(label) { updatedLabel ->
                (labelsRecyclerView.adapter as ManageLabelsAdapter).updateLabel(updatedLabel)
            }
        }
        labelsRecyclerView.layoutManager = LinearLayoutManager(this)
        labelsRecyclerView.adapter = adapter

        createNewLabelButton.setOnClickListener {
            showEditLabelDialog(null) { newLabel ->
                (labelsRecyclerView.adapter as ManageLabelsAdapter).addLabel(newLabel)
            }
        }

        builder.setPositiveButton("Done", null)
        builder.show()
    }

    private fun showEditLabelDialog(label: Label?, onLabelUpdated: (Label) -> Unit) {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_label, null)
        builder.setView(view)

        val labelNameInput = view.findViewById<EditText>(R.id.label_name_input)
        val colorPicker = view.findViewById<View>(R.id.color_picker)

        if (label != null) {
            labelNameInput.setText(label.name)
            defaultColor = Color.parseColor(label.color)
            colorPicker?.setBackgroundColor(defaultColor)
        } else {
            // Set a default color for new labels to orange
            defaultColor = Color.parseColor("#F28A30")
            colorPicker?.setBackgroundColor(defaultColor)
        }

        colorPicker?.setOnClickListener {
            val colorPickerDialog = AmbilWarnaDialog(this, defaultColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog?) {}
                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    defaultColor = color
                    colorPicker?.setBackgroundColor(color)
                }
            })
            colorPickerDialog.show()
        }

        builder.setPositiveButton(if (label == null) "Create" else "Save") { _, _ ->
            val labelName = labelNameInput.text.toString()
            val labelColor = String.format("#%06X", 0xFFFFFF and defaultColor)

            if (labelName.isNotEmpty()) {
                if (label == null) {
                    val newLabelId = databaseHelper.addLabel(labelName, labelColor)
                    if (newLabelId != -1L) {
                        onLabelUpdated(Label(newLabelId, labelName, labelColor))
                    } else {
                        Toast.makeText(this, "Failed to create label", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (databaseHelper.updateLabel(label.id, labelName, labelColor)) {
                        onLabelUpdated(Label(label.id, labelName, labelColor))
                    } else {
                        Toast.makeText(this, "Failed to update label", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter a label name", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showAddMembersDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_member, null)
        builder.setView(view)

        val groupCode = databaseHelper.getGroup(groupId)?.code
        val groupCodeText = view.findViewById<android.widget.TextView>(R.id.group_code_text)
        groupCodeText.text = "Group Code: $groupCode"

        view.findViewById<View>(R.id.copy_icon).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Group Code", groupCode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Group code copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        val emailInput = view.findViewById<EditText>(R.id.email_input)

        builder.setPositiveButton("Add") { dialog, _ ->
            val email = emailInput.text.toString()
            if (email.isNotEmpty()) {
                val newMemberId = databaseHelper.getUserId(email)
                if (newMemberId != -1) {
                    if (databaseHelper.addMemberToGroup(newMemberId, groupId)) {
                        Toast.makeText(this, "Member added successfully", Toast.LENGTH_SHORT).show()
                        notificationHelper.sendNotification(newMemberId, "New Group Member", "You have been added to a new group.", groupId.toInt())
                        val membersFragment = supportFragmentManager.findFragmentByTag("f1") as? MembersFragment
                        membersFragment?.loadMembers()
                    } else {
                        Toast.makeText(this, "Member is already in the group", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun showCreateTaskDialog() {
        Log.d("CreateTaskDialog", "showCreateTaskDialog called")
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_create_task, null)
        builder.setView(view)

        // View references
        val taskNameInput = view.findViewById<EditText>(R.id.task_name_input)
        val taskDescriptionInput = view.findViewById<EditText>(R.id.task_description_input)
        val dueDateInput = view.findViewById<EditText>(R.id.due_date_input)
        val assignToLayout = view.findViewById<View>(R.id.assign_to_layout)
        val assignedMembersChipGroup = view.findViewById<ChipGroup>(R.id.assigned_members_chip_group)
        val addLabelLayout = view.findViewById<View>(R.id.add_label_layout)
        val labelsChipGroup = view.findViewById<ChipGroup>(R.id.labels_chip_group)
        val subtasksRecyclerView = view.findViewById<RecyclerView>(R.id.subtasks_recycler_view)
        val addSubtaskButton = view.findViewById<View>(R.id.add_subtask_button)

        // Data holders
        var dueDateMillis: Long = 0
        val assignedTo = mutableListOf<Int>()
        val selectedLabelIds = mutableSetOf<Long>()
        val subtasks = mutableListOf<NewSubtask>()

        // Setup Subtasks
        val subtaskAdapter = NewSubtaskAdapter(subtasks)
        subtasksRecyclerView.layoutManager = LinearLayoutManager(this)
        subtasksRecyclerView.adapter = subtaskAdapter

        // Setup Listeners
        dueDateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            if (dueDateMillis != 0L) {
                calendar.timeInMillis = dueDateMillis
            }
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val newDueDateCalendar = Calendar.getInstance()
                newDueDateCalendar.set(selectedYear, selectedMonth, selectedDay)
                dueDateMillis = newDueDateCalendar.timeInMillis
                dueDateInput.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dueDateMillis))
            }, year, month, day)

            datePickerDialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, "Clear") { _, _ ->
                dueDateMillis = 0L
                dueDateInput.setText("")
            }
            datePickerDialog.show()
        }

        assignToLayout.setOnClickListener {
            val members = databaseHelper.getGroupMembers(groupId)
            val memberNames = members.map { it.name }.toTypedArray()
            val selectedItems = BooleanArray(memberNames.size) { i -> members[i].id in assignedTo }

            AlertDialog.Builder(this)
                .setTitle("Assign Members")
                .setMultiChoiceItems(memberNames, selectedItems) { _, which, isChecked ->
                    val memberId = members[which].id
                    if (isChecked) {
                        if (memberId !in assignedTo) assignedTo.add(memberId)
                    } else {
                        assignedTo.remove(memberId)
                    }
                }
                .setPositiveButton("OK") { _, _ ->
                    assignedMembersChipGroup.removeAllViews()
                    val assignedMembers = members.filter { it.id in assignedTo }
                    for (member in assignedMembers) {
                        val chip = Chip(this)
                        chip.text = member.name
                        assignedMembersChipGroup.addView(chip)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        addLabelLayout.setOnClickListener {
            val allLabels = databaseHelper.getAllLabels()
            val dialogView = layoutInflater.inflate(R.layout.dialog_select_labels, null)
            val labelsRecyclerViewDialog = dialogView.findViewById<RecyclerView>(R.id.labels_recycler_view)
            labelsRecyclerViewDialog.layoutManager = LinearLayoutManager(this)

            // Create a copy of the selected IDs to handle cancellations correctly
            val tempSelectedLabelIds = selectedLabelIds.toMutableSet()
            val adapter = SelectableLabelAdapter(allLabels, tempSelectedLabelIds)
            labelsRecyclerViewDialog.adapter = adapter

            AlertDialog.Builder(this)
                .setTitle("Select Labels")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    // Update the actual selected IDs set
                    selectedLabelIds.clear()
                    selectedLabelIds.addAll(tempSelectedLabelIds)

                    // Update the UI to show the selected labels as chips
                    labelsChipGroup.removeAllViews()
                    val selectedLabels = allLabels.filter { it.id in selectedLabelIds }
                    for (label in selectedLabels) {
                        val chip = Chip(this)
                        chip.text = label.name
                        try {
                            val color = Color.parseColor(label.color)
                            chip.chipBackgroundColor = ColorStateList.valueOf(color)
                        } catch (e: IllegalArgumentException) {
                            Log.w("CreateTaskDialog", "Invalid color for label: ${label.name}")
                        }
                        labelsChipGroup.addView(chip)
                    }
                }
                .setNegativeButton("Cancel", null) // On cancel, changes to tempSelectedLabelIds are discarded
                .show()
        }

        addSubtaskButton.setOnClickListener {
            val subtaskBuilder = AlertDialog.Builder(this)
            val subtaskView = layoutInflater.inflate(R.layout.dialog_add_subtask, null)
            subtaskBuilder.setView(subtaskView)
            val subtaskNameInput = subtaskView.findViewById<EditText>(R.id.subtask_name_input)

            subtaskBuilder.setPositiveButton("Add") { _, _ ->
                val subtaskName = subtaskNameInput.text.toString()
                if (subtaskName.isNotEmpty()) {
                    val newSubtask = NewSubtask(name = subtaskName)
                    subtaskAdapter.addSubtask(newSubtask)
                }
            }
                .setNegativeButton("Cancel", null)
                .show()
        }

        builder.setPositiveButton("Create") { _, _ ->
            val taskName = taskNameInput.text.toString()
            if (taskName.isEmpty()) {
                Toast.makeText(this, "Task title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val taskDescription = taskDescriptionInput.text.toString()
            val finalDueDate = if (dueDateMillis == 0L) null else dueDateMillis

            val newTaskId = databaseHelper.createTask(taskName, taskDescription, groupId, finalDueDate)

            if (newTaskId != -1L) {
                // Assign members
                assignedTo.forEach { memberId ->
                    databaseHelper.assignTaskToUser(newTaskId, memberId)
                    notificationHelper.sendNotification(memberId, "Task Assigned", "You have been assigned a new task: $taskName", newTaskId.toInt())
                }

                // Add labels
                if (selectedLabelIds.isNotEmpty()) {
                    databaseHelper.updateTaskLabels(newTaskId, selectedLabelIds)
                }

                // Add subtasks
                subtasks.forEach { subtask ->
                    databaseHelper.createSubtask(newTaskId, subtask.name)
                }

                Toast.makeText(this, "Task created successfully", Toast.LENGTH_SHORT).show()
                val tasksFragment = supportFragmentManager.findFragmentByTag("f0") as? GroupTasksFragment
                tasksFragment?.refreshTasks()

            } else {
                Toast.makeText(this, "Failed to create task", Toast.LENGTH_SHORT).show()
            }
        }
            .setNegativeButton("Cancel", null)

        builder.create().show()
        Log.d("CreateTaskDialog", "Dialog shown")
    }

    inner class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GroupTasksFragment.newInstance(groupId)
                1 -> MembersFragment.newInstance(groupId, currentUserId)
                else -> throw IllegalStateException("Invalid position")
            }
        }
    }
}
