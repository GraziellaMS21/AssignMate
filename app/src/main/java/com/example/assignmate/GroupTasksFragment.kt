package com.example.assignmate

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        taskAdapter = TaskAdapter(tasks) { task ->
            val intent = Intent(activity, TaskDetailActivity::class.java)
            intent.putExtra("TASK_ID", task.id)
            // We need to pass the current user's ID as well, which we get from the activity
            val currentUserId = (activity as? SingleGroupActivity)?.intent?.getIntExtra("USER_ID", -1) ?: -1
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }
        tasksRecyclerView.adapter = taskAdapter
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
