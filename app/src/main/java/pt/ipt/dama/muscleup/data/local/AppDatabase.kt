package pt.ipt.dama.muscleup.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de dados local (Room) da aplicação, usada como cache offline de todas as entidades
 * e como suporte à fila de sincronização com a API remota.
 */
@Database(
    entities = [UserEntity::class, WorkoutEntity::class, ExerciseEntity::class, ExerciseSetEntity::class, ExerciseSessionEntity::class, SessionExerciseSetEntity::class, MachineConfigEntity::class, ExercisePhotoEntity::class, PendingSyncEntity::class],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseSetDao(): ExerciseSetDao
    abstract fun exerciseSessionDao(): ExerciseSessionDao
    abstract fun machineConfigDao(): MachineConfigDao
    abstract fun exercisePhotoDao(): ExercisePhotoDao
    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** Cria a tabela de séries de exercício (exercise_sets). */
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

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop old table if it exists (may have wrong schema from a previous attempt)
                db.execSQL("DROP TABLE IF EXISTS machine_configs")
                db.execSQL("DROP INDEX IF EXISTS index_machine_configs_exerciseId")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS machine_configs (
                        id TEXT NOT NULL PRIMARY KEY,
                        exerciseId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_machine_configs_exerciseId ON machine_configs(exerciseId)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN profilePhotoUri TEXT")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS exercise_photos (
                        id TEXT NOT NULL PRIMARY KEY,
                        exerciseId TEXT NOT NULL,
                        uri TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercise_photos_exerciseId ON exercise_photos(exerciseId)")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Defensivo: alguns dispositivos (instalação fresca) já podem ter esta coluna
                // (o schema da entidade já a incluía antes desta migração ser alcançável).
                val cursor = db.query("PRAGMA table_info(machine_configs)")
                var hasColumn = false
                cursor.use {
                    val nameIndex = it.getColumnIndex("name")
                    while (it.moveToNext()) {
                        if (it.getString(nameIndex) == "angleDegrees") {
                            hasColumn = true
                            break
                        }
                    }
                }
                if (!hasColumn) {
                    db.execSQL("ALTER TABLE machine_configs ADD COLUMN angleDegrees REAL")
                }
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workouts ADD COLUMN remoteId TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_sync (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        entityType TEXT NOT NULL,
                        operation TEXT NOT NULL,
                        localId TEXT NOT NULL,
                        payloadJson TEXT,
                        createdAt INTEGER NOT NULL,
                        attempts INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN remoteId TEXT")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Machine Configs, Exercise Photos e Exercise Sessions/Session Sets.
                db.execSQL("ALTER TABLE exercise_sets ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE machine_configs ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE exercise_photos ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE exercise_sessions ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE session_exercise_sets ADD COLUMN remoteId TEXT")
            }
        }

        /**
         * Devolve a instância única (singleton) da base de dados, aplicando todas as
         * migrações necessárias entre versões do esquema.
         */
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
                    .addMigrations(MIGRATION_7_8)
                    .addMigrations(MIGRATION_8_9)
                    .addMigrations(MIGRATION_9_10)
                    .addMigrations(MIGRATION_10_11)
                    .addMigrations(MIGRATION_11_12)
                    .addMigrations(MIGRATION_12_13)
                    .addMigrations(MIGRATION_13_14)
                    .fallbackToDestructiveMigration(false)
                .build().also { INSTANCE = it }
            }
        }
    }
}

