package com.example.assignmate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.model.Comment
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(private val comments: List<Comment>) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount() = comments.size

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val username: TextView = itemView.findViewById(R.id.comment_username)
        private val commentText: TextView = itemView.findViewById(R.id.comment_text)
        private val timestamp: TextView = itemView.findViewById(R.id.comment_timestamp)

        fun bind(comment: Comment) {
            username.text = comment.username
            commentText.text = comment.commentText
            timestamp.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(comment.timestamp))
        }
    }
}
