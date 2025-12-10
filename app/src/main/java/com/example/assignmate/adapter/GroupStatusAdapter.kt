package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemGroupStatusBinding
import com.example.assignmate.model.Group
import com.example.assignmate.model.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GroupStatusAdapter(private val groups: List<Group>) : RecyclerView.Adapter<GroupStatusAdapter.GroupStatusViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupStatusViewHolder {
        val binding = ItemGroupStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupStatusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupStatusViewHolder, position: Int) {
        val group = groups[position]
        holder.bind(group)
    }

    override fun getItemCount() = groups.size

    class GroupStatusViewHolder(private val binding: ItemGroupStatusBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: Group) {
            binding.groupName.text = group.name
            // Fetch tasks and calculate progress
            val tasksRef = FirebaseDatabase.getInstance().getReference("tasks")
            tasksRef.orderByChild("groupId").equalTo(group.groupId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val totalTasks = snapshot.childrenCount
                            var completedTasks = 0
                            for (taskSnapshot in snapshot.children) {
                                val task = taskSnapshot.getValue(Task::class.java)
                                if (task != null && task.status == "Completed") {
                                    completedTasks++
                                }
                            }

                            val progress = if (totalTasks > 0) {
                                (completedTasks * 100 / totalTasks).toInt()
                            } else {
                                0
                            }
                            binding.groupProgress.progress = progress
                            binding.progressText.text = "$progress%"

                        } else {
                            // No tasks for this group
                            binding.groupProgress.progress = 0
                            binding.progressText.text = "0%"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error, maybe log it
                        binding.groupProgress.progress = 0
                        binding.progressText.text = "N/A"
                    }
                })
        }
    }
}
