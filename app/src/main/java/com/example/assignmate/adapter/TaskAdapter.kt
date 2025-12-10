package com.example.assignmate.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.databinding.ItemTaskBinding
import com.example.assignmate.model.Task
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private var tasks: List<Task>,
    private val currentUserId: String, // FIX: Changed Int to String
    // Removed DatabaseHelper as it is not used in Firebase architecture
    private val onItemClicked: (Task) -> Unit,
    private val onDeleteClicked: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>){
        tasks = newTasks
        notifyDataSetChanged()
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            val context = itemView.context
            binding.taskName.text = task.name
            binding.taskDescription.text = task.description

            // FIX: Date is now a String, checking isNotEmpty() instead of != 0L
            if (task.dueDate.isNotEmpty()) {
                binding.dueDate.visibility = View.VISIBLE
                binding.dueDate.text = "Due: ${task.dueDate}"
            } else {
                binding.dueDate.visibility = View.GONE
            }

            binding.status.text = task.status

            val (statusColor, statusBackground) = when (task.status) {
                "Not Started" -> R.color.status_not_started to R.drawable.status_background_not_started
                "In progress", "In Progress" -> R.color.status_in_progress to R.drawable.status_background_in_progress
                "Complete", "Completed" -> R.color.status_complete to R.drawable.status_background_complete
                else -> android.R.color.black to R.drawable.status_background_in_progress
            }

            // Use safe context for color retrieval
            try {
                binding.status.setTextColor(ContextCompat.getColor(context, statusColor))
                binding.status.setBackgroundResource(statusBackground)
            } catch (e: Exception) {
                // Fallback if resources are missing
                binding.status.setTextColor(Color.BLACK)
            }

            // FIX: Parse String date to check if overdue
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = try { sdf.parse(task.dueDate) } catch (e: Exception) { null }
            val isOverdue = date != null && date.time < System.currentTimeMillis()

            if (isOverdue && task.status != "Completed" && task.status != "Complete") {
                binding.overdueIndicator.visibility = View.VISIBLE
            } else {
                binding.overdueIndicator.visibility = View.GONE
            }

            // FIX: Handle Assignees using the String field from Firebase Model
            binding.assignedMembersChipGroup.removeAllViews()
            if (task.assignedToName.isNotEmpty() && task.assignedToName != "Unassigned") {
                binding.assigneesSection.visibility = View.VISIBLE
                val chip = Chip(context)
                chip.text = task.assignedToName
                chip.chipMinHeight = 48f
                // chip.setTextAppearance(R.style.AppChipTextAppearance) // Uncomment if style exists
                binding.assignedMembersChipGroup.addView(chip)
            } else {
                binding.assigneesSection.visibility = View.GONE
            }

            // NOTE: Label fetching removed because DatabaseHelper is gone.
            // In Firebase, labels should be loaded in the Activity or stored in the Task object.
            binding.labelsSection.visibility = View.GONE

            binding.root.setOnClickListener {
                onItemClicked(task)
            }

            binding.taskOverflowMenu.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.group_task_menu, popup.menu)
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_delete_task -> {
                            onDeleteClicked(task)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }
}