package ge.baqar.gogia.gefolk.http.request

data class LoginByTokenRequest(
    val token: String,
    val deviceId: String,
    val platformOs: Long
)

