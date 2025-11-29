package com.example.assignmate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.model.Comment
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(private val comments: List<Comment>) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.bind(comment)
    }

    override fun getItemCount(): Int = comments.size

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameTextView: TextView = itemView.findViewById(R.id.comment_username)
        private val commentTextView: TextView = itemView.findViewById(R.id.comment_text)
        private val timestampTextView: TextView = itemView.findViewById(R.id.comment_timestamp)

        fun bind(comment: Comment) {
            usernameTextView.text = comment.username
            commentTextView.text = comment.commentText
            timestampTextView.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(comment.timestamp))
        }
    }
}
