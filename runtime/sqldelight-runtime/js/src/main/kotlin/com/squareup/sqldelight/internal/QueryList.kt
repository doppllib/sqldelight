package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query

/**
 * TODO: Get rid of if CopyOnWriteArrayList joins kotlin.collections?
 */
actual class QueryList actual constructor(){
    private var queryList: List<Query<*>> = emptyList()

    actual fun addQuery(query: Query<*>) {
        synchronized(queryList) {
            queryList += query
        }
    }

    actual fun removeQuery(query: Query<*>) {
        synchronized(queryList) {
            queryList -= query
        }
    }

    actual operator fun plus(other: QueryList): QueryList {
        val result = QueryList()
        result.queryList = queryList + other.queryList
        return result
    }

    actual fun queries():List<Query<*>> = queryList
}

