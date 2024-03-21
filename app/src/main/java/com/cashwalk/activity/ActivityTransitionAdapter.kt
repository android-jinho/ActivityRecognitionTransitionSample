package com.cashwalk.activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cashwalk.activity.databinding.ItemActivityTransitionBinding

class ActivityTransitionAdapter
    : RecyclerView.Adapter<ActivityTransitionAdapter.ActivityTransitionViewHolder>() {

    private val activityTransitionList: MutableList<String> = mutableListOf()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ActivityTransitionViewHolder = ActivityTransitionViewHolder(
        ItemActivityTransitionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ActivityTransitionViewHolder, position: Int) {
        holder.bind(activityTransition = activityTransitionList[position])
    }

    override fun getItemCount(): Int = activityTransitionList.size

    fun addItem(activityTransition: String) {
        activityTransitionList.add(activityTransition)
        notifyItemInserted(activityTransitionList.lastIndex)
    }

    class ActivityTransitionViewHolder(
        private val binding: ItemActivityTransitionBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(activityTransition: String) {
            binding.tvItemActivity.text = activityTransition
        }
    }
}