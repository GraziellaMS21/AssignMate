package com.example.assignmate

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView

class RegisterActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        databaseHelper = DatabaseHelper(this)

        val usernameInput = findViewById<EditText>(R.id.username_input)
        val emailInput = findViewById<EditText>(R.id.email_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val registerButton = findViewById<Button>(R.id.register_button)
        val loginText = findViewById<TextView>(R.id.login_text)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                if (databaseHelper.addUser(username, email, password)) {
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Registration failed. Email may already be in use.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        loginText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val currentUserId = intent.getIntExtra("currentUserId", -1)
        bottomNavigationView.selectedItemId = R.id.action_create // Assuming this is the intended selected item

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("currentUserId", currentUserId)
                    startActivity(intent)
                    true
                }
                R.id.action_groups -> {
                    val intent = Intent(this, GroupActivity::class.java)
                    intent.putExtra("currentUserId", currentUserId)
                    startActivity(intent)
                    true
                }
                R.id.action_create -> {
                    val intent = Intent(this, CreateGroupActivity::class.java)
                    intent.putExtra("currentUserId", currentUserId)
                    startActivity(intent)
                    true
                }
                R.id.action_tasks -> {
                    Toast.makeText(this, "Tasks not implemented yet", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
