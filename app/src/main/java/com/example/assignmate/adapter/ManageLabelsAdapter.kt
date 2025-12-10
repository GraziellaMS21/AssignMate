package com.example.assignmate.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemManageLabelBinding
import com.example.assignmate.model.Label

class ManageLabelsAdapter(
    private val labels: MutableList<Label>,
    private val onEditClicked: (Label) -> Unit
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

    fun updateLabel(updatedLabel: Label) {
        val index = labels.indexOfFirst { it.id == updatedLabel.id }
        if (index != -1) {
            labels[index] = updatedLabel
            notifyItemChanged(index)
        }
    }

    inner class LabelViewHolder(private val binding: ItemManageLabelBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(label: Label) {
            binding.labelNameView.text = label.name

            val color = try {
                Color.parseColor(label.color)
            } catch (e: IllegalArgumentException) {
                Color.GRAY
            }

            val background = binding.labelColorView.background.mutate()
            if (background is GradientDrawable) {
                background.setColor(color)
            }

            binding.editLabelButton.setOnClickListener {
                onEditClicked(label)
            }
        }
    }
}
