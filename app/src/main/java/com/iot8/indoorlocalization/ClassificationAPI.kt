package com.iot8.indoorlocalization

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ClassificationAPI {
    @Multipart
    @POST("/classify/")
    fun classify(
        @Part pcm: MultipartBody.Part,
        @Part("name") name: RequestBody
    ): Call<PostResponseModel>
}