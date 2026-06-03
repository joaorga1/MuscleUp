package pt.ipt.dama.muscleup.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserEntity::class, WorkoutEntity::class, ExerciseEntity::class, ExerciseSetEntity::class, ExerciseSessionEntity::class, SessionExerciseSetEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseSetDao(): ExerciseSetDao
    abstract fun exerciseSessionDao(): ExerciseSessionDao

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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS exercise_sessions (
                        id TEXT NOT NULL PRIMARY KEY,
                        exerciseId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        finishedAt INTEGER,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_sessions_exerciseId ON exercise_sessions(exerciseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_sessions_userId ON exercise_sessions(userId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS session_exercise_sets (
                        id TEXT NOT NULL PRIMARY KEY,
                        sessionId TEXT NOT NULL,
                        reps INTEGER NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        weightKg REAL NOT NULL,
                        setOrder INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(sessionId) REFERENCES exercise_sessions(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_session_exercise_sets_sessionId ON session_exercise_sets(sessionId)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise_sessions ADD COLUMN status TEXT NOT NULL DEFAULT 'DRAFT'")
                db.execSQL("UPDATE exercise_sessions SET status = 'FINISHED' WHERE finishedAt IS NOT NULL")
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
                    .addMigrations(MIGRATION_5_6)
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration(false)
                .build().also { INSTANCE = it }
            }
        }
    }
}

