package com.example.assignmate

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.assignmate.adapter.ManageLabelsAdapter
import com.example.assignmate.adapter.NewSubtaskAdapter
import com.example.assignmate.adapter.SelectableLabelAdapter
import com.example.assignmate.databinding.ActivitySingleGroupBinding
import com.example.assignmate.fragments.GroupTasksFragment
import com.example.assignmate.fragments.MembersFragment
import com.example.assignmate.model.Label
import com.example.assignmate.model.Member
import com.example.assignmate.model.NewSubtask
import com.example.assignmate.model.Task
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import yuku.ambilwarna.AmbilWarnaDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SingleGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySingleGroupBinding
    private lateinit var notificationHelper: NotificationHelper
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private var groupId: String = ""
    private var currentUserId: String = ""
    private var groupName: String = "Group"

    private var defaultColor: Int = 0
    private var currentUserRole: String? = null
    private var isGroupFavourite: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notificationHelper = NotificationHelper(this)

        groupId = intent.getStringExtra("GROUP_ID") ?: ""
        currentUserId = intent.getStringExtra("USER_ID") ?: ""
        groupName = intent.getStringExtra("GROUP_NAME") ?: "Group"

        if (groupId.isEmpty() || currentUserId.isEmpty()) {
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = groupName

        fetchUserRoleAndStatus()

        binding.fabAddTaskButton.setOnClickListener {
            showCreateTaskDialog()
        }

        binding.viewPager.adapter = ViewPagerAdapter(this)

        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Group Tasks"
                1 -> "Members"
                else -> null
            }
        }.attach()

        setupFilter()
    }

    // --- SETUP & UTILS ---

    private fun fetchUserRoleAndStatus() {
        db.child("Groups").child(groupId).child("members").child(currentUserId).get().addOnSuccessListener { snapshot ->
            val roleValue = snapshot.value
            currentUserRole = if (roleValue is Boolean && roleValue) {
                "leader"
            } else {
                roleValue.toString()
            }
            binding.fabAddTaskButton.visibility = if (currentUserRole == "leader" || currentUserRole == "co-leader") View.VISIBLE else View.GONE
            invalidateOptionsMenu()
        }

        db.child("Users").child(currentUserId).child("favourites").child(groupId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isGroupFavourite = snapshot.exists()
                invalidateOptionsMenu()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupFilter() {
        val filterOptions = arrayOf("All", "By Assignee", "By Label")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, filterOptions)
        binding.filterDropdown.setAdapter(adapter)

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterTasks() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.filterDropdown.setOnItemClickListener { _, _, _, _ -> filterTasks() }
    }

    private fun filterTasks() {
        val fragment = supportFragmentManager.findFragmentByTag("f0")
        if (fragment is GroupTasksFragment) {
            val filterType = binding.filterDropdown.text.toString()
            val searchQuery = binding.searchInput.text.toString()
            fragment.filterTasks(filterType, searchQuery)
        }
    }

    // --- MENU ---

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.single_group_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val favouriteMenuItem = menu?.findItem(R.id.action_add_to_favourite)
        favouriteMenuItem?.title = if (isGroupFavourite) "Remove from Favourites" else "Add to Favourites"

        val canManageGroup = currentUserRole == "leader" || currentUserRole == "co-leader"
        menu?.findItem(R.id.action_edit_group)?.isVisible = canManageGroup
        menu?.findItem(R.id.action_delete_group)?.isVisible = currentUserRole == "leader"
        menu?.findItem(R.id.action_manage_labels)?.isVisible = canManageGroup
        menu?.findItem(R.id.action_add_members)?.isVisible = canManageGroup

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_add_to_favourite -> toggleFavourite()
            R.id.action_edit_group -> showEditGroupDialog()
            R.id.action_delete_group -> showDeleteGroupDialog()
            R.id.action_manage_labels -> showManageLabelsDialog()
            R.id.action_add_members -> showAddMembersDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // --- ACTIONS ---

    private fun toggleFavourite() {
        val favRef = db.child("Users").child(currentUserId).child("favourites").child(groupId)
        if (isGroupFavourite) {
            favRef.removeValue()
            Toast.makeText(this, "Removed from favourites", Toast.LENGTH_SHORT).show()
        } else {
            favRef.setValue(true)
            Toast.makeText(this, "Added to favourites", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditGroupDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Group")
        val view = layoutInflater.inflate(R.layout.dialog_create_group, null)
        builder.setView(view)

        val groupNameInput = view.findViewById<EditText>(R.id.group_name_input)
        val groupDescInput = view.findViewById<EditText>(R.id.group_description_input)

        db.child("Groups").child(groupId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                groupNameInput.setText(snapshot.child("name").value.toString())
                groupDescInput.setText(snapshot.child("description").value.toString())
            }
        }

        builder.setPositiveButton("Save") { dialog, _ ->
            val newName = groupNameInput.text.toString()
            val newDesc = groupDescInput.text.toString()
            if (newName.isNotEmpty()) {
                val updates = mapOf("name" to newName, "description" to newDesc)
                db.child("Groups").child(groupId).updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show()
                    supportActionBar?.title = newName
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showDeleteGroupDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Group")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ ->
                db.child("Groups").child(groupId).removeValue().addOnSuccessListener {
                    Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    // --- LABEL MANAGEMENT ---
    private fun showManageLabelsDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_manage_labels, null)
        builder.setView(view)

        val labelsRecyclerView = view.findViewById<RecyclerView>(R.id.labels_recycler_view)
        val createNewLabelButton = view.findViewById<View>(R.id.create_new_label_button)
        val labelsList = mutableListOf<Label>()

        labelsRecyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = ManageLabelsAdapter(labelsList) { label ->
            showEditLabelDialog(label) { updatedLabel ->
                db.child("Groups").child(groupId).child("labels").child(updatedLabel.id).setValue(updatedLabel)
            }
        }
        labelsRecyclerView.adapter = adapter

        // Load Labels
        db.child("Groups").child(groupId).child("labels").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                labelsList.clear()
                for(child in snapshot.children) {
                    val label = child.getValue(Label::class.java)
                    if(label != null) labelsList.add(label)
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        createNewLabelButton.setOnClickListener {
            showEditLabelDialog(null) { newLabel ->
                val key = db.child("Groups").child(groupId).child("labels").push().key ?: return@showEditLabelDialog
                // Assign the Firebase key as the ID
                val labelWithId = newLabel.copy(id = key)
                db.child("Groups").child(groupId).child("labels").child(key).setValue(labelWithId)
            }
        }

        builder.setPositiveButton("Done", null)
        builder.show()
    }

    private fun showEditLabelDialog(label: Label?, onLabelUpdated: (Label) -> Unit) {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_label, null)
        builder.setView(view)

        val labelNameInput = view.findViewById<EditText>(R.id.label_name_input)
        val colorPicker = view.findViewById<View>(R.id.color_picker)

        if (label != null) {
            labelNameInput.setText(label.name)
            defaultColor = Color.parseColor(label.color)
            colorPicker?.setBackgroundColor(defaultColor)
        } else {
            defaultColor = Color.parseColor("#F28A30")
            colorPicker?.setBackgroundColor(defaultColor)
        }

        colorPicker?.setOnClickListener {
            val colorPickerDialog = AmbilWarnaDialog(this, defaultColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog?) {}
                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    defaultColor = color
                    colorPicker?.setBackgroundColor(color)
                }
            })
            colorPickerDialog.show()
        }

        builder.setPositiveButton(if (label == null) "Create" else "Save") { _, _ ->
            val labelName = labelNameInput.text.toString()
            val labelColor = String.format("#%06X", 0xFFFFFF and defaultColor)

            if (labelName.isNotEmpty()) {
                val id = label?.id ?: "" // Empty string for new, existing string for update
                onLabelUpdated(Label(id, labelName, labelColor))
            } else {
                Toast.makeText(this, "Please enter a label name", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showAddMembersDialog() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_member, null)
        builder.setView(view)

        val groupCodeText = view.findViewById<android.widget.TextView>(R.id.group_code_text)
        db.child("Groups").child(groupId).child("code").get().addOnSuccessListener {
            val code = it.value.toString()
            groupCodeText.text = "Group Code: $code"

            view.findViewById<View>(R.id.copy_icon).setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Group Code", code)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
            }
        }

        val emailInput = view.findViewById<EditText>(R.id.email_input)

        builder.setPositiveButton("Add") { dialog, _ ->
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                db.child("Users").orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.exists()){
                            for(child in snapshot.children){
                                val newMemberId = child.key ?: continue
                                db.child("Groups").child(groupId).child("members").child(newMemberId).setValue("member")
                                db.child("Users").child(newMemberId).child("groups").child(groupId).setValue(true)
                                Toast.makeText(this@SingleGroupActivity, "Member added", Toast.LENGTH_SHORT).show()
                                notificationHelper.sendNotification(newMemberId, "New Group", "Added to group $groupName", 1)
                            }
                        } else {
                            Toast.makeText(this@SingleGroupActivity, "User not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showCreateTaskDialog() {
        db.child("Groups").child(groupId).child("members").get().addOnSuccessListener { snapshot ->
            val memberIds = snapshot.children.mapNotNull { it.key }
            if (memberIds.isEmpty()) {
                showActualCreateTaskDialog(emptyList())
                return@addOnSuccessListener
            }

            val loadedMembers = mutableListOf<Member>()
            var count = 0
            for (uid in memberIds) {
                db.child("Users").child(uid).get().addOnSuccessListener { userSnap ->
                    val name = userSnap.child("username").value.toString()
                    loadedMembers.add(Member(uid, name, "member"))
                    count++
                    if (count == memberIds.size) {
                        showActualCreateTaskDialog(loadedMembers)
                    }
                }
            }
        }
    }

    private fun showActualCreateTaskDialog(members: List<Member>) {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_create_task, null)
        builder.setView(view)

        val taskNameInput = view.findViewById<EditText>(R.id.task_name_input)
        val taskDescInput = view.findViewById<EditText>(R.id.task_description_input)
        val dueDateInput = view.findViewById<EditText>(R.id.due_date_input)
        val assignToLayout = view.findViewById<View>(R.id.assign_to_layout)
        val chipGroup = view.findViewById<ChipGroup>(R.id.assigned_members_chip_group)

        var dateString = ""
        val assignedIds = mutableListOf<String>()

        dueDateInput.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                c.set(y, m, d)
                dateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(c.time)
                dueDateInput.setText(dateString)
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        assignToLayout.setOnClickListener {
            val names: Array<CharSequence> = members.map { it.name }.toTypedArray()
            val checked = BooleanArray(members.size) { members[it].id in assignedIds }

            AlertDialog.Builder(this)
                .setTitle("Assign")
                .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                    val id = members[which].id
                    if (isChecked) assignedIds.add(id) else assignedIds.remove(id)
                }
                .setPositiveButton("OK") { _, _ ->
                    chipGroup.removeAllViews()
                    members.filter { it.id in assignedIds }.forEach {
                        val chip = Chip(this)
                        chip.text = it.name
                        chipGroup.addView(chip)
                    }
                }
                .setNegativeButton("Cancel", null).show()
        }

        builder.setPositiveButton("Create") { _, _ ->
            val name = taskNameInput.text.toString()
            if (name.isNotEmpty()) {
                val taskId = db.child("Tasks").push().key ?: return@setPositiveButton
                val task = Task(
                    taskId = taskId,
                    name = name,
                    description = taskDescInput.text.toString(),
                    groupId = groupId,
                    groupName = groupName,
                    dueDate = dateString,
                    status = "Not Started",
                    assignedToId = if(assignedIds.isNotEmpty()) assignedIds[0] else "",
                    assignedToName = if(assignedIds.isNotEmpty()) "${assignedIds.size} Assigned" else "Unassigned"
                )
                db.child("Tasks").child(taskId).setValue(task)

                assignedIds.forEach { uid ->
                    notificationHelper.sendNotification(uid, "Task Assigned", "New task: $name", 1)
                }
                Toast.makeText(this, "Created!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null).show()
    }

    // --- ADAPTER ---

    inner class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GroupTasksFragment.newInstance(groupId)
                1 -> MembersFragment.newInstance(groupId, currentUserId)
                else -> throw IllegalStateException("Invalid position")
            }
        }
    }
}