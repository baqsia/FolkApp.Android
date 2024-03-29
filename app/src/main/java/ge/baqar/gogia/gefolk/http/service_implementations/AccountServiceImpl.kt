package ge.baqar.gogia.gefolk.http.service_implementations

import ge.baqar.gogia.gefolk.http.request.LoginByTokenRequest
import ge.baqar.gogia.gefolk.http.request.LoginRequest
import ge.baqar.gogia.gefolk.http.request.RegisterAccountRequest
import ge.baqar.gogia.gefolk.http.request.VerifyAccountRequest
import ge.baqar.gogia.gefolk.http.response.BaseError
import ge.baqar.gogia.gefolk.http.services.AccountService
import ge.baqar.gogia.gefolk.model.ReactiveResult
import ge.baqar.gogia.gefolk.model.asError
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import retrofit2.HttpException

class AccountServiceImpl(
    private var accountService: AccountService
) : ServiceBase() {

    suspend fun login(
        email: String,
        password: String,
        deviceId: String
    ): Flow<ReactiveResult<BaseError, String>> {
        return coroutineScope {
            try {
                val loginResult =
                    accountService.login(LoginRequest(email, password, deviceId, 1))
                val flow = callbackFlow {
                    trySend(mapToReactiveResult(loginResult))
                    awaitClose { channel.close() }
                }
                return@coroutineScope flow
            } catch (ex: HttpException) {
                val response = escapeServerError(ex)
                return@coroutineScope flowOf(response.error?.asError!!)
            }
        }
    }

    suspend fun register(registerRequest: RegisterAccountRequest): Flow<ReactiveResult<BaseError, String>> {
        return coroutineScope {
            try {
                val result =
                    accountService.register(registerRequest)
                val flow = callbackFlow {
                    trySend(mapToReactiveResult(result))
                    awaitClose { channel.close() }
                }
                return@coroutineScope flow
            } catch (ex: HttpException) {
                val response = escapeServerError(ex)
                return@coroutineScope flowOf(response.error?.asError!!)
            }
        }
    }

    suspend fun verify(
        request: VerifyAccountRequest,
        id: String
    ): Flow<ReactiveResult<BaseError, Boolean>> {
        return coroutineScope {
            try {
                val result =
                    accountService.verify(request, id)
                val flow = callbackFlow {
                    trySend(mapToReactiveResult(result))
                    awaitClose { channel.close() }
                }
                return@coroutineScope flow
            } catch (ex: HttpException) {
                val response = escapeServerError(ex)
                return@coroutineScope flowOf(response.error?.asError!!)
            }
        }
    }

    suspend fun loginByToken(
        token: String,
        deviceId: String
    ): Flow<ReactiveResult<BaseError, String>> {
        return coroutineScope {
            try {
                val loginResult =
                    accountService.loginByToken(LoginByTokenRequest(token, deviceId, 1))
                val flow = callbackFlow {
                    trySend(mapToReactiveResult(loginResult))
                    awaitClose { channel.close() }
                }
                return@coroutineScope flow
            } catch (ex: HttpException) {
                val response = escapeServerError(ex)
                return@coroutineScope flowOf(response.error?.asError!!)
            }
        }
    }
}