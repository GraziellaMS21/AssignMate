package com.example.assignmate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignmate.adapter.GroupAdapter
import com.example.assignmate.databinding.ActivityGroupBinding
import com.example.assignmate.model.Group
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityGroupBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var groupAdapter: GroupAdapter
    private val groups = mutableListOf<Group>()
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)
        currentUserId = intent.getIntExtra("USER_ID", -1)

        setupRecyclerView()

        binding.addGroupButton.setOnClickListener {
            showJoinGroupDialog()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.action_groups
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
    }

    override fun onResume() {
        super.onResume()
        loadGroups()
    }

    private fun setupRecyclerView() {
        groupAdapter = GroupAdapter(groups,
            onGroupClicked = { group ->
                val intent = Intent(this, SingleGroupActivity::class.java)
                intent.putExtra("GROUP_NAME", group.name)
                intent.putExtra("GROUP_ID", group.id)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
            },
            onEditClicked = { group ->
                showEditGroupDialog(group)
            },
            onDeleteClicked = { group ->
                showDeleteGroupConfirmationDialog(group)
            },
            onAddToFavouriteClicked = { group ->
                databaseHelper.setFavouriteGroup(currentUserId, group.id)
                Toast.makeText(this, "\"${group.name}\" has been set as your favourite group", Toast.LENGTH_SHORT).show()
            }
        )
        binding.groupsRecyclerView.apply {
            adapter = groupAdapter
            layoutManager = LinearLayoutManager(this@GroupActivity)
        }
    }

    private fun loadGroups() {
        lifecycleScope.launch(Dispatchers.IO) {
            val userGroups = databaseHelper.getGroupsForUser(currentUserId)
            withContext(Dispatchers.Main) {
                groups.clear()
                groups.addAll(userGroups)
                groupAdapter.notifyDataSetChanged()
                updateUI()
            }
        }
    }

    private fun updateUI() {
        if (groups.isEmpty()) {
            binding.groupsRecyclerView.visibility = View.GONE
            binding.noGroupsLayout.visibility = View.VISIBLE
        } else {
            binding.groupsRecyclerView.visibility = View.VISIBLE
            binding.noGroupsLayout.visibility = View.GONE
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

    private fun showEditGroupDialog(group: Group) {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_create_group, null)
        builder.setView(view)

        val groupNameInput = view.findViewById<EditText>(R.id.group_name_input)
        val groupDescriptionInput = view.findViewById<EditText>(R.id.group_description_input)
        val groupCodeText = view.findViewById<TextView>(R.id.group_code_text)
        val copyCodeButton = view.findViewById<ImageButton>(R.id.copy_code_button)
        val joinGroupInsteadButton = view.findViewById<TextView>(R.id.join_group_instead_button)

        groupNameInput.setText(group.name)
        groupDescriptionInput.setText(group.description)
        groupCodeText.text = "Group Code: ${group.code}"
        copyCodeButton.visibility = View.VISIBLE
        joinGroupInsteadButton.visibility = View.GONE

        val dialog = builder.create()

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Save Changes") { _, _ ->
            val newGroupName = groupNameInput.text.toString()
            val newGroupDescription = groupDescriptionInput.text.toString()

            if (newGroupName.isNotEmpty() && newGroupDescription.isNotEmpty()) {
                if (databaseHelper.updateGroup(group.id, newGroupName, newGroupDescription)) {
                    Toast.makeText(this, "Group updated successfully", Toast.LENGTH_SHORT).show()
                    loadGroups()
                } else {
                    Toast.makeText(this, "Failed to update group", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") { _, _ -> dialog.dismiss() }

        dialog.show()
    }

    private fun showDeleteGroupConfirmationDialog(group: Group) {
        AlertDialog.Builder(this)
            .setTitle("Delete Group")
            .setMessage("Are you sure you want to delete \"${group.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                if (databaseHelper.deleteGroup(group.id)) {
                    Toast.makeText(this, "Group deleted", Toast.LENGTH_SHORT).show()
                    loadGroups()
                } else {
                    Toast.makeText(this, "Failed to delete group", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateGroupCode(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_home -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
                finish()
                return true
            }
            R.id.action_groups -> {
                // Already here
                return true
            }
            R.id.action_create -> {
                showCreateGroupDialog()
                return false
            }
            R.id.action_tasks -> {
                Toast.makeText(this, "Tasks not implemented yet", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.action_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
                finish()
                return true
            }
        }
        return false
    }
}
