package com.squareup.sqldelight.multiplatform

import co.touchlab.multiplatform.architecture.db.Cursor
import co.touchlab.multiplatform.architecture.db.sqlite.*
import co.touchlab.multiplatform.architecture.threads.*
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlDatabase
import com.squareup.sqldelight.db.SqlDatabaseConnection
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.db.SqlResultSet
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.DELETE
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.EXEC
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.INSERT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.SELECT
import com.squareup.sqldelight.db.SqlPreparedStatement.Type.UPDATE

class SqlDelightDatabaseHelper(
        private val openHelper: SQLiteOpenHelper
) : SqlDatabase {
    private val transactions = ThreadLocalReference<SqlDelightDatabaseConnection.Transaction>()

    override fun getConnection(): SqlDatabaseConnection {
        return SqlDelightDatabaseConnection(openHelper.getWritableDatabase(), transactions)
    }

    override fun close() {
        return openHelper.close()
    }

    class Callback(
            private val helper: SqlDatabase.Helper
    ) : PlatformSQLiteOpenHelperCallback(helper.version) {
        override fun onCreate(db: SQLiteDatabase) {
            helper.onCreate(SqlDelightDatabaseConnection(db, ThreadLocalReference()))
        }

        override fun onUpgrade(
                db: SQLiteDatabase,
                oldVersion: Int,
                newVersion: Int
        ) {
            helper.onMigrate(SqlDelightDatabaseConnection(db, ThreadLocalReference()), oldVersion, newVersion)
        }
    }
}

/**
 * Wraps [database] into a [SqlDatabase] usable by a SqlDelight generated QueryWrapper.
 */
fun SqlDatabase.Helper.create(
        database: SQLiteDatabase
): SqlDatabase {
    return SqlDelightInitializationHelper(database)
}

/**
 * Wraps [context] into a [SqlDatabase] usable by a SqlDelight generated QueryWrapper.
 */
fun SqlDatabase.Helper.create(
        name: String? = null,
        openHelperFactory: NativeOpenHelperFactory,
        callback: PlatformSQLiteOpenHelperCallback = SqlDelightDatabaseHelper.Callback(this)
): SqlDatabase {
    return SqlDelightDatabaseHelper(openHelperFactory.createOpenHelper(name, callback, null))
}

private class SqlDelightInitializationHelper(
        private val database: SQLiteDatabase
) : SqlDatabase {
    override fun getConnection(): SqlDatabaseConnection {
        return SqlDelightDatabaseConnection(database, ThreadLocalReference())
    }

    override fun close() {
        throw IllegalStateException("Tried to call close during initialization")
    }
}

private class SqlDelightDatabaseConnection(
        private val database: SQLiteDatabase,
        private val transactions: ThreadLocalReference<Transaction>
) : SqlDatabaseConnection {
    override fun newTransaction(): Transaction {
        val enclosing = transactions.get()
        val transaction = Transaction(enclosing)
        transactions.set(transaction)

        if (enclosing == null) {
            database.beginTransactionNonExclusive()
        }

        return transaction
    }

    override fun currentTransaction() = transactions.get()

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
            transactions.set(enclosingTransaction)
        }
    }

    override fun prepareStatement(
            sql: String,
            type: SqlPreparedStatement.Type,
            parameters: Int) = when(type) {
        SELECT -> SqlDelightQuery(sql, database)
        INSERT, UPDATE, DELETE, EXEC -> SqlDelightPreparedStatement({database.compileStatement(sql)}, type)
    }
}

private class SqlDelightPreparedStatement(
        private val statementFactory: () -> SQLiteStatement,
        private val type: SqlPreparedStatement.Type
) : SqlPreparedStatement {
    private val statementLocal = ThreadLocalReference<SQLiteStatement>()

    private fun statement():SQLiteStatement{
        val stmt = statementLocal.get()
        if(stmt == null){
            statementLocal.set(statementFactory())
        }
        return statementLocal.get()!!
    }

    override fun bindBytes(index: Int, bytes: ByteArray?) {
        if (bytes == null) statement().bindNull(index) else statement().bindBlob(index, bytes)
    }

    override fun bindLong(index: Int, long: Long?) {
        if (long == null) statement().bindNull(index) else statement().bindLong(index, long)
    }

    override fun bindDouble(index: Int, double: Double?) {
        if (double == null) statement().bindNull(index) else statement().bindDouble(index, double)
    }

    override fun bindString(index: Int, string: String?) {
        if (string == null) statement().bindNull(index) else statement().bindString(index, string)
    }

    override fun executeQuery() = throw UnsupportedOperationException()

    override fun execute() = when (type) {
        INSERT -> statement().executeInsert()
        UPDATE, DELETE -> statement().executeUpdateDelete().toLong()
        EXEC -> {
            statement().execute()
            1
        }
        SELECT -> throw AssertionError()
    }

}

private class SqlDelightQuery(
        private val sql: String,
        private val database: SQLiteDatabase
) : SqlPreparedStatement {
    private val bindsRef: AtomicRef<List<(SQLiteProgram) -> Unit>> = AtomicRef(ArrayList())

    private fun addBind(action:((SQLiteProgram) -> Unit)){
        val oldValue = bindsRef.getValue()!!
        val newMap = ArrayList<(SQLiteProgram) -> Unit>(oldValue)
        newMap.add(action)
        bindsRef.compareAndSwapValue(oldValue, newMap)
    }
    override fun bindBytes(index: Int, bytes: ByteArray?) {
        addBind { if (bytes == null) it.bindNull(index) else it.bindBlob(index, bytes) }
    }

    override fun bindLong(index: Int, long: Long?) {
        addBind { if (long == null) it.bindNull(index) else it.bindLong(index, long) }
    }

    override fun bindDouble(index: Int, double: Double?) {
        addBind { if (double == null) it.bindNull(index) else it.bindDouble(index, double) }
    }

    override fun bindString(index: Int, string: String?) {
        addBind { if (string == null) it.bindNull(index) else it.bindString(index, string) }
    }

    override fun execute() = throw UnsupportedOperationException()

    override fun executeQuery() :SqlResultSet {

        val cursorFactory = object : CursorFactory {
            override fun newCursor(db: SQLiteDatabase,
                                   masterQuery: SQLiteCursorDriver, editTable: String?,
                                   query: SQLiteQuery): Cursor {
                for (action in bindsRef.getValue()!!) {
                    action(query)
                }

                return createSQLiteCursor(masterQuery, query)
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
