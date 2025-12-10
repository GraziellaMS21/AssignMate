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
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignmate.adapter.GroupAdapter
import com.example.assignmate.databinding.ActivityGroupBinding
import com.example.assignmate.model.Group
import com.example.assignmate.MainActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class GroupActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityGroupBinding
    private lateinit var groupAdapter: GroupAdapter
    private val groups = mutableListOf<Group>()

    // Firebase Setup
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = auth.currentUser?.uid ?: ""
        if (currentUserId.isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupRecyclerView()
        setupFilterAndSort()

        binding.addGroupButton.setOnClickListener {
            showJoinGroupDialog()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.action_groups
        bottomNavigationView.setOnNavigationItemSelectedListener(this)

        val notificationBell = findViewById<ImageView>(R.id.notification_bell)
        notificationBell.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }

        loadGroups()
        listenForNotifications()
    }

    private fun setupRecyclerView() {
        groupAdapter = GroupAdapter(groups, currentUserId,
            onGroupClicked = { group ->
                val intent = Intent(this, SingleGroupActivity::class.java)
                intent.putExtra("GROUP_NAME", group.name)
                intent.putExtra("GROUP_ID", group.groupId)
                startActivity(intent)
            },
            onEditClicked = { group ->
                showEditGroupDialog(group)
            },
            onDeleteClicked = { group ->
                showDeleteGroupConfirmationDialog(group)
            }
        )
        binding.groupsRecyclerView.apply {
            adapter = groupAdapter
            layoutManager = LinearLayoutManager(this@GroupActivity)
        }
    }

    private fun loadGroups() {
        val selection = binding.filterDropdown.text.toString()

        db.child("Users").child(currentUserId).child("groups").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupIds = snapshot.children.mapNotNull { it.key }
                fetchGroupDetails(groupIds, selection)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchGroupDetails(groupIds: List<String>, sortOption: String) {
        val tempGroups = mutableListOf<Group>()

        if (groupIds.isEmpty()) {
            groups.clear()
            updateUI()
            return
        }

        val groupCount = groupIds.size
        var fetchedCount = 0

        for (id in groupIds) {
            db.child("Groups").child(id).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val group = snapshot.getValue(Group::class.java)
                    if (group != null) {
                        if (tempGroups.none { it.groupId == group.groupId }) {
                            tempGroups.add(group)
                        }
                    }
                    fetchedCount++
                    if (fetchedCount == groupCount) {
                        sortAndDisplay(tempGroups, sortOption)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    fetchedCount++
                    if (fetchedCount == groupCount) {
                        sortAndDisplay(tempGroups, sortOption)
                    }
                }
            })
        }
    }

    private fun sortAndDisplay(list: MutableList<Group>, sortOption: String) {
        when (sortOption) {
            "Date Created" -> list.sortByDescending { it.groupId }
            else -> list.sortByDescending { it.name }
        }

        groups.clear()
        groups.addAll(list)

        updateUI()
    }

    private fun updateUI() {
        if (groups.isEmpty()) {
            binding.groupsRecyclerView.visibility = View.GONE
            binding.noGroupsLayout.visibility = View.VISIBLE
        } else {
            binding.groupsRecyclerView.visibility = View.VISIBLE
            binding.noGroupsLayout.visibility = View.GONE
        }
        groupAdapter.notifyDataSetChanged()
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

        //TODO: Group joining by code is broken as 'code' is not in the Group model.
        groupCodeText.text = "Group joining by code is currently unavailable."
        copyCodeButton.visibility = View.GONE

        val dialog = builder.create()

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Create") { _, _ ->
            val name = groupNameInput.text.toString()
            val desc = groupDescriptionInput.text.toString()

            if (name.isNotEmpty()) {
                createNewGroup(name, desc)
            }
        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") { _, _ -> dialog.dismiss() }

        joinGroupInsteadButton.setOnClickListener {
            dialog.dismiss()
            showJoinGroupDialog()
        }

        dialog.show()
    }

    private fun createNewGroup(name: String, desc: String) {
        val groupId = db.child("Groups").push().key ?: return

        db.child("Users").child(currentUserId).get().addOnSuccessListener { userSnap ->
            val leaderName = userSnap.child("username").value.toString()

            val newGroup = Group(
                groupId = groupId,
                name = name,
                description = desc,
                createdBy = leaderName,
                members = hashMapOf(currentUserId to true),
                anouncements = ""
            )

            db.child("Groups").child(groupId).setValue(newGroup)
            db.child("Users").child(currentUserId).child("groups").child(groupId).setValue(true)
                .addOnSuccessListener {
                    Toast.makeText(this, "Group Created!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showJoinGroupDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_join_group, null)
        builder.setView(view)

        val groupCodeInput = view.findViewById<EditText>(R.id.group_code_input)
        val createGroupInsteadButton = view.findViewById<TextView>(R.id.create_group_instead_button)
        val dialog = builder.create()

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Join") { _, _ ->
            val code = groupCodeInput.text.toString().trim()
            //TODO: Group joining by code is broken as 'code' is not in the Group model.
            if (code.isNotEmpty()) Toast.makeText(this, "Group joining by code is currently unavailable.", Toast.LENGTH_SHORT).show()
        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") { _, _ -> dialog.dismiss() }

        createGroupInsteadButton.setOnClickListener {
            dialog.dismiss()
            showCreateGroupDialog()
        }

        dialog.show()
    }

    private fun joinGroup(code: String) {
        //TODO: This function will fail at runtime as "code" is not a field in the Group model anymore.
        db.child("Groups").orderByChild("code").equalTo(code).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val groupId = child.key ?: continue
                        db.child("Groups").child(groupId).child("members").child(currentUserId).setValue(true) // Changed from "member"
                        db.child("Users").child(currentUserId).child("groups").child(groupId).setValue(true)
                            .addOnSuccessListener {
                                Toast.makeText(this@GroupActivity, "Joined Group Successfully!", Toast.LENGTH_SHORT).show()
                            }
                        return
                    }
                } else {
                    Toast.makeText(this@GroupActivity, "Invalid Group Code", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showEditGroupDialog(group: Group) {
        Toast.makeText(this, "Edit functionality for Firebase to be implemented similar to create", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteGroupConfirmationDialog(group: Group) {
        AlertDialog.Builder(this)
            .setTitle("Delete Group")
            .setMessage("Are you sure? This will remove the group for everyone.")
            .setPositiveButton("Delete") { _, _ ->
                db.child("Groups").child(group.groupId).removeValue()
                Toast.makeText(this, "Group Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun listenForNotifications() {
        val notificationBadge = findViewById<TextView>(R.id.notification_badge)
        db.child("Notifications").child(currentUserId).orderByChild("isRead").equalTo(false)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.childrenCount
                    if (count > 0) {
                        notificationBadge.visibility = View.VISIBLE
                        notificationBadge.text = count.toString()
                    } else {
                        notificationBadge.visibility = View.GONE
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupFilterAndSort(){
        val filterOptions = arrayOf("All", "Date Created")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, filterOptions)
        binding.filterDropdown.setAdapter(adapter)

        binding.searchInput.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                //TODO: Re-implement search/filter functionality
                updateUI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.filterDropdown.setOnItemClickListener { _, _, _, _ ->
            loadGroups() // Reloads with new sort option
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_home -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return true
            }
            R.id.action_groups -> return true
            R.id.action_create -> {
                showCreateGroupDialog()
                return false
            }
            R.id.action_tasks -> {
                startActivity(Intent(this, TaskActivity::class.java))
                finish()
                return true
            }
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                finish()
                return true
            }
        }
        return false
    }
}
