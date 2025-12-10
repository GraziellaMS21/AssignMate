package com.example.assignmate.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignmate.TaskDetailActivity
import com.example.assignmate.adapter.TaskAdapter
import com.example.assignmate.databinding.FragmentGroupTasksBinding
import com.example.assignmate.model.Task
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class GroupTasksFragment : Fragment() {

    private lateinit var binding: FragmentGroupTasksBinding
    private lateinit var adapter: TaskAdapter
    private val taskList = mutableListOf<Task>()
    private var groupId: String = ""

    companion object {
        fun newInstance(groupId: String): GroupTasksFragment {
            val fragment = GroupTasksFragment()
            val args = Bundle()
            args.putString("GROUP_ID", groupId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = arguments?.getString("GROUP_ID") ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentGroupTasksBinding.inflate(inflater, container, false)

        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(context)
        adapter = TaskAdapter(taskList) { task ->
            val intent = Intent(context, TaskDetailActivity::class.java)
            intent.putExtra("TASK_ID", task.taskId)
            startActivity(intent)
        }
        binding.tasksRecyclerView.adapter = adapter

        fetchTasksSorted()
        return binding.root
    }

    private fun fetchTasksSorted() {
        val db = FirebaseDatabase.getInstance().reference
        db.child("Tasks").orderByChild("groupId").equalTo(groupId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    taskList.clear()
                    for (child in snapshot.children) {
                        val task = child.getValue(Task::class.java)
                        if (task != null) taskList.add(task)
                    }

                    // --- SORTING LOGIC ---
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                    taskList.sortWith(Comparator { t1, t2 ->
                        val d1 = try { sdf.parse(t1.dueDate) } catch (e: Exception) { null }
                        val d2 = try { sdf.parse(t2.dueDate) } catch (e: Exception) { null }

                        // Logic: Null dates go last. Earlier dates go first.
                        if (d1 == null) 1 else if (d2 == null) -1 else d1.compareTo(d2)
                    })

                    adapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}