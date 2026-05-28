package pt.ipt.dama.muscleup.model

import androidx.annotation.StringRes
import pt.ipt.dama.muscleup.R

enum class WorkoutType(@param:StringRes val labelRes: Int) {
    FORCA(R.string.workout_type_forca),
    HIPERTROFIA(R.string.workout_type_hipertrofia),
    CARDIO(R.string.workout_type_cardio),
    MOBILIDADE(R.string.workout_type_mobilidade),
    HIIT(R.string.workout_type_hiit),
    FULL_BODY(R.string.workout_type_full_body)
}
