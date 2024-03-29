package ge.baqar.gogia.gefolk.http

import android.app.Application
import com.google.gson.Gson
import ge.baqar.gogia.gefolk.FolkApplication
import ge.baqar.gogia.gefolk.http.response.ResponseBase
import ge.baqar.gogia.gefolk.storage.FolkAppPreferences
import ge.baqar.gogia.gefolk.utility.NetworkStatus
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody

class AuthGuardRequestInterceptor(
    private val preferences: FolkAppPreferences,
    private var networkStatus: NetworkStatus,
    private val application: Application?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (!networkStatus.isOnline()) {
            val response = Gson().toJson(ResponseBase("", null))
            chain.call().cancel()
            return Response.Builder()
                .code(200)
                .request(request)
                .protocol(Protocol.HTTP_2)
                .message("")
                .body(
                    ResponseBody.create("application/json".toMediaType(), response)
                )
                .build()
        }

        try {
            val response = chain.proceed(request)
            if (!checkAuthorization(response)) {
                (application as FolkApplication).let {
                    preferences.setToken(null)
                    application.logOut()
                    return Response.Builder()
                        .code(401)
                        .request(request)
                        .protocol(Protocol.HTTP_2)
                        .message("authentication error")
                        .body(
                            ResponseBody.create(
                                "application/text".toMediaType(),
                                "authentication error"
                            )
                        )
                        .build()
                }
            }
            return response
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Response.Builder()
            .code(401)
            .request(request)
            .protocol(Protocol.HTTP_2)
            .message("authentication error")
            .body(
                ResponseBody.create(
                    "application/text".toMediaType(),
                    "authentication error"
                )
            )
            .build()
    }

    private fun checkAuthorization(response: Response): Boolean {
        return response.code != 401
    }
}