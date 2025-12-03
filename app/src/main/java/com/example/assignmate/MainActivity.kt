package com.example.assignmate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.assignmate.adapter.UpcomingTasksAdapter
import com.example.assignmate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var notificationHelper: NotificationHelper
    private var currentUserId: Int = -1

    companion object {
        private var dummyNotificationSent = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)
        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()

        currentUserId = intent.getIntExtra("USER_ID", -1)

        if (!dummyNotificationSent) {
            notificationHelper.sendNotification(currentUserId, "Welcome!", "This is a dummy notification to test the system.", 0)
            dummyNotificationSent = true
        }

        binding.notificationBell.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    // Already on the home screen, do nothing
                    true
                }
                R.id.action_groups -> {
                    val intent = Intent(this, GroupActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    true
                }
                R.id.action_create -> {
                    showCreateGroupDialog()
                    false
                }
                R.id.action_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUserInfo()
    }

    private fun updateUserInfo() {
        val userDetails = databaseHelper.getUserDetails(currentUserId)
        if (userDetails != null) {
            binding.welcomeMessage.text = "Welcome back, ${userDetails.first}!"
        } else {
            binding.welcomeMessage.text = "Welcome back, User!"
        }

        val groupCount = databaseHelper.getGroupsForUser(currentUserId).size
        binding.totalGroups.text = groupCount.toString()

        val totalTasks = databaseHelper.getTotalTasksForUser(currentUserId)
        binding.totalTasks.text = totalTasks.toString()

        val pendingTasks = databaseHelper.getPendingTasksForUser(currentUserId)
        binding.pendingTasks.text = pendingTasks.toString()

        val dueTasks = databaseHelper.getDueTasksForUser(currentUserId)
        binding.dueTasks.text = dueTasks.toString()

        val upcomingTasks = databaseHelper.getUpcomingTasksForUser(currentUserId)
        binding.upcomingDeadlinesRecyclerView.adapter = UpcomingTasksAdapter(upcomingTasks) { task ->
            val intent = Intent(this, TaskDetailActivity::class.java)
            intent.putExtra("TASK_ID", task.id)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }

        updateFavouriteGroup()
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        val unreadCount = databaseHelper.getUnreadNotificationCount(currentUserId)
        if (unreadCount > 0) {
            binding.notificationBadge.visibility = View.VISIBLE
            binding.notificationBadge.text = unreadCount.toString()
        } else {
            binding.notificationBadge.visibility = View.GONE
        }
    }

    private fun updateFavouriteGroup() {
        val favouriteGroupId = databaseHelper.getFavouriteGroup(currentUserId)
        if (favouriteGroupId != -1L) {
            val favouriteGroup = databaseHelper.getGroupsForUser(currentUserId).find { it.id == favouriteGroupId }
            if (favouriteGroup != null) {
                binding.favouriteGroupCard.visibility = View.VISIBLE
                binding.noFavouriteGroupText.visibility = View.GONE
                binding.favouriteGroupName.text = favouriteGroup.name
                binding.favouriteGroupDescription.text = favouriteGroup.description

                binding.favouriteGroupCard.setOnClickListener {
                    val intent = Intent(this, SingleGroupActivity::class.java)
                    intent.putExtra("GROUP_ID", favouriteGroup.id)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                }
            } else {
                binding.favouriteGroupCard.visibility = View.GONE
                binding.noFavouriteGroupText.visibility = View.VISIBLE
            }
        } else {
            binding.favouriteGroupCard.visibility = View.GONE
            binding.noFavouriteGroupText.visibility = View.VISIBLE
        }
    }

    private fun showCreateGroupDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_create_group, null)
        builder.setView(view)

        val groupNameInput = view.findViewById<EditText>(R.id.group_name_input)
        val groupDescriptionInput = view.findViewById<EditText>(R.id.group_description_input)
        val groupCodeText = view.findViewById<TextView>(R.id.group_code_text)
        val copyCodeButton = view.findViewById<ImageButton>(R.id.copy_code_button)
        val joinGroupInsteadButton = view.findViewById<TextView>(R.id.join_group_instead_button)

        val dialog = builder.create()

        groupNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty() && groupCodeText.text.contains("will appear")) {
                    val groupCode = generateGroupCode()
                    groupCodeText.text = "Group Code: $groupCode"
                    copyCodeButton.visibility = View.VISIBLE
                } else if (s.isNullOrEmpty()) {
                    groupCodeText.text = "Group Code will appear here"
                    copyCodeButton.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        copyCodeButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val code = groupCodeText.text.toString().substringAfter("Group Code: ")
            val clip = ClipData.newPlainText("Group Code", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Group code copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Create") { _, _ ->
            val groupName = groupNameInput.text.toString()
            val groupDescription = groupDescriptionInput.text.toString()
            val groupCode = groupCodeText.text.toString().substringAfter("Group Code: ")

            if (groupName.isNotEmpty() && groupDescription.isNotEmpty() && groupCode.length == 6) {
                val newGroupId = databaseHelper.createGroup(groupName, groupDescription, currentUserId, groupCode)
                if (newGroupId != -1L) {
                    Toast.makeText(this, "Group created successfully", Toast.LENGTH_SHORT).show()
                    notificationHelper.sendNotification(currentUserId, "New Group", "You have created a new group: $groupName", newGroupId.toInt())
                    val intent = Intent(this, SingleGroupActivity::class.java)
                    intent.putExtra("GROUP_ID", newGroupId)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Failed to create group. The code might already exist.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") { _, _ -> dialog.dismiss() }

        joinGroupInsteadButton.setOnClickListener {
            dialog.dismiss()
            showJoinGroupDialog()
        }

        dialog.show()
    }

    private fun showJoinGroupDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_join_group, null)
        builder.setView(view)

        val groupCodeInput = view.findViewById<EditText>(R.id.group_code_input)
        val createGroupInsteadButton = view.findViewById<TextView>(R.id.create_group_instead_button)

        val dialog = builder.create()

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Join") { _, _ ->
            val groupCode = groupCodeInput.text.toString()
            if (groupCode.isNotEmpty()) {
                val groupId = databaseHelper.getGroupIdByCode(groupCode)
                if (groupId != -1L) {
                    if (databaseHelper.joinGroup(currentUserId, groupCode)) {
                        Toast.makeText(this, "Group Joined Successfully", Toast.LENGTH_SHORT).show()
                        notificationHelper.sendNotification(currentUserId, "Joined Group", "You have joined a new group.", groupId.toInt())
                        val intent = Intent(this, SingleGroupActivity::class.java)
                        intent.putExtra("GROUP_ID", groupId)
                        intent.putExtra("USER_ID", currentUserId)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Failed to join group", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Invalid Group Code", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a group code", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") { _, _ -> dialog.dismiss() }

        createGroupInsteadButton.setOnClickListener {
            dialog.dismiss()
            showCreateGroupDialog()
        }

        dialog.show()
    }

    private fun generateGroupCode(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
