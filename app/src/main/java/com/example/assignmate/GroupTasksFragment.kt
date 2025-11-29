package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.adapter.TaskAdapter
import com.example.assignmate.model.Task

class GroupTasksFragment : Fragment() {

    private lateinit var databaseHelper: DatabaseHelper
    private var groupId: Long = -1

    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getLong(ARG_GROUP_ID)
        }
        databaseHelper = DatabaseHelper(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_tasks, container, false)
        tasksRecyclerView = view.findViewById(R.id.tasks_recycler_view)
        tasksRecyclerView.layoutManager = LinearLayoutManager(context)

        loadTasks()

        return view
    }

    private fun loadTasks() {
        val tasks = databaseHelper.getTasksForGroup(groupId)
        val currentUserId = (activity as? SingleGroupActivity)?.intent?.getIntExtra("USER_ID", -1) ?: -1
        taskAdapter = TaskAdapter(tasks, currentUserId) { task ->
            showDeleteConfirmationDialog(task)
        }
        tasksRecyclerView.adapter = taskAdapter
    }

    fun refreshTasks() {
        loadTasks()
    }

    private fun showDeleteConfirmationDialog(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { _, _ ->
                if (databaseHelper.deleteTask(task.id)) {
                    Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show()
                    refreshTasks()
                } else {
                    Toast.makeText(requireContext(), "Failed to delete task", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val ARG_GROUP_ID = "GROUP_ID"

        @JvmStatic
        fun newInstance(groupId: Long) =
            GroupTasksFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_GROUP_ID, groupId)
                }
            }
    }
}
