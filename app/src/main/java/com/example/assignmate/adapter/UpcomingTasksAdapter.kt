package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemUpcomingTaskBinding
import com.example.assignmate.model.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UpcomingTasksAdapter(
    private val tasks: List<Task>,
    private val onTaskClicked: (Task) -> Unit
) : RecyclerView.Adapter<UpcomingTasksAdapter.UpcomingTaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpcomingTaskViewHolder {
        val binding = ItemUpcomingTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UpcomingTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UpcomingTaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
        holder.itemView.setOnClickListener {
            onTaskClicked(task)
        }
    }

    override fun getItemCount() = tasks.size

    class UpcomingTaskViewHolder(private val binding: ItemUpcomingTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.taskName.text = task.name
            binding.groupName.text = task.groupName
            binding.dueDate.text = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(task.dueDate))
        }
    }
}
