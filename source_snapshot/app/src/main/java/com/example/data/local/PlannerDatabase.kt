package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FolderEntity::class, LessonPlanEntity::class, LessonTemplateEntity::class],
    version = 6,
    exportSchema = false
)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun plannerDao(): PlannerDao

    companion object {
        @Volatile
        private var INSTANCE: PlannerDatabase? = null

        fun getDatabase(context: Context): PlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlannerDatabase::class.java,
                    "planner_database"
                )
                .addMigrations(
                    object : androidx.room.migration.Migration(1, 5) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            addColumnIfNotExists(db, "folders", "isArchived", "INTEGER NOT NULL DEFAULT 0")
                            addColumnIfNotExists(db, "lesson_plans", "isFavorite", "INTEGER NOT NULL DEFAULT 0")
                            addColumnIfNotExists(db, "lesson_plans", "tags", "TEXT NOT NULL DEFAULT ''")
                            addColumnIfNotExists(db, "lesson_plans", "durationMinutes", "INTEGER NOT NULL DEFAULT 45")
                        }
                    },
                    object : androidx.room.migration.Migration(2, 5) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            addColumnIfNotExists(db, "folders", "isArchived", "INTEGER NOT NULL DEFAULT 0")
                            addColumnIfNotExists(db, "lesson_plans", "tags", "TEXT NOT NULL DEFAULT ''")
                            addColumnIfNotExists(db, "lesson_plans", "durationMinutes", "INTEGER NOT NULL DEFAULT 45")
                        }
                    },
                    object : androidx.room.migration.Migration(3, 5) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            addColumnIfNotExists(db, "folders", "isArchived", "INTEGER NOT NULL DEFAULT 0")
                            addColumnIfNotExists(db, "lesson_plans", "tags", "TEXT NOT NULL DEFAULT ''")
                            addColumnIfNotExists(db, "lesson_plans", "durationMinutes", "INTEGER NOT NULL DEFAULT 45")
                        }
                    },
                    object : androidx.room.migration.Migration(4, 5) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            addColumnIfNotExists(db, "lesson_plans", "durationMinutes", "INTEGER NOT NULL DEFAULT 45")
                        }
                    },
                    object : androidx.room.migration.Migration(5, 6) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS `lesson_templates` (
                                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    `name` TEXT NOT NULL,
                                    `useSecondClass` INTEGER NOT NULL,
                                    `classASubject` TEXT NOT NULL,
                                    `classAGrade` TEXT NOT NULL,
                                    `classATopic` TEXT NOT NULL,
                                    `classBSubject` TEXT NOT NULL,
                                    `classBGrade` TEXT NOT NULL,
                                    `classBTopic` TEXT NOT NULL,
                                    `classAIndependentWorkLimit` TEXT NOT NULL,
                                    `classBIndependentWorkLimit` TEXT NOT NULL,
                                    `planMode` TEXT NOT NULL,
                                    `additionalInstructions` TEXT NOT NULL,
                                    `lessonDuration` TEXT NOT NULL,
                                    `lessonType` TEXT NOT NULL,
                                    `updatedAt` INTEGER NOT NULL
                                )
                                """.trimIndent()
                            )
                            db.execSQL(
                                "CREATE UNIQUE INDEX IF NOT EXISTS `index_lesson_templates_name` ON `lesson_templates` (`name`)"
                            )
                        }
                    }
                )
                .build()
                INSTANCE = instance
                instance
            }
        }

        private fun addColumnIfNotExists(db: SupportSQLiteDatabase, tableName: String, columnName: String, columnDefinition: String) {
            val cursor = db.query("PRAGMA table_info($tableName)")
            var exists = false
            while (cursor.moveToNext()) {
                val name = cursor.getString(1)
                if (name == columnName) {
                    exists = true
                    break
                }
            }
            cursor.close()
            if (!exists) {
                db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName $columnDefinition")
            }
        }
    }
}
