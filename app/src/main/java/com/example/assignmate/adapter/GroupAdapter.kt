package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.databinding.ItemGroupCardBinding
import com.example.assignmate.model.Group
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GroupAdapter(
    private val groups: List<Group>,
    private val onGroupClicked: (Group) -> Unit,
    private val onEditClicked: (Group) -> Unit,
    private val onDeleteClicked: (Group) -> Unit,
    private val onAddToFavouriteClicked: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.bind(group)
    }

    override fun getItemCount() = groups.size

    inner class GroupViewHolder(private val binding: ItemGroupCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: Group) {
            binding.groupName.text = group.name
            binding.groupLeader.text = "Group Leader: ${group.leader}"
            binding.groupMembers.text = "Members: ${group.members.size}"
            binding.assignedTasks.text = "Assigned Tasks: ${group.assignedTasksCount}"
            binding.groupDescription.text = group.description
            binding.lastUpdated.text = "Last updated: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(group.lastUpdated))}"
            binding.groupProgress.progress = group.progress

            binding.root.setOnClickListener {
                onGroupClicked(group)
            }

            binding.groupOverflowMenu.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menuInflater.inflate(R.menu.group_card_menu, popup.menu)
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_add_to_favourite -> {
                            onAddToFavouriteClicked(group)
                            true
                        }
                        R.id.action_edit_group -> {
                            onEditClicked(group)
                            true
                        }
                        R.id.action_delete_group -> {
                            onDeleteClicked(group)
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
