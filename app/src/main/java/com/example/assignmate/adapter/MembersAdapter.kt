package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R

class MembersAdapter(private val members: List<String>) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(members[position])
    }

    override fun getItemCount() = members.size

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val memberName: TextView = itemView.findViewById(R.id.member_name)

        fun bind(name: String) {
            memberName.text = name
        }
    }
}
