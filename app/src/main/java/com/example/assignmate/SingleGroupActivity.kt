package com.example.assignmate

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.assignmate.databinding.ActivitySingleGroupBinding
import com.google.android.material.tabs.TabLayoutMediator
import java.util.Calendar

class SingleGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySingleGroupBinding
    private lateinit var databaseHelper: DatabaseHelper
    private var groupId: Long = -1
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)
        groupId = intent.getLongExtra("GROUP_ID", -1)
        currentUserId = intent.getIntExtra("USER_ID", -1)
        val groupName = intent.getStringExtra("GROUP_NAME")
        binding.groupNameHeader.text = groupName

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.addMemberButton.setOnClickListener {
            showAddMemberDialog()
        }

        if (databaseHelper.getGroupLeaderId(groupId) == currentUserId) {
            binding.fabAddTask.visibility = View.VISIBLE
        }

        binding.fabAddTask.setOnClickListener {
            showCreateTaskDialog()
        }

        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Group Tasks"
                1 -> "Members"
                else -> null
            }
        }.attach()
    }

    private fun showAddMemberDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Member")

        val input = EditText(this)
        input.hint = "Enter user email"
        builder.setView(input)

        builder.setPositiveButton("Add") { dialog, _ ->
            val email = input.text.toString()
            if (email.isNotEmpty()) {
                val newMemberId = databaseHelper.getUserId(email)
                if (newMemberId != -1) {
                    if (databaseHelper.addMemberToGroup(newMemberId, groupId)) {
                        Toast.makeText(this, "Member added successfully", Toast.LENGTH_SHORT).show()
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
        builder.setTitle("Create Task")

        val view = layoutInflater.inflate(R.layout.dialog_create_task, null)
        builder.setView(view)

        val taskNameInput = view.findViewById<EditText>(R.id.task_name_input)
        val taskDescriptionInput = view.findViewById<EditText>(R.id.task_description_input)
        val dueDateInput = view.findViewById<EditText>(R.id.due_date_input)

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
                    showAssignTaskDialog(newTaskId)
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

    private fun showAssignTaskDialog(taskId: Long) {
        val members = databaseHelper.getGroupMembers(groupId)
        val memberNames = members.map { it.second }.toTypedArray()
        val selectedMembers = BooleanArray(memberNames.size)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Assign Task to Members")
        builder.setMultiChoiceItems(memberNames, selectedMembers) { _, which, isChecked ->
            selectedMembers[which] = isChecked
        }

        builder.setPositiveButton("Assign") { dialog, _ ->
            for (i in selectedMembers.indices) {
                if (selectedMembers[i]) {
                    databaseHelper.assignTaskToUser(taskId, members[i].first)
                }
            }
            Toast.makeText(this, "Task assigned successfully", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private inner class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GroupTasksFragment.newInstance(groupId)
                1 -> MembersFragment.newInstance(groupId)
                else -> throw IllegalStateException("Invalid position")
            }
        }
    }
}
