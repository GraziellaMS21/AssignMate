package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
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

        createGroupButton.setOnClickListener {
            val groupName = groupNameInput.text.toString()
            val groupDescription = groupDescriptionInput.text.toString()

            if (groupName.isNotEmpty() && groupDescription.isNotEmpty()) {
                val groupCode = databaseHelper.createGroup(groupName, groupDescription, currentUserId)
                Toast.makeText(this, "Group Created! Code: $groupCode", Toast.LENGTH_LONG).show()
                val intent = Intent(this, GroupActivity::class.java)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_create
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_home -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
                finish()
                return true
            }
            R.id.navigation_groups -> {
                val intent = Intent(this, GroupActivity::class.java)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
                finish()
                return true
            }
            R.id.navigation_create -> {
                // Already here
                return true
            }
            R.id.navigation_tasks -> {
                // You can create and navigate to a TasksActivity here
                Toast.makeText(this, "Tasks not implemented yet", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.navigation_profile -> {
                 // You can create and navigate to a ProfileActivity here
                Toast.makeText(this, "Profile not implemented yet", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return false
    }
}
