package pt.ipt.dama.muscleup

import android.app.Application
import pt.ipt.dama.muscleup.data.local.AppDatabase

class MuscleUpApp : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }
}

