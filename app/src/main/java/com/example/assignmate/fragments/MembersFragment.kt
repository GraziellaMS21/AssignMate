package com.example.assignmate.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.adapter.MembersAdapter
import com.google.firebase.database.*

class MembersFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MembersAdapter
    private val membersList = mutableListOf<String>()
    private var groupId: String = ""

    companion object {
        fun newInstance(groupId: String): MembersFragment {
            val fragment = MembersFragment()
            val args = Bundle()
            args.putString("GROUP_ID", groupId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupId = arguments?.getString("GROUP_ID") ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // We inflate the XML manually here because we are using a simple FrameLayout XML
        val view = inflater.inflate(R.layout.fragment_members, container, false)

        recyclerView = view.findViewById(R.id.members_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = MembersAdapter(membersList)
        recyclerView.adapter = adapter

        fetchMembers()
        return view
    }

    private fun fetchMembers() {
        val db = FirebaseDatabase.getInstance().reference

        // 1. Get List of Member IDs
        db.child("Groups").child(groupId).child("members").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                membersList.clear()
                val memberIds = snapshot.children.mapNotNull { it.key }

                // 2. Fetch Name for each ID
                for (id in memberIds) {
                    db.child("Users").child(id).get().addOnSuccessListener { userSnap ->
                        val username = userSnap.child("username").value.toString()
                        membersList.add(username)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}