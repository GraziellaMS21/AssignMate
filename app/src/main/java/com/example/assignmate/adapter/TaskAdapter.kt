package com.example.assignmate.adapter

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.TaskDetailActivity
import com.example.assignmate.databinding.ItemTaskBinding
import com.example.assignmate.model.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private val tasks: List<Task>,
    private val currentUserId: Int,
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

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.taskName.text = task.name
            binding.taskDescription.text = task.description
            binding.dueDate.text = "Due: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(task.dueDate))}"
            binding.status.text = task.status

            val statusColor = when (task.status) {
                "Not Started" -> R.color.status_not_started
                "In progress" -> R.color.status_in_progress
                "Complete" -> R.color.status_complete
                else -> android.R.color.black
            }
            binding.status.setTextColor(ContextCompat.getColor(itemView.context, statusColor))

            binding.root.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, TaskDetailActivity::class.java).apply {
                    putExtra("TASK_ID", task.id)
                    putExtra("USER_ID", currentUserId)
                }
                context.startActivity(intent)
            }

            binding.taskOverflowMenu.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.task_detail_menu, popup.menu)
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
