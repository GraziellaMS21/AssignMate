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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private var currentUserId: Int = -1
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        databaseHelper = DatabaseHelper(this)
        currentUserId = intent.getIntExtra("USER_ID", -1)

        val userDetails = databaseHelper.getUserDetails(currentUserId)

        val profileName = findViewById<TextView>(R.id.profile_name)
        val profileEmail = findViewById<TextView>(R.id.profile_email)

        if (userDetails != null) {
            profileName.text = userDetails.first
            profileEmail.text = userDetails.second
        } else {
            profileName.text = "User Not Found"
            profileEmail.text = ""
        }

        val signOutButton = findViewById<Button>(R.id.sign_out_button)
        signOutButton.setOnClickListener {
            // Clear any user session data if necessary (e.g., in SharedPreferences)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.selectedItemId = R.id.action_profile
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_profile) {
            return true
        }

        if (item.itemId == R.id.action_create) {
            showCreateGroupDialog()
            return false
        }

        val intent = when (item.itemId) {
            R.id.action_home -> Intent(this, MainActivity::class.java)
            R.id.action_groups -> Intent(this, GroupActivity::class.java)
            else -> null
        }
        intent?.putExtra("USER_ID", currentUserId)
        startActivity(intent)
        return true
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

    private fun generateGroupCode(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..6)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
