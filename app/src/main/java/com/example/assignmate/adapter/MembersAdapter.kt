package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.model.Member

class MembersAdapter(
    private val members: List<Member>,
    private val onMemberActionListener: (Member, String) -> Unit
) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        holder.bind(member)
        holder.memberActionsButton.setOnClickListener {
            val popup = PopupMenu(holder.itemView.context, it)
            popup.menuInflater.inflate(R.menu.member_actions_menu, popup.menu)

            if (member.role == "co-leader") {
                popup.menu.findItem(R.id.action_assign_co_leader).title = "Remove as Co-Leader"
            } else {
                popup.menu.findItem(R.id.action_assign_co_leader).title = "Assign as Co-Leader"
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_assign_co_leader -> {
                        val action = if (member.role == "co-leader") "remove_co_leader" else "assign_co_leader"
                        onMemberActionListener(member, action)
                        true
                    }
                    R.id.action_remove_member -> {
                        onMemberActionListener(member, "remove_member")
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun getItemCount() = members.size

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val memberName: TextView = itemView.findViewById(R.id.member_name)
        val memberActionsButton: ImageButton = itemView.findViewById(R.id.member_actions_button)

        fun bind(member: Member) {
            var name = member.name
            if (member.role == "co-leader") {
                name += " (Co-Leader)"
            }
            memberName.text = name
        }
    }
}
