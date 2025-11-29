package com.example.assignmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.model.Task

private const val VIEW_TYPE_GROUP = 0
private const val VIEW_TYPE_TASK = 1

class GroupedTaskAdapter(private val tasksByGroup: Map<String, List<Task>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()

    init {
        tasksByGroup.forEach { (groupName, tasks) ->
            items.add(groupName)
            items.addAll(tasks)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) {
            VIEW_TYPE_GROUP
        } else {
            VIEW_TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_GROUP) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group_header, parent, false)
            GroupViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
            TaskViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is GroupViewHolder) {
            holder.bind(items[position] as String)
        } else if (holder is TaskViewHolder) {
            holder.bind(items[position] as Task)
        }
    }

    override fun getItemCount(): Int = items.size

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupNameTextView: TextView = itemView.findViewById(R.id.group_name_header)
        fun bind(groupName: String) {
            groupNameTextView.text = groupName
        }
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskNameTextView: TextView = itemView.findViewById(R.id.task_name)
        private val taskStatusTextView: TextView = itemView.findViewById(R.id.status)

        fun bind(task: Task) {
            taskNameTextView.text = task.name
            taskStatusTextView.text = "Status: ${task.status}"
        }
    }
}
