package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemUpcomingTaskBinding
import com.example.assignmate.model.Task
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class UpcomingTasksAdapter(
    tasks: List<Task>,
    private val onTaskClicked: (Task) -> Unit
) : RecyclerView.Adapter<UpcomingTasksAdapter.UpcomingTaskViewHolder>() {

    // FIX 1: Filter check changed from != 0L to isNotEmpty() since date is a String
    private val filteredTasks = tasks.filter { it.dueDate.isNotEmpty() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpcomingTaskViewHolder {
        val binding = ItemUpcomingTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UpcomingTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UpcomingTaskViewHolder, position: Int) {
        val task = filteredTasks[position]
        holder.bind(task)
    }

    override fun getItemCount() = filteredTasks.size

    inner class UpcomingTaskViewHolder(private val binding: ItemUpcomingTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.taskName.text = task.name

            // Handle groupName (defaults to "Group" if empty)
            binding.groupName.text = if (task.groupName.isNotEmpty()) task.groupName else "Group Task"

            // FIX 2: Parse String date to Long for calculation
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = try {
                sdf.parse(task.dueDate)
            } catch (e: Exception) {
                null
            }

            if (date != null) {
                val diff = date.time - System.currentTimeMillis()
                val days = TimeUnit.MILLISECONDS.toDays(diff)

                // Logic for "Days Left" display
                binding.dueDate.text = when {
                    days < -1 -> "${-days} days ago" // Overdue
                    days == -1L -> "1 day ago"
                    days == 0L -> "Due today"
                    days == 1L -> "1 day left"
                    else -> "$days days left"
                }
            } else {
                binding.dueDate.text = "Invalid Date"
            }

            binding.root.setOnClickListener { onTaskClicked(task) }
        }
    }
}