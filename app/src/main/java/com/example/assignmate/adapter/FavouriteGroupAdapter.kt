package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemFavouriteGroupBinding
import com.example.assignmate.model.Group

class FavouriteGroupAdapter(private val groups: List<Group>, private val onGroupClicked: (Group) -> Unit) : RecyclerView.Adapter<FavouriteGroupAdapter.FavouriteGroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteGroupViewHolder {
        val binding = ItemFavouriteGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavouriteGroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavouriteGroupViewHolder, position: Int) {
        val group = groups[position]
        holder.bind(group)
    }

    override fun getItemCount() = groups.size

    inner class FavouriteGroupViewHolder(private val binding: ItemFavouriteGroupBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: Group) {
            binding.favouriteGroupName.text = group.name
            binding.favouriteGroupDescription.text = group.description
            binding.root.setOnClickListener { onGroupClicked(group) }
        }
    }
}
