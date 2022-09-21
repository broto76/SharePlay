package com.broto.shareplay.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.broto.shareplay.R
import com.broto.shareplay.retrofit.SharePlayApiService
import com.broto.shareplay.retrofit.model.PostSearchResponse
import com.broto.shareplay.retrofit.model.PostTrackResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SharePlayApiRepository private constructor() {

    companion object {
        private const val TAG = "SharePlayApiRepository"

        private lateinit var mInstance: SharePlayApiRepository
        fun getInstance(): SharePlayApiRepository {
            if (!this::mInstance.isInitialized) {
                mInstance = SharePlayApiRepository()
            }
            return mInstance
        }
    }

    val mSearchResults: MutableLiveData<List<PostSearchResponse.MessageBody.SearchItem>> =
        MutableLiveData()
    val mIsSearching: MutableLiveData<Boolean> = MutableLiveData(false)
    val mIsAudioUrlDirty: MutableLiveData<Boolean> = MutableLiveData(false)
    var mActiveAudioTitle: String = ""
    var mNextMusicId: String? = null
    var mActiveAudioUrl = ""

    fun getAudioUrl(context: Context, id: String) {
        Log.d(TAG, "getAudioUrl for: $id  ....")
        val sharePlayRequest = SharePlayApiService.getSharePlayApiService()
            .create(SharePlayApiService::class.java)
        sharePlayRequest.postTrack(id).enqueue(
            object: Callback<PostTrackResponse> {
                override fun onResponse(
                    call: Call<PostTrackResponse>?,
                    response: Response<PostTrackResponse>?
                ) {
                    Log.d(TAG, "getAudioUrl Response Code: ${response?.code()}")
                    val body = response?.body()
                    if (body == null) {
                        Log.e(TAG, "Body is null")
                        return
                    }
                    mActiveAudioUrl = body.message.result.audioUrl
                    mNextMusicId = body.message.result.relatedVideo[0].id
                    mIsAudioUrlDirty.postValue(false)
                    mActiveAudioTitle = (body.message.result.title)
                }

                override fun onFailure(call: Call<PostTrackResponse>?, t: Throwable?) {
                    Log.e(TAG, "Audio URL fetch request Failed. ${t?.message}")
                    mSearchResults.postValue(emptyList())
                    Toast.makeText(
                        context,
                        context.getString(R.string.audio_url_request_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }

            }
        )
        mIsAudioUrlDirty.postValue(true)
    }

    fun search(context: Context, keyword: String) {
        Log.d(TAG, "Searching for: $keyword  ....")
        val sharePlayRequest = SharePlayApiService.getSharePlayApiService()
            .create(SharePlayApiService::class.java)
        sharePlayRequest.postSearch(keyword).enqueue(
            object : Callback<PostSearchResponse> {
                override fun onResponse(
                    call: Call<PostSearchResponse>?,
                    response: Response<PostSearchResponse>?
                ) {
                    Log.d(TAG, "Search Response Code: ${response?.code()}")
                    val body = response?.body()
                    if (body == null) {
                        Log.e(TAG, "Body is null")
                        return
                    }
                    Log.d(TAG, "Success: ${body.message.success}")
                    mSearchResults.postValue(body.message.items)
                    mIsSearching.postValue(false)
                    for (item in body.message.items) {
                        Log.d(TAG, "")
                        Log.d(TAG, "Item Start")
                        Log.d(TAG, "Title: ${item.title}")
                        Log.d(TAG, "Id: ${item.id}")
                        Log.d(TAG, "Duration: ${item.duration}")
                        Log.d(TAG, "Thumbnail: ${item.thumbnail}")
                        Log.d(TAG, "Item End")
                        Log.d(TAG, "")
                    }
                }

                override fun onFailure(call: Call<PostSearchResponse>?, t: Throwable?) {
                    Log.e(TAG, "Search Request Failed. ${t?.message}")
                    mSearchResults.postValue(emptyList())
                    Toast.makeText(
                        context,
                        context.getString(R.string.search_request_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }

            }
        )
        mIsSearching.postValue(true)
    }
}