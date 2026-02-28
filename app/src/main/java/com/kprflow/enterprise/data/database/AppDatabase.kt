package com.kprflow.enterprise.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import com.kprflow.enterprise.data.database.converters.DateConverters
import com.kprflow.enterprise.data.database.dao.OfflineDossierDao
import com.kprflow.enterprise.data.database.dao.OfflineDocumentDao
import com.kprflow.enterprise.data.database.dao.OfflinePaymentDao
import com.kprflow.enterprise.data.database.dao.OfflineUnitDao
import com.kprflow.enterprise.data.database.dao.SyncQueueDao
import com.kprflow.enterprise.data.database.entities.OfflineDocumentEntity
import com.kprflow.enterprise.data.database.entities.OfflineDossierEntity
import com.kprflow.enterprise.data.database.entities.OfflinePaymentEntity
import com.kprflow.enterprise.data.database.entities.OfflineUnitEntity
import com.kprflow.enterprise.data.database.entities.SyncQueueEntity
import android.content.Context
import javax.inject.Inject

@Database(
    entities = [
        OfflineUnitEntity::class,
        OfflineDossierEntity::class,
        OfflineDocumentEntity::class,
        OfflinePaymentEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun offlineUnitDao(): OfflineUnitDao
    abstract fun offlineDossierDao(): OfflineDossierDao
    abstract fun offlineDocumentDao(): OfflineDocumentDao
    abstract fun offlinePaymentDao(): OfflinePaymentDao
    abstract fun syncQueueDao(): SyncQueueDao
}

object DatabaseMigrations {
    
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            // Add new columns or tables for future versions
            connection.execSQL("""
                ALTER TABLE offline_dossiers 
                ADD COLUMN priority INTEGER DEFAULT 0
            """.trimIndent())
        }
    }
}
