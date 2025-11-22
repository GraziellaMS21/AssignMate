package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class JoinGroupActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var databaseHelper: DatabaseHelper
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_group)

        databaseHelper = DatabaseHelper(this)
        currentUserId = intent.getIntExtra("USER_ID", -1)

        val groupCodeInput = findViewById<EditText>(R.id.group_code_input)
        val joinGroupButton = findViewById<Button>(R.id.join_group_button)
        val createGroupInsteadButton = findViewById<Button>(R.id.create_group_instead_button)

        joinGroupButton.setOnClickListener {
            val groupCode = groupCodeInput.text.toString()
            if (groupCode.isNotEmpty()) {
                if (databaseHelper.joinGroup(currentUserId, groupCode)) {
                    Toast.makeText(this, "Group Joined Successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, GroupActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Invalid Group Code", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a group code", Toast.LENGTH_SHORT).show()
            }
        }

        createGroupInsteadButton.setOnClickListener {
            val intent = Intent(this, CreateGroupActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
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
                val intent = Intent(this, CreateGroupActivity::class.java)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
                finish()
                return true
            }
            R.id.navigation_tasks -> {
                Toast.makeText(this, "Tasks not implemented yet", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.navigation_profile -> {
                Toast.makeText(this, "Profile not implemented yet", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return false
    }
}
