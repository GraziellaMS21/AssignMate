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
            var intent: Intent? = null
            when (item.itemId) {
                R.id.action_home -> {
                    // Already on the home screen, do nothing
                }
                R.id.action_groups -> {
                    intent = Intent(this, GroupActivity::class.java)
                }
                R.id.action_create -> {
                    intent = Intent(this, CreateGroupActivity::class.java)
                }
                R.id.action_profile -> {
                    intent = Intent(this, ProfileActivity::class.java)
                }
            }

            intent?.let {
                it.putExtra("USER_ID", currentUserId)
                startActivity(it)
            }

            true
        }
    }
}
