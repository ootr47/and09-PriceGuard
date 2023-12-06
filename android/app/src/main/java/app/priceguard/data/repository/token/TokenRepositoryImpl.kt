package app.priceguard.data.repository.token

import android.util.Log
import app.priceguard.data.datastore.TokenDataSource
import app.priceguard.data.dto.firebase.FirebaseTokenUpdateRequest
import app.priceguard.data.network.AuthAPI
import app.priceguard.data.network.UserAPI
import app.priceguard.data.repository.APIResult
import app.priceguard.data.repository.RepositoryResult
import app.priceguard.data.repository.getApiResult
import app.priceguard.ui.data.UserDataResult
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

class TokenRepositoryImpl @Inject constructor(
    private val tokenDataSource: TokenDataSource,
    private val authAPI: AuthAPI,
    private val userAPI: UserAPI
) : TokenRepository {

    override suspend fun storeTokens(accessToken: String, refreshToken: String) {
        tokenDataSource.saveTokens(accessToken, refreshToken)
    }

    override suspend fun getAccessToken(): String? {
        return tokenDataSource.getAccessToken()
    }

    override suspend fun getRefreshToken(): String? {
        return tokenDataSource.getRefreshToken()
    }

    override suspend fun getFirebaseToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e("ERROR", e.toString())
            null
        }
    }

    override suspend fun updateFirebaseToken(firebaseToken: String): RepositoryResult<Boolean, TokenErrorState> {
        return when (
            val response =
                getApiResult { userAPI.updateFirebaseToken(FirebaseTokenUpdateRequest(firebaseToken)) }
        ) {
            is APIResult.Success -> {
                RepositoryResult.Success(true)
            }

            is APIResult.Error -> {
                handleError(response.code)
            }
        }
    }

    override suspend fun getUserData(): UserDataResult {
        val accessToken = tokenDataSource.getAccessToken() ?: return UserDataResult("", "")
        val parts = accessToken.split(".")
        return try {
            val charset = charset("UTF-8")
            val payload = Json.decodeFromString<TokenUserData>(
                String(Base64.getUrlDecoder().decode(parts[1].toByteArray(charset)), charset)
            )
            UserDataResult(payload.email, payload.name)
        } catch (e: Exception) {
            Log.e("Data Not Found", "Error parsing JWT: $e")
            UserDataResult("", "")
        }
    }

    override suspend fun renewTokens(refreshToken: String): RepositoryResult<Boolean, TokenErrorState> {
        return when (val response = getApiResult { authAPI.renewTokens("Bearer $refreshToken") }) {
            is APIResult.Success -> {
                storeTokens(response.data.accessToken, response.data.refreshToken)
                RepositoryResult.Success(true)
            }

            is APIResult.Error -> {
                handleError(response.code)
            }
        }
    }

    override suspend fun clearTokens() {
        Firebase.messaging.deleteToken()
        tokenDataSource.clearTokens()
    }

    private fun <T> handleError(
        code: Int?
    ): RepositoryResult<T, TokenErrorState> {
        return when (code) {
            401 -> {
                RepositoryResult.Error(TokenErrorState.UNAUTHORIZED)
            }

            410 -> {
                RepositoryResult.Error(TokenErrorState.EXPIRED)
            }

            else -> {
                RepositoryResult.Error(TokenErrorState.UNDEFINED_ERROR)
            }
        }
    }
}
