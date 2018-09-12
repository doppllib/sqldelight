package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query
import kotlin.native.*
import kotlin.native.concurrent.*

actual class ListenerCollection actual constructor(query: Query<*>, queries:QueryList){
    private val listeners = AtomicReference<Set<Query.Listener>>(mutableSetOf<Query.Listener>().freeze())
    private val queries = queries
    private val query = query

    actual fun notifyResultSetChanged() {
        synchronized(listeners) {
            listeners.value.forEach(Query.Listener::queryResultsChanged)
        }
    }

    /**
     * Register a listener to be notified of future changes in the result set.
     */
    actual fun addListener(listener: Query.Listener) {
        val oldSet = listeners.value
        if (oldSet.isEmpty()) queries.addQuery(query)
        val newSet = HashSet(oldSet)
        newSet.add(listener)
        listeners.compareAndSwap(oldSet, newSet.freeze())
    }

    actual fun removeListener(listener: Query.Listener) {
        val oldSet = listeners.value
        val newSet = HashSet(oldSet)
        newSet.remove(listener)
        if (newSet.isEmpty()) queries.removeQuery(query)
        listeners.compareAndSwap(oldSet, newSet.freeze())
    }
}