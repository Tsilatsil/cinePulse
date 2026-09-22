// data/local/CinePulseDatabase.kt
package com.cinepulse.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.cinepulse.app.data.local.dao.*
import com.cinepulse.app.data.local.entity.*

class Converters {
    @TypeConverter fun mediaTypeToString(v: MediaType) = v.name
    @TypeConverter fun stringToMediaType(v: String) = MediaType.valueOf(v)
    @TypeConverter fun statusToString(v: WatchStatus) = v.name
    @TypeConverter fun stringToStatus(v: String) = WatchStatus.valueOf(v)
    @TypeConverter fun syncToString(v: SyncState) = v.name
    @TypeConverter fun stringToSync(v: String) = SyncState.valueOf(v)
}

@Database(
    entities = [UserMediaEntity::class, EpisodeWatchedEntity::class, PendingOpEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CinePulseDatabase : RoomDatabase() {
    abstract fun userMediaDao(): UserMediaDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun pendingOpsDao(): PendingOpsDao

    companion object {
        @Volatile private var instance: CinePulseDatabase? = null
        fun get(context: Context): CinePulseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CinePulseDatabase::class.java,
                    "cinepulse.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}