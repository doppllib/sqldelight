package com.squareup.sqldelight.multiplatform

import co.touchlab.knarch.db.sqlite.*

actual fun createSQLiteCursor(masterQuery: SQLiteCursorDriver, query: SQLiteQuery):co.touchlab.knarch.db.Cursor{
    return SQLiteCursor(masterQuery, query)
}