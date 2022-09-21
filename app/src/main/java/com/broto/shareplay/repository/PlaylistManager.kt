package com.broto.shareplay.repository

import android.util.Log
import java.util.*

class PlaylistManager private constructor() {

    companion object {
        private const val TAG = "PlaylistManager"

        private lateinit var mInstance: PlaylistManager
        fun getInstance(): PlaylistManager {
            if (!this::mInstance.isInitialized) {
                mInstance = PlaylistManager()
            }
            return mInstance
        }
    }

    private val mPlayListItems: Queue<String> = LinkedList()

    fun addItemToPlayList(id: String?) {
        Log.d(TAG, "addItemToPlayList: id: $id")
        if (id == null || id.isEmpty()) {
            Log.d(TAG, "Id is invalid. Ignore")
            return
        }
        mPlayListItems.add(id)
    }

    fun getNextItem(): String {
        Log.d(TAG, "getNextItem")
        val item = mPlayListItems.poll() ?: ""
        if (item.isEmpty()) {
            Log.e(TAG, "No Item found")
            return ""
        }
        Log.d(TAG, "Next Playlist Media Id: $item")
        return item
    }

    fun isQueued(id: String?): Boolean {
        if (id == null) return false
        val res = mPlayListItems.contains(id)
        Log.d(TAG, "isQueued: $res")
        return res
    }

    fun removeItemFromPlaylist(id: String?) {
        Log.d(TAG, "removeItemFromPlaylist: $id")
        if (id == null || id.isEmpty()) {
            Log.d(TAG, "Invalid ID. Ignore... ")
            return
        }
        mPlayListItems.remove(id)
    }

}