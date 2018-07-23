package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query
import konan.worker.*

/**
 * TODO: Get rid of if CopyOnWriteArrayList joins kotlin.collections?
 */
actual class QueryList actual constructor(){
    private var queryList = AtomicReference<List<Query<*>>>(emptyList())

    actual fun addQuery(query: Query<*>) {
        val oldList = queryList.get()!!
        val newList = ArrayList(oldList)
        newList += query
        queryList.compareAndSwap(oldList, newList.freeze())
    }

    actual fun removeQuery(query: Query<*>) {
        val oldList = queryList.get()!!
        val newList = ArrayList(oldList)
        newList -= query
        queryList.compareAndSwap(oldList, newList.freeze())
    }

    actual operator fun plus(other: QueryList): QueryList {
        val oldList = queryList.get()!!
        val otherList = other.queryList.get()!!
        val al = ArrayList(oldList)
        al += otherList
        val result = QueryList()
        result.queryList.compareAndSwap(result.queryList.get()!!, al.freeze())
        return result
    }

    actual fun queries():List<Query<*>> = queryList.get()!!
}
