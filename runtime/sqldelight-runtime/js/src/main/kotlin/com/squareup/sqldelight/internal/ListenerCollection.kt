package com.squareup.sqldelight.internal

import com.squareup.sqldelight.Query

actual class ListenerCollection actual constructor(private val query: Query<*>, private val queries: QueryList){
    private val listeners = mutableSetOf<Query.Listener>()

    actual fun notifyResultSetChanged() {
        synchronized(listeners) {
            listeners.forEach(Query.Listener::queryResultsChanged)
        }
    }

    /**
     * Register a listener to be notified of future changes in the result set.
     */
    actual fun addListener(listener: Query.Listener) {
        synchronized(listeners) {
            if (listeners.isEmpty()) queries.addQuery(query)
            listeners.add(listener)
        }
    }

    actual fun removeListener(listener: Query.Listener) {
        synchronized(listeners) {
            listeners.remove(listener)
            if (listeners.isEmpty()) queries.removeQuery(query)
        }
    }
}