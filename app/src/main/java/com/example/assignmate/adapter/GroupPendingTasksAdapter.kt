package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.databinding.ItemGroupPendingTasksBinding
import com.example.assignmate.model.Group

class GroupPendingTasksAdapter(
    private val groups: List<Group>,
    private val onGroupLongClicked: (Group) -> Unit
) : RecyclerView.Adapter<GroupPendingTasksAdapter.GroupPendingTasksViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupPendingTasksViewHolder {
        val binding = ItemGroupPendingTasksBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupPendingTasksViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupPendingTasksViewHolder, position: Int) {
        val group = groups[position]
        holder.bind(group)
        holder.itemView.setOnLongClickListener {
            onGroupLongClicked(group)
            true
        }
    }

    override fun getItemCount() = groups.size

    class GroupPendingTasksViewHolder(private val binding: ItemGroupPendingTasksBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: Group) {
            binding.groupName.text = group.name
            binding.pendingTaskCount.text = group.pendingTaskCount.toString()

            val progressColor = when {
                group.progress < 50 -> R.color.progress_low
                group.progress < 100 -> R.color.progress_medium
                else -> R.color.progress_high
            }
            binding.progressIndicator.setBackgroundColor(ContextCompat.getColor(binding.root.context, progressColor))
        }
    }
}
