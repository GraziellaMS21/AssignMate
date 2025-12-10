package com.example.assignmate.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.TaskDetailActivity
import com.example.assignmate.adapter.TaskAdapter
import com.example.assignmate.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class GroupTasksFragment : Fragment() {

    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter

    private val allTasksList = mutableListOf<Task>()
    private val displayTaskList = mutableListOf<Task>()

    private var groupId: String = ""
    private var currentUserId: String = "" // Added to store User ID
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance() // Added Auth

    companion object {
        private const val ARG_GROUP_ID = "GROUP_ID"

        @JvmStatic
        fun newInstance(groupId: String) =
            GroupTasksFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_GROUP_ID, groupId)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getString(ARG_GROUP_ID) ?: ""
        }
        // Fetch Current User ID
        currentUserId = auth.currentUser?.uid ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_tasks, container, false)
        tasksRecyclerView = view.findViewById(R.id.tasks_recycler_view)
        tasksRecyclerView.layoutManager = LinearLayoutManager(context)

        // FIX IS HERE: Updated Constructor to match TaskAdapter(list, userId, clickListener, deleteListener)
        taskAdapter = TaskAdapter(
            displayTaskList,
            currentUserId,
            onItemClicked = { task ->
                val intent = Intent(requireContext(), TaskDetailActivity::class.java).apply {
                    putExtra("TASK_ID", task.taskId)
                }
                startActivity(intent)
            },
            onDeleteClicked = { task ->
                // Pass -1 for position as we don't need to reset swipe state for menu clicks
                showDeleteConfirmationDialog(task, -1)
            }
        )
        tasksRecyclerView.adapter = taskAdapter

        setupSwipeToDelete()
        loadTasks()

        return view
    }

    private fun loadTasks() {
        db.child("Tasks").orderByChild("groupId").equalTo(groupId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allTasksList.clear()
                    for (child in snapshot.children) {
                        val task = child.getValue(Task::class.java)
                        if (task != null) {
                            allTasksList.add(task)
                        }
                    }
                    // Sort by Date
                    allTasksList.sortWith(Comparator { t1, t2 ->
                        if (t1.dueDate.isEmpty()) 1 else if (t2.dueDate.isEmpty()) -1 else t1.dueDate.compareTo(t2.dueDate)
                    })

                    refreshFilter()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun filterTasks(filterType: String, query: String) {
        displayTaskList.clear()

        val filtered = if (query.isEmpty()) {
            if(filterType == "All") allTasksList else allTasksList
        } else {
            when (filterType) {
                "By Assignee" -> allTasksList.filter {
                    it.assignedToName.contains(query, ignoreCase = true)
                }
                "By Label" -> allTasksList.filter {
                    it.description.contains(query, ignoreCase = true)
                }
                else -> allTasksList.filter {
                    it.name.contains(query, ignoreCase = true)
                }
            }
        }

        displayTaskList.addAll(filtered)
        taskAdapter.notifyDataSetChanged()
    }

    private fun refreshFilter() {
        displayTaskList.clear()
        displayTaskList.addAll(allTasksList)
        taskAdapter.notifyDataSetChanged()
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val taskToDelete = displayTaskList[position]
                showDeleteConfirmationDialog(taskToDelete, position)
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(tasksRecyclerView)
    }

    private fun showDeleteConfirmationDialog(task: Task, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete '${task.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTaskFromFirebase(task.taskId)
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Only notify adapter if it was a swipe action (position != -1)
                if (position != -1) {
                    taskAdapter.notifyItemChanged(position)
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun deleteTaskFromFirebase(taskId: String) {
        db.child("Tasks").child(taskId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
    }

    fun refreshTasks() {
        // No manual refresh needed
    }
}