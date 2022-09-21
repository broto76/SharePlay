package com.broto.shareplay.retrofit.model

import com.google.gson.annotations.SerializedName

data class PostTrackResponse (
    @SerializedName("message") val message: MessageBody
) {
    data class MessageBody(
        @SerializedName("result") val result: Result,
        @SerializedName("success") val success: String
    ) {
        data class Result(
            @SerializedName("url") val audioUrl: String,
            @SerializedName("related_videos") val relatedVideo: List<VideoData>,
            @SerializedName("title") val title: String
        ) {
            data class VideoData(
                @SerializedName("title") val title: String,
                @SerializedName("id") val id: String,
                @SerializedName("thumbnails") val thumbnails: List<PostSearchResponse.MessageBody.SearchItem.ThumbnailData>,
                @SerializedName("length_seconds") val duration: String
            )
        }
    }
}