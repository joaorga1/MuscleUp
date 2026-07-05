package pt.ipt.dama.muscleup.ui.screens.workout

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.ipt.dama.muscleup.MuscleUpApp
import pt.ipt.dama.muscleup.R
import pt.ipt.dama.muscleup.data.local.AppDatabase
import pt.ipt.dama.muscleup.data.local.ExerciseEntity
import pt.ipt.dama.muscleup.data.local.toModel
import pt.ipt.dama.muscleup.data.session.UserSession
import pt.ipt.dama.muscleup.model.Workout
import java.util.UUID

/**
 * ViewModel do ecrã de detalhe de um treino.
 *
 * Gere a lista de exercícios associados ao treino identificado por [workoutId], incluindo
 * criação, edição e remoção. Sincroniza automaticamente com a API no arranque, esvaziando
 * primeiro a fila de operações pendentes antes de fazer pull dos dados remotos.
 *
 * @param application Contexto da aplicação, necessário para aceder à base de dados e ao syncManager.
 * @param workoutId Identificador do treino cujos exercícios devem ser geridos.
 */
class WorkoutViewModel(
    application: Application,
    val workoutId: String
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val workoutDao = db.workoutDao()
    private val exerciseDao = db.exerciseDao()
    private val userDao = db.userDao()

    init {
        // esvazia primeiro a fila pendente (para não "ressuscitar" exercises
        // apagados offline) e só depois traz da API os que ainda não existem localmente.
        viewModelScope.launch {
            val app = getApplication<MuscleUpApp>()
            try { app.syncManager.syncPending() } catch (_: Exception) { /* offline: ignorar */ }
            app.syncManager.pullExercises(workoutId)
        }
    }

    val userName: String get() = UserSession.currentUserName

    val profilePhotoUri: StateFlow<String?> = userDao
        .getUserByEmailFlow(UserSession.currentUserEmail)
        .map { it?.profilePhotoUri?.ifBlank { null } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val workout: StateFlow<Workout?> = combine(
        workoutDao.getWorkoutById(workoutId),
        exerciseDao.getExercisesForWorkout(workoutId)
    ) { workoutEntity, exercises ->
        workoutEntity?.toModel(exercises.map { it.toModel() })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

    /** Adiciona um novo exercício ao treino. Rejeita nomes duplicados dentro do mesmo treino. */
    fun addExercise(name: String, description: String, targetMuscle: String) {
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            try {
                if (exerciseDao.isDuplicate(workoutId, null, name.trim())) {
                    _uiEvent.emit(app.getString(R.string.workout_error_duplicate_exercise))
                    return@launch
                }
                val entity = ExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    workoutId = workoutId,
                    name = name.trim(),
                    description = description.trim(),
                    targetMuscle = targetMuscle.trim()
                )
                exerciseDao.insert(entity)
                app.syncManager.onExerciseCreated(entity)
                app.triggerSync()
                _navigateBack.emit(Unit)
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.workout_error_save_exercise))
            }
        }
    }

    /** Atualiza os dados de um exercício existente. Rejeita nomes duplicados e preserva o `remoteId` já atribuído. */
    fun editExercise(exerciseId: String, name: String, description: String, targetMuscle: String) {
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            try {
                if (exerciseDao.isDuplicate(workoutId, exerciseId, name.trim())) {
                    _uiEvent.emit(app.getString(R.string.workout_error_duplicate_exercise))
                    return@launch
                }
                // Preserva o remoteId já atribuído (senão o próximo sync trataria isto como um novo exercise).
                val existingRemoteId = exerciseDao.getByIdOnce(exerciseId)?.remoteId
                val entity = ExerciseEntity(
                    id = exerciseId,
                    workoutId = workoutId,
                    name = name.trim(),
                    description = description.trim(),
                    targetMuscle = targetMuscle.trim(),
                    remoteId = existingRemoteId
                )
                exerciseDao.update(entity)
                app.syncManager.onExerciseUpdated(entity)
                app.triggerSync()
                _navigateBack.emit(Unit)
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.workout_error_update_exercise))
            }
        }
    }

    /** Remove um exercício do treino. */
    fun deleteExercise(exerciseId: String) {
        val app = getApplication<MuscleUpApp>()
        viewModelScope.launch {
            try {
                val existing = exerciseDao.getByIdOnce(exerciseId)
                exerciseDao.deleteById(exerciseId)
                if (existing != null) {
                    app.syncManager.onExerciseDeleted(existing)
                    app.triggerSync()
                }
            } catch (_: Exception) {
                _uiEvent.emit(app.getString(R.string.workout_error_delete_exercise))
            }
        }
    }

    companion object {
        /** Cria a fábrica necessária para instanciar o WorkoutViewModel com o identificador do treino. */
        fun factory(workoutId: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                return WorkoutViewModel(app, workoutId) as T
            }
        }
    }
}
