package pt.ipt.dama.muscleup.data.session

object UserSession {
    var currentUserName: String = ""
    var currentUserEmail: String = ""

    fun set(name: String, email: String) {
        currentUserName = name
        currentUserEmail = email
    }

    fun clear() {
        currentUserName = ""
        currentUserEmail = ""
    }
}

