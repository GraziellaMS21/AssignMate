package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.model.NewSubtask

class NewSubtaskAdapter(
    private val subtasks: MutableList<NewSubtask>
) : RecyclerView.Adapter<NewSubtaskAdapter.NewSubtaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewSubtaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subtask, parent, false)
        return NewSubtaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewSubtaskViewHolder, position: Int) {
        val subtask = subtasks[position]
        holder.bind(subtask)
    }

    override fun getItemCount() = subtasks.size

    fun addSubtask(subtask: NewSubtask) {
        subtasks.add(subtask)
        notifyItemInserted(subtasks.size - 1)
    }

    inner class NewSubtaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val subtaskCheckbox: CheckBox = itemView.findViewById(R.id.subtask_checkbox)
        private val subtaskName: TextView = itemView.findViewById(R.id.subtask_name)

        fun bind(subtask: NewSubtask) {
            subtaskCheckbox.isChecked = subtask.isCompleted
            subtaskName.text = subtask.name

            subtaskCheckbox.setOnCheckedChangeListener { _, isChecked ->
                subtask.isCompleted = isChecked
            }
        }
    }
}
