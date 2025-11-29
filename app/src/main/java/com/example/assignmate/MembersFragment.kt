package com.example.assignmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignmate.adapter.MembersAdapter
import com.example.assignmate.databinding.FragmentMembersBinding
import com.example.assignmate.model.Member

class MembersFragment : Fragment() {

    private var _binding: FragmentMembersBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: DatabaseHelper
    private var groupId: Long = -1
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            groupId = it.getLong(ARG_GROUP_ID)
            currentUserId = it.getInt(ARG_CURRENT_USER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        databaseHelper = DatabaseHelper(requireContext())
        loadMembers()
    }

    private fun loadMembers() {
        val members = databaseHelper.getGroupMembers(groupId)
        binding.membersRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.membersRecyclerView.adapter = MembersAdapter(members, ::onMemberAction)
    }

    private fun onMemberAction(member: Member, action: String) {
        if (databaseHelper.getGroupLeaderId(groupId) != currentUserId) {
            Toast.makeText(context, "Only the group leader can perform this action", Toast.LENGTH_SHORT).show()
            return
        }

        when (action) {
            "assign_co_leader" -> {
                databaseHelper.updateMemberRole(groupId, member.id, "co-leader")
                loadMembers()
            }
            "remove_co_leader" -> {
                databaseHelper.updateMemberRole(groupId, member.id, "member")
                loadMembers()
            }
            "remove_member" -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Remove Member")
                    .setMessage("Are you sure you want to remove ${member.name} from the group?")
                    .setPositiveButton("Remove") { _, _ ->
                        databaseHelper.removeMemberFromGroup(groupId, member.id)
                        loadMembers()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_GROUP_ID = "group_id"
        private const val ARG_CURRENT_USER_ID = "current_user_id"

        fun newInstance(groupId: Long, currentUserId: Int): MembersFragment {
            val fragment = MembersFragment()
            val args = Bundle()
            args.putLong(ARG_GROUP_ID, groupId)
            args.putInt(ARG_CURRENT_USER_ID, currentUserId)
            fragment.arguments = args
            return fragment
        }
    }
}
