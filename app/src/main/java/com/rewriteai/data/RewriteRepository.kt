package com.rewriteai.data

import com.rewriteai.data.RewriteApi.RewriteRequest
import com.rewriteai.data.RewriteApi.RewriteResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.UnknownHostException

class RewriteRepository(
    private val baseUrl: String
) {
    private val api: RewriteApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RewriteApi::class.java)

    suspend fun rewrite(text: String, style: RewriteStyle): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.rewriteText(
                RewriteRequest(text = text, style = style.apiValue)
            )
            Result.success(response.rewritten)
        } catch (e: UnknownHostException) {
            Result.failure(Exception("No internet connection"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
