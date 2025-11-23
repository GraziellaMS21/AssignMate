package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class ProfileActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        currentUserId = intent.getIntExtra("USER_ID", -1)

        val databaseHelper = DatabaseHelper(this)
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
        val intent = when (item.itemId) {
            R.id.action_home -> Intent(this, MainActivity::class.java)
            R.id.action_groups -> Intent(this, GroupActivity::class.java)
            R.id.action_create -> Intent(this, CreateGroupActivity::class.java)
            // R.id.action_tasks -> Intent(this, TasksActivity::class.java)
            // R.id.action_profile -> return true // Already on this screen
            else -> return false
        }
        intent.putExtra("USER_ID", currentUserId)
        startActivity(intent)
        // finish() // Optional: uncomment if you want to close the current activity
        return true
    }
}
