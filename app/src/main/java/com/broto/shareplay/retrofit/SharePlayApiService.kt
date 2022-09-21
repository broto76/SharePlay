package com.broto.shareplay.retrofit

import com.broto.shareplay.retrofit.model.PostSearchResponse
import com.broto.shareplay.retrofit.model.PostTrackResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SharePlayApiService {

    @Headers(
        "Origin: https://www.shareplay.co.in"
    )
    @POST("search")
    fun postSearch(
        @Query("q") q: String
    ) : Call<PostSearchResponse>

    @Headers(
        "Origin: https://www.shareplay.co.in"
    )
    @POST("track")
    fun postTrack(
        @Query("id") id: String
    ) : Call<PostTrackResponse>


    companion object {
        fun getSharePlayApiService(): Retrofit {
            return Retrofit.Builder()
                .baseUrl("https://www.shareplay.co.in")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }
}