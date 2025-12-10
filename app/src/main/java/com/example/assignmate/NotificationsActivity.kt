package com.example.assignmate

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignmate.adapter.NotificationAdapter
import com.example.assignmate.databinding.ActivityNotificationsBinding
import com.example.assignmate.model.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var notificationAdapter: NotificationAdapter
    private val notifications = mutableListOf<Notification>()
    private var isSelectionMode = false

    // Firebase Setup
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get Firebase User ID (String)
        currentUserId = auth.currentUser?.uid ?: ""
        if (currentUserId.isEmpty()) {
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        loadNotifications()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            notifications,
            onMarkAsReadClicked = { notification ->
                // Now works because notification.id is a String in the updated Model
                markNotificationAsRead(notification.id)
            },
            onItemLongClicked = {
                toggleSelectionMode()
            }
        )
        binding.notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = notificationAdapter
        }
    }

    // --- FIREBASE LOGIC ---

    private fun loadNotifications() {
        db.child("Notifications").child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    notifications.clear()
                    for (child in snapshot.children) {
                        val notification = child.getValue(Notification::class.java)
                        if (notification != null) {
                            notifications.add(notification)
                        }
                    }
                    notifications.sortByDescending { it.timestamp }
                    notificationAdapter.notifyDataSetChanged()
                    updateUIState()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun markNotificationAsRead(notificationId: String) {
        db.child("Notifications").child(currentUserId).child(notificationId).child("isRead").setValue(true)
    }

    private fun markAllNotificationsAsRead() {
        val updates = mutableMapOf<String, Any>()
        notifications.forEach {
            updates["${it.id}/isRead"] = true
        }

        if (updates.isNotEmpty()) {
            db.child("Notifications").child(currentUserId).updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "All marked as read", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteNotification(notificationId: String) {
        db.child("Notifications").child(currentUserId).child(notificationId).removeValue()
    }

    private fun deleteAllNotifications() {
        db.child("Notifications").child(currentUserId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "All notifications deleted", Toast.LENGTH_SHORT).show()
                if (isSelectionMode) toggleSelectionMode()
            }
    }

    private fun updateUIState() {
        if (notifications.isEmpty()) {
            // Optional: Show empty state logic
        }
    }

    // --- UI LOGIC ---

    private fun setupClickListeners() {
        binding.markAllReadButton.setOnClickListener {
            showMarkAllReadConfirmationDialog()
        }

        binding.deleteButton.setOnClickListener {
            toggleSelectionMode()
        }

        binding.cancelSelectionButton.setOnClickListener {
            toggleSelectionMode()
        }

        binding.deleteSelectionButton.setOnClickListener {
            val selectedNotifications = notificationAdapter.getSelectedNotifications()
            if (selectedNotifications.isNotEmpty()) {
                showDeleteConfirmationDialog(selectedNotifications)
            } else {
                toggleSelectionMode()
            }
        }

        binding.deleteAllButton.setOnClickListener {
            showDeleteAllConfirmationDialog()
        }
    }

    private fun toggleSelectionMode() {
        isSelectionMode = !isSelectionMode
        notificationAdapter.setSelectionMode(isSelectionMode)
        if (isSelectionMode) {
            binding.normalToolbarLayout.visibility = View.GONE
            binding.selectionToolbarLayout.visibility = View.VISIBLE
        } else {
            binding.normalToolbarLayout.visibility = View.VISIBLE
            binding.selectionToolbarLayout.visibility = View.GONE
            notificationAdapter.clearSelections()
        }
    }

    private fun showMarkAllReadConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Mark All As Read")
            .setMessage("Are you sure you want to mark all messages as read?")
            .setPositiveButton("Yes") { _, _ ->
                markAllNotificationsAsRead()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(selectedNotifications: List<Notification>) {
        AlertDialog.Builder(this)
            .setTitle("Delete Notifications")
            .setMessage("Are you sure you want to delete ${selectedNotifications.size} selected notifications?")
            .setPositiveButton("Delete") { _, _ ->
                selectedNotifications.forEach { notification ->
                    deleteNotification(notification.id)
                }
                toggleSelectionMode()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAllConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete All Notifications")
            .setMessage("Are you sure you want to delete all notifications?")
            .setPositiveButton("Delete") { _, _ ->
                deleteAllNotifications()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}