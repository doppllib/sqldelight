package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query
import konan.worker.*

actual class ListenerCollection actual constructor(query: Query<*>, queries:QueryList){
    private val listeners = AtomicReference<Set<Query.Listener>>(mutableSetOf<Query.Listener>().freeze())
    private val queries = queries
    private val query = query

    actual fun notifyResultSetChanged() {
        synchronized(listeners) {
            listeners.get()!!.forEach(Query.Listener::queryResultsChanged)
        }
    }

    /**
     * Register a listener to be notified of future changes in the result set.
     */
    actual fun addListener(listener: Query.Listener) {
        val oldSet = listeners.get()!!
        if (oldSet.isEmpty()) queries.addQuery(query)
        val newSet = HashSet(oldSet)
        newSet.add(listener)
        listeners.compareAndSwap(oldSet, newSet.freeze())
    }

    actual fun removeListener(listener: Query.Listener) {
        val oldSet = listeners.get()!!
        val newSet = HashSet(oldSet)
        newSet.remove(listener)
        if (newSet.isEmpty()) queries.removeQuery(query)
        listeners.compareAndSwap(oldSet, newSet.freeze())
    }
}