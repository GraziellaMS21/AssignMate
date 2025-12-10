package com.example.assignmate.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.R
import com.example.assignmate.adapter.MembersAdapter
import com.example.assignmate.model.Member
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MembersFragment : Fragment() {

    // Firebase References
    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private var groupId: String = ""
    private var currentUserId: String = ""

    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var memberAdapter: MembersAdapter
    private val memberList = mutableListOf<Member>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getString(ARG_GROUP_ID) ?: ""
            currentUserId = it.getString(ARG_CURRENT_USER_ID) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_members, container, false)
        membersRecyclerView = view.findViewById(R.id.members_recycler_view)
        membersRecyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize adapter
        memberAdapter = MembersAdapter(memberList, "member") { member, action ->
            handleMemberAction(member, action)
        }
        membersRecyclerView.adapter = memberAdapter

        loadMembers()

        return view
    }

    private fun loadMembers() {
        db.child("Groups").child(groupId).child("members").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                memberList.clear()
                val memberMap = mutableMapOf<String, String>() // Map<UserId, Role>

                for (child in snapshot.children) {
                    val userId = child.key ?: continue
                    val role = child.value.toString()
                    memberMap[userId] = role
                }

                val myRole = memberMap[currentUserId] ?: "member"
                updateAdapterRole(myRole)

                if (memberMap.isEmpty()) {
                    memberAdapter.notifyDataSetChanged()
                    return
                }

                var loadedCount = 0
                val totalMembers = memberMap.size

                for ((uid, role) in memberMap) {
                    db.child("Users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnap: DataSnapshot) {
                            val username = userSnap.child("username").value.toString()

                            memberList.add(Member(uid, username, role))

                            loadedCount++
                            if (loadedCount == totalMembers) {
                                memberList.sortWith(compareBy<Member> {
                                    when(it.role) { "leader" -> 1; "co-leader" -> 2; else -> 3 }
                                }.thenBy { it.name })

                                memberAdapter.notifyDataSetChanged()
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateAdapterRole(role: String) {
        memberAdapter = MembersAdapter(memberList, role) { member, action ->
            handleMemberAction(member, action)
        }
        membersRecyclerView.adapter = memberAdapter
    }

    private fun handleMemberAction(member: Member, action: String) {
        when (action) {
            "assign_co_leader" -> updateMemberRole(member, "co-leader", "Co-leader role assigned.")
            "remove_co_leader" -> updateMemberRole(member, "member", "Member role assigned.")
            "remove_member" -> showRemoveMemberConfirmationDialog(member)
        }
    }

    private fun updateMemberRole(member: Member, role: String, message: String) {
        db.child("Groups").child(groupId).child("members").child(member.id).setValue(role)
            .addOnSuccessListener {
                if (context != null) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                if (context != null) Toast.makeText(requireContext(), "Failed to update role", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showRemoveMemberConfirmationDialog(member: Member) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Member")
            .setMessage("Are you sure you want to remove ${member.name} from the group?")
            .setPositiveButton("Remove") { _, _ ->
                removeMemberFromFirebase(member)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeMemberFromFirebase(member: Member) {
        db.child("Groups").child(groupId).child("members").child(member.id).removeValue()

        db.child("Users").child(member.id).child("groups").child(groupId).removeValue()
            .addOnSuccessListener {
                if (context != null) Toast.makeText(requireContext(), "Member removed", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val ARG_GROUP_ID = "GROUP_ID"
        private const val ARG_CURRENT_USER_ID = "CURRENT_USER_ID"

        // FIX IS HERE: Added 'currentUserId' parameter
        @JvmStatic
        fun newInstance(groupId: String, currentUserId: String) =
            MembersFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_GROUP_ID, groupId)
                    putString(ARG_CURRENT_USER_ID, currentUserId)
                }
            }
    }
}