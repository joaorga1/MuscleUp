package pt.ipt.dama.muscleup.data.local

/**
 * Responsável por popular a base de dados com dados de demonstração.
 * Chamado apenas uma vez por utilizador, quando não existem treinos na BD.
 * Para re-popular: apagar os dados da app no telemóvel e fazer login novamente.
 */
object DatabaseSeeder {

    suspend fun seed(db: AppDatabase, userId: String) {
        val workoutDao = db.workoutDao()
        val exerciseDao = db.exerciseDao()

        // Só popula se o utilizador ainda não tiver treinos
        if (workoutDao.getCountForUser(userId) > 0) return

        // IDs únicos por utilizador para evitar conflitos entre contas
        val wPush  = java.util.UUID.randomUUID().toString()
        val wPull  = java.util.UUID.randomUUID().toString()
        val wCardio = java.util.UUID.randomUUID().toString()

        // Treinos
        workoutDao.insert(WorkoutEntity(wPush,   userId, "Push Day",      "Peito, ombros e tríceps",    "FORCA"))
        workoutDao.insert(WorkoutEntity(wPull,   userId, "Pull Day",      "Costas e bíceps",            "FORCA"))
        workoutDao.insert(WorkoutEntity(wCardio, userId, "Core & Cardio", "Abdominais e resistência",   "CARDIO"))

        // Exercícios
        exerciseDao.insert(ExerciseEntity(java.util.UUID.randomUUID().toString(), wPush,   "Supino Plano",   "Exercício composto para peito",   "Peito"))
        exerciseDao.insert(ExerciseEntity(java.util.UUID.randomUUID().toString(), wPush,   "Press Militar",  "Exercício composto para ombros",  "Ombros"))
        exerciseDao.insert(ExerciseEntity(java.util.UUID.randomUUID().toString(), wPull,   "Remada Curvada", "Exercício composto para costas",  "Costas"))
        exerciseDao.insert(ExerciseEntity(java.util.UUID.randomUUID().toString(), wCardio, "Prancha",        "Isométrico para core",            "Core"))
    }
}

