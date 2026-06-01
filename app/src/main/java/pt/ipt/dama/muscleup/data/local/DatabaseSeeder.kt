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

        // Treinos
        workoutDao.insert(WorkoutEntity("w1", userId, "Push Day", "Peito, ombros e tríceps", "FORCA"))
        workoutDao.insert(WorkoutEntity("w2", userId, "Pull Day", "Costas e bíceps", "FORCA"))
        workoutDao.insert(WorkoutEntity("w3", userId, "Core & Cardio", "Abdominais e resistência", "CARDIO"))

        // Exercícios
        exerciseDao.insert(ExerciseEntity("e1", "w1", "Supino Plano", "Exercício composto para peito", "Peito"))
        exerciseDao.insert(ExerciseEntity("e2", "w1", "Press Militar", "Exercício composto para ombros", "Ombros"))
        exerciseDao.insert(ExerciseEntity("e3", "w2", "Remada Curvada", "Exercício composto para costas", "Costas"))
        exerciseDao.insert(ExerciseEntity("e4", "w3", "Prancha", "Isométrico para core", "Core"))
    }
}

