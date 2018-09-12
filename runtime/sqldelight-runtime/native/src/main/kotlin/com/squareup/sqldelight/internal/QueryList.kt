package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query
import kotlin.native.*
import kotlin.native.concurrent.*

/**
 * TODO: Get rid of if CopyOnWriteArrayList joins kotlin.collections?
 */
actual class QueryList actual constructor(){
    private var queryList = AtomicReference<List<Query<*>>>(emptyList())

    actual fun addQuery(query: Query<*>) {
        val oldList = queryList.value
        val newList = ArrayList(oldList)
        newList += query
        queryList.compareAndSwap(oldList, newList.freeze())
    }

    actual fun removeQuery(query: Query<*>) {
        val oldList = queryList.value
        val newList = ArrayList(oldList)
        newList -= query
        queryList.compareAndSwap(oldList, newList.freeze())
    }

    actual operator fun plus(other: QueryList): QueryList {
        val oldList = queryList.value
        val otherList = other.queryList.value
        val al = ArrayList(oldList)
        al += otherList
        val result = QueryList()
        result.queryList.compareAndSwap(result.queryList.value, al.freeze())
        return result
    }

    actual fun queries():List<Query<*>> = queryList.value
}
