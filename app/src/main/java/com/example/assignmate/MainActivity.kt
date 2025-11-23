package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.assignmate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = intent.getIntExtra("USER_ID", -1)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val intent = when (item.itemId) {
                R.id.action_home -> {
                    // Already on the home screen
                    return@setOnItemSelectedListener true
                }
                R.id.action_groups -> Intent(this, GroupActivity::class.java)
                R.id.action_create -> Intent(this, CreateGroupActivity::class.java)
                // R.id.action_tasks -> Intent(this, TasksActivity::class.java)
                R.id.action_profile -> Intent(this, ProfileActivity::class.java)
                else -> null
            }
            intent?.let {
                it.putExtra("USER_ID", currentUserId)
                startActivity(it)
                // Do not finish MainActivity so user can return
            }
            true
        }
    }
}
