package com.example.assignmate

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import com.example.assignmate.databinding.ActivitySingleGroupBinding
import com.example.assignmate.model.Label
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayoutMediator
import yuku.ambilwarna.AmbilWarnaDialog
import java.util.Calendar

class SingleGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySingleGroupBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var notificationHelper: NotificationHelper
    private var groupId: Long = -1
    private var currentUserId: Int = -1
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private var defaultColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)
        notificationHelper = NotificationHelper(this)
        groupId = intent.getLongExtra("GROUP_ID", -1)
        currentUserId = intent.getIntExtra("USER_ID", -1)
        val groupName = intent.getStringExtra("GROUP_NAME")

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = groupName

        if (databaseHelper.getGroupLeaderId(groupId) == currentUserId) {
            binding.fabAddTask.visibility = View.VISIBLE
        }

        binding.fabAddTask.setOnClickListener {
            showCreateTaskDialog()
        }

        viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Group Tasks"
                1 -> "Members"
                else -> null
            }
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.single_group_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_add_to_favourite -> addToFavourite()
            R.id.action_edit_group -> showEditGroupDialog()
            R.id.action_delete_group -> showDeleteGroupDialog()
            R.id.action_manage_labels -> showManageLabelsDialog()
            R.id.action_add_members -> showAddMembersDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun addToFavourite() {
        databaseHelper.setFavouriteGroup(currentUserId, groupId)
        Toast.makeText(this, "Group added to favorites", Toast.LENGTH_SHORT).show()
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
            if (newGroupName.isNotEmpty() && newGroupDescription.isNotEmpty()) {
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
            defaultColor = android.graphics.Color.parseColor(label.color)
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
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_create_task, null)
        builder.setView(view)

        val taskNameInput = view.findViewById<EditText>(R.id.task_name_input)
        val taskDescriptionInput = view.findViewById<EditText>(R.id.task_description_input)
        val dueDateInput = view.findViewById<EditText>(R.id.due_date_input)
        val assignedMembersChipGroup = view.findViewById<ChipGroup>(R.id.assigned_members_chip_group)
        val editAssignmentsButton = view.findViewById<View>(R.id.edit_assignments_button)

        val members = databaseHelper.getGroupMembers(groupId)
        val memberNames = members.map { it.name }.toTypedArray()
        val selectedMembers = BooleanArray(memberNames.size)
        val assignedTo = mutableListOf<Int>()

        dueDateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, {
                _, selectedYear, selectedMonth, selectedDay ->
                dueDateInput.setText("$selectedDay/${selectedMonth + 1}/$selectedYear")
            }, year, month, day)
            datePickerDialog.show()
        }

        editAssignmentsButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Assign Members")
                .setMultiChoiceItems(memberNames, selectedMembers) { _, which, isChecked ->
                    selectedMembers[which] = isChecked
                }
                .setPositiveButton("OK") { _, _ ->
                    assignedTo.clear()
                    assignedMembersChipGroup.removeAllViews()
                    for (i in selectedMembers.indices) {
                        if (selectedMembers[i]) {
                            assignedTo.add(members[i].id)
                            val chip = Chip(this)
                            chip.text = members[i].name
                            assignedMembersChipGroup.addView(chip)
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        builder.setPositiveButton("Create") { dialog, _ ->
            val taskName = taskNameInput.text.toString()
            val taskDescription = taskDescriptionInput.text.toString()
            val dueDate = dueDateInput.text.toString()

            if (taskName.isNotEmpty() && taskDescription.isNotEmpty() && dueDate.isNotEmpty()) {
                val calendar = Calendar.getInstance()
                val dateParts = dueDate.split("/")
                calendar.set(dateParts[2].toInt(), dateParts[1].toInt() - 1, dateParts[0].toInt())
                val dueDateMillis = calendar.timeInMillis

                val newTaskId = databaseHelper.createTask(taskName, taskDescription, groupId, dueDateMillis)
                if (newTaskId != -1L) {
                    notificationHelper.sendNotification(currentUserId, "New Task", "A new task has been created: $taskName", newTaskId.toInt())
                    assignedTo.forEach { 
                        databaseHelper.assignTaskToUser(newTaskId, it)
                        notificationHelper.sendNotification(it, "Task Assigned", "You have been assigned a new task: $taskName", newTaskId.toInt())
                    }
                    Toast.makeText(this, "Task created successfully", Toast.LENGTH_SHORT).show()
                    (viewPagerAdapter.getFragment(0) as? GroupTasksFragment)?.refreshTasks()
                } else {
                    Toast.makeText(this, "Failed to create task", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    inner class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        private val fragments = mutableMapOf<Int, Fragment>()

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            val fragment = when (position) {
                0 -> GroupTasksFragment.newInstance(groupId)
                1 -> MembersFragment.newInstance(groupId, currentUserId)
                else -> throw IllegalStateException("Invalid position")
            }
            fragments[position] = fragment
            return fragment
        }

        fun getFragment(position: Int): Fragment? = fragments[position]
    }
}
