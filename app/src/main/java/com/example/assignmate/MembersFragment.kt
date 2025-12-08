package com.example.assignmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignmate.adapter.MembersAdapter
import com.example.assignmate.model.Member

class MembersFragment : Fragment() {

    private lateinit var databaseHelper: DatabaseHelper
    private var groupId: Long = -1
    private var currentUserId: Int = -1

    private lateinit var membersRecyclerView: RecyclerView
    private lateinit var memberAdapter: MembersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getLong(ARG_GROUP_ID)
            currentUserId = it.getInt(ARG_CURRENT_USER_ID)
        }
        databaseHelper = DatabaseHelper(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_members, container, false)
        membersRecyclerView = view.findViewById(R.id.members_recycler_view)
        membersRecyclerView.layoutManager = LinearLayoutManager(context)

        loadMembers()

        return view
    }

    fun loadMembers() {
        val members = databaseHelper.getGroupMembers(groupId)
        val currentUserRole = databaseHelper.getRoleForUserInGroup(currentUserId, groupId) ?: "member"

        memberAdapter = MembersAdapter(members, currentUserRole) { member, action ->
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
        if (databaseHelper.updateMemberRole(groupId, member.id, role)) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            loadMembers()
        } else {
            Toast.makeText(requireContext(), "Failed to update role", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRemoveMemberConfirmationDialog(member: Member) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Member")
            .setMessage("Are you sure you want to remove ${member.name} from the group?")
            .setPositiveButton("Remove") { _, _ ->
                if (databaseHelper.removeMemberFromGroup(groupId, member.id)) {
                    Toast.makeText(requireContext(), "Member removed", Toast.LENGTH_SHORT).show()
                    loadMembers()
                } else {
                    Toast.makeText(requireContext(), "Failed to remove member", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val ARG_GROUP_ID = "GROUP_ID"
        private const val ARG_CURRENT_USER_ID = "CURRENT_USER_ID"

        @JvmStatic
        fun newInstance(groupId: Long, currentUserId: Int) =
            MembersFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_GROUP_ID, groupId)
                    putInt(ARG_CURRENT_USER_ID, currentUserId)
                }
            }
    }
}
