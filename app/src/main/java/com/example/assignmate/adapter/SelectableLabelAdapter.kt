package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.model.Label

class SelectableLabelAdapter(
    private val allLabels: List<Label>,
    // FIX: Changed MutableSet<Long> to MutableSet<String>
    private val selectedLabelIds: MutableSet<String>
) : RecyclerView.Adapter<SelectableLabelAdapter.SelectableLabelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectableLabelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selectable_label, parent, false)
        return SelectableLabelViewHolder(view)
    }

    override fun onBindViewHolder(holder: SelectableLabelViewHolder, position: Int) {
        val label = allLabels[position]
        holder.bind(label)
    }

    override fun getItemCount() = allLabels.size

    // FIX: Changed return type to Set<String>
    fun getSelectedLabelIds(): Set<String> = selectedLabelIds

    inner class SelectableLabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val labelCheckbox: CheckBox = itemView.findViewById(R.id.label_checkbox)

        fun bind(label: Label) {
            labelCheckbox.text = label.name

            // FIX: Remove listener temporarily to set state without triggering logic
            labelCheckbox.setOnCheckedChangeListener(null)

            // Now safe because both label.id and the Set use Strings
            labelCheckbox.isChecked = selectedLabelIds.contains(label.id)

            labelCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedLabelIds.add(label.id)
                } else {
                    selectedLabelIds.remove(label.id)
                }
            }
        }
    }
}