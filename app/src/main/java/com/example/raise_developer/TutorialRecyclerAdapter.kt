package com.example.raise_developer

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ResourceCursorAdapter
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView


class TutorialRecyclerAdapter(private val tutorialPage: ArrayList<Int>) : RecyclerView.Adapter<TutorialRecyclerAdapter.PagerViewHolder>() {

    inner class PagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tutorialImage: ImageView = itemView.findViewById(R.id.tutorialImage)

        fun bind(position: Int) {
            tutorialImage.setImageResource(tutorialPage[position])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_view,
            parent,
            false
        )
        return PagerViewHolder(view)
    }
    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = tutorialPage.size
}
