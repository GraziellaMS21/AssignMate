package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemGroupStatusBinding
import com.example.assignmate.model.Group

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
            binding.groupProgress.progress = group.progress
            binding.progressText.text = "${group.progress}%"
        }
    }
}
