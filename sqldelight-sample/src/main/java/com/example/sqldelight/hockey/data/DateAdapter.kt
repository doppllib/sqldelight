package com.example.sqldelight.hockey.data

import com.squareup.sqldelight.ColumnAdapter
import java.util.Calendar

class DateAdapter : ColumnAdapter<Calendar, Long> {
  override fun encode(value: Calendar): Long {
    return value.timeInMillis
  }

  override fun decode(databaseValue: Long): Calendar {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = databaseValue
    return calendar
  }
}
