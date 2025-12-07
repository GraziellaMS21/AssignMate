package com.example.assignmate.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.databinding.ItemLabelSelectableBinding
import com.example.assignmate.model.Label

class SelectableLabelAdapter(
    private val allLabels: List<Label>,
    private val assignedLabels: MutableSet<Long>
) : RecyclerView.Adapter<SelectableLabelAdapter.LabelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val binding = ItemLabelSelectableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LabelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        val label = allLabels[position]
        holder.bind(label)
    }

    override fun getItemCount() = allLabels.size

    fun getSelectedLabelIds(): Set<Long> = assignedLabels

    inner class LabelViewHolder(private val binding: ItemLabelSelectableBinding) : RecyclerView.ViewHolder(binding.root) {
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

            binding.labelCheckbox.setOnCheckedChangeListener(null)
            binding.labelCheckbox.isChecked = assignedLabels.contains(label.id)

            binding.labelCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    assignedLabels.add(label.id)
                } else {
                    assignedLabels.remove(label.id)
                }
            }

            itemView.setOnClickListener {
                binding.labelCheckbox.toggle()
            }
        }
    }
}
