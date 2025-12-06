package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemManageLabelBinding
import com.example.assignmate.model.Label

class ManageLabelsAdapter(
    private val labels: MutableList<Label>,
    private val onLabelClick: (Label) -> Unit
) : RecyclerView.Adapter<ManageLabelsAdapter.LabelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val binding = ItemManageLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LabelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        val label = labels[position]
        holder.bind(label)
    }

    override fun getItemCount() = labels.size

    fun addLabel(label: Label) {
        labels.add(label)
        notifyItemInserted(labels.size - 1)
    }

    fun updateLabel(label: Label) {
        val index = labels.indexOfFirst { it.id == label.id }
        if (index != -1) {
            labels[index] = label
            notifyItemChanged(index)
        }
    }

    inner class LabelViewHolder(private val binding: ItemManageLabelBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(label: Label) {
            binding.labelName.text = label.name
            binding.labelColor.setBackgroundColor(android.graphics.Color.parseColor(label.color))
            binding.root.setOnClickListener {
                onLabelClick(label)
            }
        }
    }
}
