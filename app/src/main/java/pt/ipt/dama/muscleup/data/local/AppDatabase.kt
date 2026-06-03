package pt.ipt.dama.muscleup.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserEntity::class, WorkoutEntity::class, ExerciseEntity::class, ExerciseSetEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseSetDao(): ExerciseSetDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS exercise_sets (
                        id TEXT NOT NULL PRIMARY KEY,
                        exerciseId TEXT NOT NULL,
                        targetType TEXT NOT NULL,
                        reps INTEGER NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        weightKg REAL NOT NULL,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_sets_exerciseId ON exercise_sets(exerciseId)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise_sets ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    UPDATE exercise_sets
                    SET createdAt = rowid
                    WHERE createdAt = 0
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS exercise_sets_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        exerciseId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        seriesOrder INTEGER NOT NULL,
                        reps INTEGER NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        weightKg REAL NOT NULL,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO exercise_sets_new (id, exerciseId, createdAt, seriesOrder, reps, durationSeconds, weightKg)
                    SELECT
                        a.id,
                        a.exerciseId,
                        a.createdAt,
                        (
                            SELECT COUNT(*)
                            FROM exercise_sets b
                            WHERE b.exerciseId = a.exerciseId
                              AND (
                                  b.createdAt < a.createdAt
                                  OR (b.createdAt = a.createdAt AND b.rowid <= a.rowid)
                              )
                        ) AS seriesOrder,
                        a.reps,
                        a.durationSeconds,
                        a.weightKg
                    FROM exercise_sets a
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE exercise_sets")
                db.execSQL("ALTER TABLE exercise_sets_new RENAME TO exercise_sets")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_sets_exerciseId ON exercise_sets(exerciseId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "muscleup.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration(false)
                .build().also { INSTANCE = it }
            }
        }
    }
}

