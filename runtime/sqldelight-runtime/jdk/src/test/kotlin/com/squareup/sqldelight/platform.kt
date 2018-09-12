package com.squareup.sqldelight

import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.sqlite.driver.SqliteJdbcOpenHelper

fun createSqlDatabase(): SqlDatabase = SqliteJdbcOpenHelper()
