package com.broto.shareplay

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import androidx.lifecycle.MutableLiveData

class AudioFocusManager private constructor(context: Context) {
    
    private var mAudioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    var mIsAudioFocusAvailable: MutableLiveData<Boolean> = MutableLiveData(false)

    init {
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    companion object {
        private const val TAG = "AudioFocusManager"
        private lateinit var mInstance: AudioFocusManager
        fun getInstance(context: Context): AudioFocusManager {
            if (!this::mInstance.isInitialized) {
                mInstance = AudioFocusManager(context)
            }
            return mInstance
        }
    }

    fun requestFocus() {
        if (mIsAudioFocusAvailable.value == true) {
            Log.d(TAG, "Audio Focus already available. Ignore request")
            mIsAudioFocusAvailable.postValue(true)
            return
        }
        Log.d(TAG, "requestFocus: ")
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener {
                when (it) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        Log.d(TAG, "Audio Focus Gained")
                        mIsAudioFocusAvailable.postValue(true)
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        Log.d(TAG, "Audio Focus Lost")
                        mIsAudioFocusAvailable.postValue(false)
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT")
                        mIsAudioFocusAvailable.postValue(false)
                    }
                }
            }
            .build()
        val result = mAudioManager.requestAudioFocus(audioFocusRequest!!)
        Log.d(TAG, "AudioFocus Result: $result")
        when (result) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                Log.d(TAG, "Audio Focus Request Failed")
                mIsAudioFocusAvailable.postValue(false)
            }
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                Log.d(TAG, "Audio Focus Request Granted")
                mIsAudioFocusAvailable.postValue(true)
            }
        }
    }

    fun abandonFocus() {
        Log.d(TAG, "abandonFocus")
        audioFocusRequest?.let {
            mAudioManager.abandonAudioFocusRequest(it)
        }
    }

    fun isAudioFocusAvailable(): Boolean {
        return mIsAudioFocusAvailable.value ?: false
    }
}