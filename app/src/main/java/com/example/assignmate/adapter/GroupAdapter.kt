package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.model.Group
import java.text.SimpleDateFormat
import java.util.*

class GroupAdapter(
    private var groups: List<Group>,
    private val currentUserId: String,
    private val onGroupClicked: (Group) -> Unit,
    private val onEditClicked: (Group) -> Unit,
    private val onDeleteClicked: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    private var filteredGroups = groups.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group_card, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(filteredGroups[position])
    }

    override fun getItemCount() = filteredGroups.size

    fun setGroups(newGroups: List<Group>) {
        this.groups = newGroups
        this.filteredGroups = newGroups.toMutableList()
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filteredGroups = if (query.isEmpty()) {
            groups.toMutableList()
        } else {
            groups.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val groupName: TextView = itemView.findViewById(R.id.group_name)
        private val groupLeader: TextView = itemView.findViewById(R.id.group_leader)
        private val groupDesc: TextView = itemView.findViewById(R.id.group_description)
        private val optionsMenu: ImageView = itemView.findViewById(R.id.options_menu)

        // TODO: Re-implement stats display. The following fields were removed from Group model:
        // lastUpdated, assignedTasksCount, progress
        private val membersCount: TextView? = itemView.findViewById(R.id.group_members_count)
        // private val lastUpdate: TextView = itemView.findViewById(R.id.last_update_text)
        // private val tasksCount: TextView? = itemView.findViewById(R.id.group_tasks_count)
        // private val progressBar: ProgressBar? = itemView.findViewById(R.id.group_progress_bar)

        // TODO: Re-implement favourite feature. '''isFavourite''' was removed from Group model.
        // private val favIcon: ImageView = itemView.findViewById(R.id.favourite_icon)

        fun bind(group: Group) {
            groupName.text = group.name
            groupLeader.text = "Leader: ${group.createdBy}"
            groupDesc.text = group.description

            // Stats Binding
            membersCount?.text = "${group.members.size} Members"
            
            // TODO: Re-implement last updated display
            // val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            // val date = Date(group.lastUpdated)
            // lastUpdate.text = "Last Update: ${sdf.format(date)}"

            // TODO: Re-implement tasks count and progress bar
            // tasksCount?.text = "${group.assignedTasksCount} Tasks"
            // progressBar?.progress = group.progress

            // TODO: Re-implement favourite icon logic
            // if (group.isFavourite) {
            //     favIcon.setImageResource(R.drawable.ic_star_filled)
            // } else {
            //     favIcon.setImageResource(R.drawable.ic_star_outline)
            // }
            // favIcon.setOnClickListener {
            //     onFavouriteClicked(group)
            // }

            itemView.setOnClickListener {
                onGroupClicked(group)
            }

            // Options Menu (Edit/Delete)
            optionsMenu.setOnClickListener {
                val popup = PopupMenu(itemView.context, optionsMenu)
                popup.menu.add("Edit")
                popup.menu.add("Delete")

                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Edit" -> onEditClicked(group)
                        "Delete" -> onDeleteClicked(group)
                    }
                    true
                }
                popup.show()
            }
        }
    }
}