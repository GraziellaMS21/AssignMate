package com.example.assignmate

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.assignmate.databinding.ActivitySingleGroupBinding
import com.google.android.material.tabs.TabLayoutMediator

class SingleGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySingleGroupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val groupName = intent.getStringExtra("GROUP_NAME")
        binding.groupNameText.text = groupName

        binding.backButton.setOnClickListener {
            finish()
        }

        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Group Tasks"
                1 -> "Members"
                else -> null
            }
        }.attach()
    }

    private inner class ViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GroupTasksFragment()
                1 -> MembersFragment()
                else -> throw IllegalStateException("Invalid position")
            }
        }
    }
}
