package com.gh.healthapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HealthRecordEntity::class,
        DailySummaryEntity::class,
        SyncStateEntity::class,
        WalkSessionEntity::class,
        WalkPointEntity::class,
        ChatMessageEntity::class,
        KbChunkEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun dao(): HealthDao
    abstract fun walkDao(): WalkDao
    abstract fun chatDao(): ChatDao
    abstract fun kbDao(): KbDao

    companion object {
        @Volatile private var instance: HealthDatabase? = null

        /** v1 → v2: 걷기 테이블 2개 + daily_summary.walkDistanceM (기존 데이터는 그대로 유지) */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `walk_session` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`startTime` INTEGER NOT NULL, " +
                        "`endTime` INTEGER, " +
                        "`distanceM` REAL NOT NULL, " +
                        "`activeMs` INTEGER NOT NULL, " +
                        "`steps` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `walk_point` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`sessionId` INTEGER NOT NULL, " +
                        "`time` INTEGER NOT NULL, " +
                        "`lat` REAL NOT NULL, " +
                        "`lon` REAL NOT NULL, " +
                        "`accuracy` REAL NOT NULL, " +
                        "`altitude` REAL, " +
                        "FOREIGN KEY(`sessionId`) REFERENCES `walk_session`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_walk_point_sessionId` ON `walk_point` (`sessionId`)")
                db.execSQL("ALTER TABLE `daily_summary` ADD COLUMN `walkDistanceM` REAL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `chat_message` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `role` TEXT NOT NULL, `text` TEXT NOT NULL, `time` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chat_message` ADD COLUMN `sourcesJson` TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS `kb_chunk` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `docId` TEXT NOT NULL, `title` TEXT NOT NULL, `heading` TEXT NOT NULL, `text` TEXT NOT NULL, `source` TEXT NOT NULL, `sourceUrl` TEXT, `vector` BLOB NOT NULL, `contentHash` TEXT NOT NULL)")
            }
        }

        fun get(context: Context): HealthDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "health.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also {
                    instance = it
                    com.gh.healthapp.chat.KnowledgeBase.start(context.applicationContext)
                }
            }
    }
}
