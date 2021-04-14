package org.odk.collect.android.database

import android.database.sqlite.SQLiteDatabase
import org.odk.collect.android.storage.StoragePathProvider
import javax.inject.Singleton

/**
 * Holds "connection" (in this case an instance of [android.database.sqlite.SQLiteOpenHelper]
 * to the Instances database. According to the Android team these should be kept open for the whole apps
 * lifecycle.
 *
 * @see [https://stackoverflow.com/questions/6608498/best-place-to-close-database-connection](https://stackoverflow.com/questions/6608498/best-place-to-close-database-connection)
 */
@Singleton
class InstancesDatabaseProvider {

    val writeableDatabase: SQLiteDatabase
        get() = dbHelper.writableDatabase
    val readableDatabase: SQLiteDatabase
        get() = dbHelper.readableDatabase

    private var _dbHelper: InstancesDatabaseHelper? = null
    private val dbHelper: InstancesDatabaseHelper
        get() = synchronized(this) {
            return _dbHelper ?: recreateDatabaseHelper()
        }

    fun recreateDatabaseHelper(): InstancesDatabaseHelper {
        return InstancesDatabaseHelper(InstanceDatabaseMigrator(), StoragePathProvider()).also {
            _dbHelper = it
        }
    }

    fun releaseDatabaseHelper() {
        _dbHelper?.close()
        _dbHelper = null
    }
}
