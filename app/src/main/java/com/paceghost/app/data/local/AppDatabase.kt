package com.paceghost.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun walkDao(): WalkDao

    companion object {
        const val DATABASE_NAME = "paceghost.db"

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
            var dbInstance: AppDatabase? = null
            val database = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            ).addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    coroutineScope.launch {
                        dbInstance?.let { populateDemoWalk(it) }
                    }
                }
            }).build()

            dbInstance = database
            return database
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
