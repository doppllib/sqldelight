package com.example.sqldelight.hockey.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.example.sqldelight.hockey.R

class MainActivity : Activity() {
  private val listView by lazy { findViewById<Button>(R.id.list) }
  private val teamsView by lazy { findViewById<Button>(R.id.teams) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)
    listView.setOnClickListener {
      startActivity(Intent(this, PlayersActivity::class.java))
    }
    teamsView.setOnClickListener {
      startActivity(Intent(this, TeamsActivity::class.java))
    }
  }
}
