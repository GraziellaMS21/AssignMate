package com.example.assignmate.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemLabelBinding
import com.example.assignmate.model.Label

class LabelAdapter(private val labels: List<Label>) : RecyclerView.Adapter<LabelAdapter.LabelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val binding = ItemLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LabelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        val label = labels[position]
        holder.bind(label)
    }

    override fun getItemCount() = labels.size

    inner class LabelViewHolder(private val binding: ItemLabelBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(label: Label) {
            binding.root.text = label.name
            binding.root.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor(label.color))
        }
    }
}
