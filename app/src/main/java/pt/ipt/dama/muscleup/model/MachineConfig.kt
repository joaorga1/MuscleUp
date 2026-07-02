package pt.ipt.dama.muscleup.model

data class MachineConfig(
    val id: String,
    val name: String,
    val description: String,
    val angleDegrees: Float? = null
)
