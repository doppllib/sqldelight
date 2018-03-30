package com.example.sqldelight.hockey.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.example.sqldelight.hockey.R
import com.example.sqldelight.hockey.data.Team
import java.text.SimpleDateFormat

class TeamRow(
  context: Context,
  attrs: AttributeSet
) : LinearLayout(context, attrs) {
  private val teamName by lazy { findViewById<TextView>(R.id.team_name) }
  private val coachName by lazy { findViewById<TextView>(R.id.coach_name) }
  private val founded by lazy { findViewById<TextView>(R.id.founded) }

  private val df = SimpleDateFormat("dd/MM/yyyy")

  fun populate(team: Team) {
    teamName.text = team.name
    coachName.text = team.coach
    founded.text = context.getString(R.string.founded, df.format(team.founded.time))
  }
}
