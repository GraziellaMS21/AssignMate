package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
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
            val intent = Intent(this, JoinGroupActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
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
        groupAdapter = GroupAdapter(groups) { group ->
            val intent = Intent(this, SingleGroupActivity::class.java)
            intent.putExtra("GROUP_NAME", group.name)
            intent.putExtra("GROUP_ID", group.id)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }
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
                val intent = Intent(this, CreateGroupActivity::class.java)
                intent.putExtra("USER_ID", currentUserId)
                startActivity(intent)
                finish()
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
