package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemMemberBinding
import com.example.assignmate.model.Member

class MemberAdapter(
    private val members: List<Member>,
    private val onMemberAction: (Member, View) -> Unit
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]
        holder.bind(member)
    }

    override fun getItemCount() = members.size

    inner class MemberViewHolder(private val binding: ItemMemberBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(member: Member) {
            binding.memberName.text = member.name
            binding.memberRole.text = member.role.replaceFirstChar { it.uppercase() }

            itemView.setOnClickListener {
                onMemberAction(member, itemView)
            }
        }
    }
}
