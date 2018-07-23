package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query

expect class ListenerCollection(query: Query<*>, queries:QueryList){
    fun notifyResultSetChanged()
    fun addListener(listener: Query.Listener)
    fun removeListener(listener: Query.Listener)
}