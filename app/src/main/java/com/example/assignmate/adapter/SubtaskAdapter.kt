package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemSubtaskBinding
import com.example.assignmate.model.Subtask

class SubtaskAdapter(
    private val subtasks: List<Subtask>,
    private val onSubtaskChecked: (Subtask, Boolean) -> Unit
) : RecyclerView.Adapter<SubtaskAdapter.SubtaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubtaskViewHolder {
        val binding = ItemSubtaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubtaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubtaskViewHolder, position: Int) {
        val subtask = subtasks[position]
        holder.bind(subtask)
    }

    override fun getItemCount() = subtasks.size

    inner class SubtaskViewHolder(private val binding: ItemSubtaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(subtask: Subtask) {
            binding.subtaskCheckbox.isChecked = subtask.isCompleted
            binding.subtaskName.text = subtask.name

            binding.subtaskCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onSubtaskChecked(subtask, isChecked)
            }
        }
    }
}
