package pt.ipt.dama.muscleup.domain.model

data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val targetMuscle: String,
    val sets: List<ExerciseSet> = emptyList<ExerciseSet>(),
    val machineConfigs: List<MachineConfig> = emptyList<MachineConfig>()
)


