package com.example.ferhat.myapplication

import android.graphics.BitmapFactory
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.jetbrains.anko.imageBitmap
import java.util.*

data class MessageRVD(val id: Long,
                      val expediteur: String,
                      val date: Date,
                      val image: String?,
                      var distance: Double?)

class MessageRecyclerAdapter(val list: List<MessageRVD>, val listener: (MessageRVD) -> Unit) : RecyclerView.Adapter<MessageRecyclerAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var stade: TextView = view.findViewById(R.id.tv_stade)
        var date: TextView = view.findViewById(R.id.tv_date)
        var image: ImageView = view.findViewById(R.id.iv_image)
        var distance: TextView = view.findViewById(R.id.tv_distance)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.stade.text = list[position].expediteur
        holder.date.text = list[position].date.toLocaleString()
        holder.distance.visibility = if(list[position].distance == null) View.GONE else View.VISIBLE
        holder.distance.text = "${list[position].distance} km"

        if(list[position].image != null) {
            holder.image.imageBitmap = BitmapFactory.decodeFile(list[position].image)
        } else {
            holder.image.imageBitmap = BitmapFactory.decodeResource(holder.image.context.resources, R.drawable.aucune_image)
        }
        holder.itemView.setOnClickListener{ listener(list[position]) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_message, parent, false))

    override fun getItemCount() = list.size
}