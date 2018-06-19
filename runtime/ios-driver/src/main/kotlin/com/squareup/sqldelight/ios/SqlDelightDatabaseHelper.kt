package com.squareup.sqldelight.ios

//import android.arch.persistence.db.SupportSQLiteDatabase
//import android.arch.persistence.db.SupportSQLiteOpenHelper
//import android.arch.persistence.db.SupportSQLiteProgram
//import android.arch.persistence.db.SupportSQLiteQuery
//import android.arch.persistence.db.SupportSQLiteStatement
//import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory

import co.touchlab.knarch.db.*
import co.touchlab.knarch.db.sqlite.*
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.DELETE
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.EXEC
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.UPDATE
import com.squareup.sqldelight.db.SqlResultSet

class SqlDelightDatabase(
        private val sqliteDb: SQLiteDatabase
) : SqlDatabase {
    private var transactions: SqlDelightDatabaseConnection.Transaction? = null

    override fun getConnection(): SqlDatabaseConnection {
        return SqlDelightDatabaseConnection(sqliteDb, transactions)
    }

    override fun close() {
        return sqliteDb.close()
    }

    /*class Callback(
            private val helper: SqlDatabase.Helper
    ) : SupportSQLiteOpenHelper.Callback(helper.version) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            helper.onCreate(SqlDelightDatabaseConnection(db, ThreadLocal()))
        }

        override fun onUpgrade(
                db: SupportSQLiteDatabase,
                oldVersion: Int,
                newVersion: Int
        ) {
            helper.onMigrate(SqlDelightDatabaseConnection(db, ThreadLocal()), oldVersion, newVersion)
        }
    }*/
}
/*
fun SqlDatabase.Helper.create(
        database: SupportSQLiteDatabase
): SqlDatabase {
    return SqlDelightInitializationHelper(database)
}

fun SqlDatabase.Helper.create(
        context: Context,
        name: String? = null,
        callback: SupportSQLiteOpenHelper.Callback = SqlDelightDatabaseHelper.Callback(this)
): SqlDatabase {
    val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .callback(callback)
            .name(name)
            .build()
    return SqlDelightDatabaseHelper(FrameworkSQLiteOpenHelperFactory().create(configuration))
}*/

/*private class SqlDelightInitializationHelper(
        private val database: SupportSQLiteDatabase
) : SqlDatabase {
    override fun getConnection(): SqlDatabaseConnection {
        return SqlDelightDatabaseConnection(database, ThreadLocal())
    }

    override fun close() {
        throw IllegalStateException("Tried to call close during initialization")
    }
}*/

private class SqlDelightDatabaseConnection(
        private val database: SQLiteDatabase,
        private var transactions: Transaction?
) : SqlDatabaseConnection {
    override fun newTransaction(): Transaction {
        val enclosing = transactions
        val transaction = Transaction(enclosing)
        transactions = transaction

        if (enclosing == null) {
            database.beginTransactionNonExclusive()
        }

        return transaction
    }

    override fun currentTransaction() = transactions

    inner class Transaction(
            override val enclosingTransaction: Transaction?
    ) : Transacter.Transaction() {
        override fun endTransaction(successful: Boolean) {
            if (enclosingTransaction == null) {
                if (successful) {
                    database.setTransactionSuccessful()
                    database.endTransaction()
                } else {
                    database.endTransaction()
                }
            }
            transactions = enclosingTransaction
        }
    }

    override fun prepareStatement(sql: String, type: SqlPreparedStatement.Type) = when (type) {
        SELECT -> SqlDelightQuery(sql, database)
        INSERT, UPDATE, DELETE, EXEC -> SqlDelightPreparedStatement(database.compileStatement(sql), type)
    }
}

private class SqlDelightPreparedStatement(
        private val statement: SQLiteStatement,
        private val type: SqlPreparedStatement.Type
) : SqlPreparedStatement {
    override fun bindBytes(index: Int, bytes: ByteArray?) {
        if (bytes == null) statement.bindNull(index) else statement.bindBlob(index, bytes)
    }

    override fun bindLong(index: Int, long: Long?) {
        if (long == null) statement.bindNull(index) else statement.bindLong(index, long)
    }

    override fun bindDouble(index: Int, double: Double?) {
        if (double == null) statement.bindNull(index) else statement.bindDouble(index, double)
    }

    override fun bindString(index: Int, string: String?) {
        if (string == null) statement.bindNull(index) else statement.bindString(index, string)
    }

    override fun executeQuery() = throw UnsupportedOperationException()

    override fun execute() = when (type) {
        INSERT -> statement.executeInsert()
        UPDATE, DELETE -> statement.executeUpdateDelete().toLong()
        EXEC -> {
            statement.execute()
            1
        }
        SELECT -> throw AssertionError()
    }

}

private class SqlDelightQuery(
        private val sql: String,
        private val database: SQLiteDatabase
) : SqlPreparedStatement {
    private val binds: MutableMap<Int, (SQLiteProgram) -> Unit> = LinkedHashMap()

    override fun bindBytes(index: Int, bytes: ByteArray?) {
        binds[index] = { if (bytes == null) it.bindNull(index) else it.bindBlob(index, bytes) }
    }

    override fun bindLong(index: Int, long: Long?) {
        binds[index] = { if (long == null) it.bindNull(index) else it.bindLong(index, long) }
    }

    override fun bindDouble(index: Int, double: Double?) {
        binds[index] = { if (double == null) it.bindNull(index) else it.bindDouble(index, double) }
    }

    override fun bindString(index: Int, string: String?) {
        binds[index] = { if (string == null) it.bindNull(index) else it.bindString(index, string) }
    }

    override fun execute() = throw UnsupportedOperationException()

    override fun executeQuery():SqlResultSet {

        val cursorFactory = object : SQLiteDatabase.CursorFactory {
            override fun newCursor(db: SQLiteDatabase,
                          masterQuery: SQLiteCursorDriver, editTable: String?,
                          query: SQLiteQuery): Cursor {
                for (action in binds.values) {
                    action(query)
                }

                return SQLiteCursor(masterQuery, editTable, query)
            }
        }

        return SqlDelightResultSet(database.rawQueryWithFactory(
                cursorFactory,
                sql,
                null,
                null))
    }

    override fun toString() = sql
}




private class SqlDelightResultSet(
        private val cursor: Cursor
) : SqlResultSet {
    override fun next() = cursor.moveToNext()
    override fun getString(index: Int) = cursor.getString(index)
    override fun getLong(index: Int) = cursor.getLong(index)
    override fun getBytes(index: Int) = cursor.getBlob(index)
    override fun getDouble(index: Int) = cursor.getDouble(index)
    override fun close() = cursor.close()
}