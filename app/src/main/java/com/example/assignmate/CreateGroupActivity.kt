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
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class CreateGroupActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var databaseHelper: DatabaseHelper
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        databaseHelper = DatabaseHelper(this)
        currentUserId = intent.getIntExtra("USER_ID", -1)

        val groupNameInput = findViewById<EditText>(R.id.group_name_input)
        val groupDescriptionInput = findViewById<EditText>(R.id.group_description_input)
        val createGroupButton = findViewById<Button>(R.id.create_group_button)
        val groupCodeText = findViewById<TextView>(R.id.group_code_text)
        val copyCodeButton = findViewById<ImageButton>(R.id.copy_code_button)
        val joinGroupInsteadButton = findViewById<TextView>(R.id.join_group_instead_button)

        groupNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty() && groupCodeText.text.length != 6) {
                    groupCodeText.text = generateGroupCode()
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
            val clip = ClipData.newPlainText("Group Code", groupCodeText.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Group code copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        createGroupButton.setOnClickListener {
            val groupName = groupNameInput.text.toString()
            val groupDescription = groupDescriptionInput.text.toString()
            val groupCode = groupCodeText.text.toString()

            if (groupName.isNotEmpty() && groupDescription.isNotEmpty() && groupCode.length == 6) {
                val newGroupId = databaseHelper.createGroup(groupName, groupDescription, currentUserId, groupCode)
                if (newGroupId != -1L) {
                    Toast.makeText(this, "Group created successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, GroupActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to create group. The code might already exist.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            }
        }

        joinGroupInsteadButton.setOnClickListener {
            val intent = Intent(this, JoinGroupActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
            finish()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.action_create
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
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
                val intent = Intent(this, GroupActivity::class.java)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
                finish()
                return true
            }
            R.id.action_create -> {
                // Already here
                return true
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
