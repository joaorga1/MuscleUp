package pt.ipt.dama.muscleup.data.session

/** Estado em memória do utilizador autenticado. */
object UserSession {
    var currentUserName: String = ""
    var currentUserEmail: String = ""
    var currentUserId: String = ""

    /** Define os dados do utilizador autenticado. */
    fun set(name: String, email: String, userId: String = email) {
        currentUserName = name
        currentUserEmail = email
        currentUserId = userId
    }

    /** Limpa os dados do utilizador. */
    fun clear() {
        currentUserName = ""
        currentUserEmail = ""
        currentUserId = ""
    }
}

