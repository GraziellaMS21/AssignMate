package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignmate.MainActivity
import com.example.assignmate.databinding.ActivityTaskBinding
import com.example.assignmate.model.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskBinding

    // Firebase Setup
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Check Authentication
        currentUserId = auth.currentUser?.uid ?: ""
        if (currentUserId.isEmpty()) {
            finish() // Or redirect to Login
            return
        }

        setupBottomNavigation()
        loadTasks()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.action_tasks

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.action_groups -> {
                    startActivity(Intent(this, GroupActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.action_tasks -> true
                R.id.action_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadTasks() {
        // 1. Get the list of groups the user belongs to
        db.child("Users").child(currentUserId).child("groups").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userGroupsSnap: DataSnapshot) {
                val myGroupIds = userGroupsSnap.children.mapNotNull { it.key }

                if (myGroupIds.isEmpty()) {
                    showEmptyState()
                    return
                }

                // 2. Fetch all tasks and filter for those groups
                fetchTasksForGroups(myGroupIds)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchTasksForGroups(groupIds: List<String>) {
        db.child("Tasks").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val taskList = mutableListOf<Task>()

                for (child in snapshot.children) {
                    val task = child.getValue(Task::class.java)
                    // Filter: Only show tasks that belong to the user's groups
                    if (task != null && groupIds.contains(task.groupId)) {
                        taskList.add(task)
                    }
                }

                if (taskList.isEmpty()) {
                    showEmptyState()
                } else {
                    setupRecyclerView(taskList)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupRecyclerView(tasks: List<Task>) {
        // Hide empty state if visible
        // binding.noTasksLayout.visibility = View.GONE (If you have this view)

        // Group tasks by "Group Name" for the adapter
        val tasksByGroup = tasks.groupBy { it.groupName }

        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@TaskActivity)
            // Use the GroupedTaskAdapter we created earlier
            adapter = GroupedTaskAdapter(tasksByGroup, currentUserId)
        }
    }

    private fun showEmptyState() {
        // Optional: Show a "No Tasks" text view if you have one
    }
}