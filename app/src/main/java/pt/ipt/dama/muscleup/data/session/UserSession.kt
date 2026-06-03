package pt.ipt.dama.muscleup.data.session

object UserSession {
    var currentUserName: String = ""
    var currentUserEmail: String = ""
    var currentUserId: String = ""

    fun set(name: String, email: String, userId: String = email) {
        currentUserName = name
        currentUserEmail = email
        currentUserId = userId
    }

    fun clear() {
        currentUserName = ""
        currentUserEmail = ""
        currentUserId = ""
    }
}

