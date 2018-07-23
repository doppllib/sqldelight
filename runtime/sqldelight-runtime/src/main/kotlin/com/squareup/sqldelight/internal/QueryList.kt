package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query

/**
 * TODO: Get rid of if CopyOnWriteArrayList joins kotlin.collections?
 */
expect class QueryList(){
  fun addQuery(query: Query<*>)
  fun removeQuery(query: Query<*>)
  operator fun plus(other: QueryList): QueryList
  fun queries():List<Query<*>>
}

