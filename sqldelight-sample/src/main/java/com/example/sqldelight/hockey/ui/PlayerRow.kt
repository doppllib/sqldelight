package com.example.sqldelight.hockey.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.example.sqldelight.hockey.R
import com.example.sqldelight.hockey.data.ForTeam

class PlayerRow(
  context: Context,
  attrs: AttributeSet
) : LinearLayout(context, attrs) {
  private val playerName by lazy { findViewById<TextView>(R.id.player_name) }
  private val teamName by lazy { findViewById<TextView>(R.id.team_name) }
  private val playerNumber by lazy { findViewById<TextView>(R.id.player_number) }

  fun populate(row: ForTeam) {
    playerName.text = row.first_name + " " + row.last_name
    playerNumber.text = row.number
    teamName.text = row.teamName
  }
}
