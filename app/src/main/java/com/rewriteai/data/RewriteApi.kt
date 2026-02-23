package com.rewriteai.data

import retrofit2.http.Body
import retrofit2.http.POST

interface RewriteApi {

    @POST("rewriteText")
    suspend fun rewriteText(@Body body: RewriteRequest): RewriteResponse

    data class RewriteRequest(
        val text: String,
        val style: String,
        val regenerate: Boolean = false
    )

    data class RewriteResponse(
        val rewritten: String
    )
}
