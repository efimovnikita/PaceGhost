package com.paceghost.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.data.local.entity.WalkEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WalkEntity::class,
        TrackPointEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun walkDao(): WalkDao

    companion object {
        const val DATABASE_NAME = "paceghost.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_track_points_latitude_longitude` ON `track_points` (`latitude`, `longitude`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(
            context: Context,
            coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext, coroutineScope).also {
                    INSTANCE = it
                }
            }
        }

        private fun buildDatabase(context: Context, coroutineScope: CoroutineScope): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        suspend fun populateDemoWalk(database: AppDatabase) {
            val dao = database.walkDao()
            if (dao.getWalkCount() == 0) {
                val (walk, points) = DemoDataGenerator.createDemoWalkData()
                val walkId = dao.insertWalk(walk)
                val pointsWithWalkId = points.map { it.copy(walkId = walkId) }
                dao.insertTrackPoints(pointsWithWalkId)
            }
        }
    }
}
