package pt.ipt.dama.muscleup.model

data class Exercise(
    val id: String,
    val name: String,
    val description: String,
    val targetMuscle: String,
    val sets: List<ExerciseSet> = emptyList(),
    val machineConfigs: List<MachineConfig> = emptyList()
)

