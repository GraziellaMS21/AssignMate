package com.example.assignmate.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.model.Task
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(private val tasks: List<Task>, private val onTaskClick: (Task) -> Unit) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
        holder.itemView.setOnClickListener { onTaskClick(task) }
    }

    override fun getItemCount() = tasks.size

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskName: TextView = itemView.findViewById(R.id.task_name)
        private val taskDescription: TextView = itemView.findViewById(R.id.task_description)
        private val dueDate: TextView = itemView.findViewById(R.id.due_date)
        private val status: TextView = itemView.findViewById(R.id.status)

        fun bind(task: Task) {
            taskName.text = task.name

            if (task.description.isNotEmpty()) {
                taskDescription.text = task.description
                taskDescription.visibility = View.VISIBLE
            } else {
                taskDescription.visibility = View.GONE
            }

            if (task.dueDate > 0) {
                val date = Date(task.dueDate)
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dueDate.text = "Due: ${format.format(date)}"
                dueDate.visibility = View.VISIBLE
            } else {
                dueDate.visibility = View.GONE
            }

            if (task.status == "Complete") {
                status.text = "Completed"
                status.setTextColor(Color.GREEN)
            } else {
                status.text = "Pending"
                status.setTextColor(Color.RED)
            }
        }
    }
}
