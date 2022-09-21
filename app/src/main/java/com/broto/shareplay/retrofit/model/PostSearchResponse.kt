package com.broto.shareplay.retrofit.model

import com.google.gson.annotations.SerializedName

data class PostSearchResponse (
    @SerializedName("message") val message: MessageBody
) {
    data class MessageBody (
        @SerializedName("result") val items: List<SearchItem>,
        @SerializedName("success") val success: String
    ) {
        data class SearchItem (
            @SerializedName("title") val title: String,
            @SerializedName("id") val id: String,
            @SerializedName("bestThumbnail") val thumbnail: ThumbnailData,
            @SerializedName("duration") val duration: String
        ) {
            data class ThumbnailData (
                @SerializedName("url") val url: String,
                @SerializedName("width") val width: String,
                @SerializedName("height") val height: String
            )
        }
    }
}