package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.model.Group
import java.text.SimpleDateFormat
import java.util.*

class GroupAdapter(private val groups: List<Group>) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group_card, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.bind(group)
    }

    override fun getItemCount() = groups.size

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupIcon: ImageView = itemView.findViewById(R.id.group_icon)
        private val groupName: TextView = itemView.findViewById(R.id.group_name)
        private val groupLeader: TextView = itemView.findViewById(R.id.group_leader)
        private val groupMembers: TextView = itemView.findViewById(R.id.group_members)
        private val assignedTasks: TextView = itemView.findViewById(R.id.assigned_tasks)
        private val groupDescription: TextView = itemView.findViewById(R.id.group_description)
        private val lastUpdated: TextView = itemView.findViewById(R.id.last_updated)
        private val groupProgress: ProgressBar = itemView.findViewById(R.id.group_progress)

        fun bind(group: Group) {
            groupName.text = group.name
            groupLeader.text = "Group Leader: ${group.leader}"
            groupMembers.text = "Members: ${group.members.size}"
            // TODO: Implement assigned tasks count
            assignedTasks.text = "Assigned Tasks: 0"
            groupDescription.text = group.description
            lastUpdated.text = "Last updated: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(group.lastUpdated))}"
            groupProgress.progress = group.progress
        }
    }
}
