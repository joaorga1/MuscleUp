package pt.ipt.dama.muscleup.model

/** Modelo de domínio que representa uma configuração de máquina de um exercício. */
data class MachineConfig(

    val id: String,
    val name: String,
    val description: String,
    val angleDegrees: Float? = null
)
