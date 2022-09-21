package com.broto.shareplay.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.broto.shareplay.AudioFocusManager
import com.broto.shareplay.repository.SharePlayApiRepository
import com.broto.shareplay.activities.LandingPageActivity
import com.broto.shareplay.repository.PlaylistManager


class MusicPlayerService : Service() {

    companion object {
        private const val TAG = "MusicPlayerService"
    }

    private lateinit var mBinder: MusicPlayerServiceBinder
    private var mActiveURL: String? = null
    private var mActiveMediaTitle: String = ""
    private var mNextMediaId: String? = null

    private var mPlayer: ExoPlayer? = null
    private var mPlayerStateLiveData: MutableLiveData<Int> = MutableLiveData(-1)
    private var mActiveTrackDuration = 0L
    private var mActiveTrackPosition = 0L
    private var mActiveTrackPositionLiveData: MutableLiveData<Long> = MutableLiveData(0L)

    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    private var mHandler = Handler(Looper.getMainLooper())
    private var mUpdateTimerRunnable = Runnable {
        updateTimerData()
    }

    private val MAX_SERVICE_KILL_DELAY = 1000L * 60
    private var mStopServiceRunnable = Runnable {
        AudioFocusManager.getInstance(applicationContext).abandonFocus()
        AudioFocusManager.getInstance(applicationContext)
            .mIsAudioFocusAvailable.removeObserver(mAudioFocusObserver)
        stopSelf()
    }
    private val mAudioFocusObserver: Observer<Boolean> = Observer {
        if (!it) {
            Log.d(TAG, "Focus Lost. Stop Playback")
            mPlayer?.playWhenReady = false
        } else {
            Log.d(TAG, "Focus Gained.")
            mPlayer?.playWhenReady = true
        }
    }

    var mIsPlayingLivedata: MutableLiveData<Boolean> = MutableLiveData(false)

    override fun onCreate() {
        super.onCreate()
        AudioFocusManager.getInstance(applicationContext)
            .mIsAudioFocusAvailable.observeForever(mAudioFocusObserver)
    }

    override fun onBind(intent: Intent): IBinder {
        if (!this::mBinder.isInitialized) mBinder = MusicPlayerServiceBinder()
        return mBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun releasePlayer() {
        mPlayer?.let {
            playbackPosition = it.currentPosition
            currentItem = it.currentMediaItemIndex
            playWhenReady = it.playWhenReady
            it.release()
        }
        mPlayer = null
    }

     fun updateTimerData() {
        synchronized(this) {
            Log.d(TAG, "updateTimerData")
            mHandler.removeCallbacks(mUpdateTimerRunnable)
            mActiveTrackPosition = mPlayer?.currentPosition?:0
            mActiveTrackPositionLiveData.postValue(mActiveTrackPosition)
            Log.d(TAG, "Current Position: $mActiveTrackPosition")
            mHandler.postDelayed(mUpdateTimerRunnable, 1000)
        }
    }

    private fun playNextMusic(): Boolean {
        Log.d(TAG, "playNextMusic")
        val nextPlayListItemId = PlaylistManager.getInstance().getNextItem()
        if (nextPlayListItemId.isEmpty()) {
            Log.d(TAG, "No more item available in playlist. ")
        } else {
            Log.d(TAG, "Playing next item in playlist: $nextPlayListItemId")
            SharePlayApiRepository.getInstance().getAudioUrl(applicationContext, nextPlayListItemId)
            return true
        }
        synchronized(this) {
            if (mNextMediaId == null) {
                Log.d(TAG, "Next media is not available. Ignore ...")
                return false
            }
            Log.d(TAG, "Playing next related video: $mNextMediaId")
            SharePlayApiRepository.getInstance().getAudioUrl(applicationContext, mNextMediaId!!)
            mNextMediaId = null
            return true
        }
    }

    private fun configureForeGroundService() {
        Log.d(TAG, "configureForeGroundService: ")

        val channel = NotificationChannel(
            applicationContext.packageName,
            "My Foreground Service",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent =
            Intent(this, LandingPageActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE)
            }

        val notification: Notification = Notification.Builder(this, applicationContext.packageName)
            .setContentTitle(mActiveMediaTitle)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(System.currentTimeMillis().toInt(), notification)
    }

    fun detachForeGroundService() {
        Log.d(TAG, "detachForeGroundService")
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun initializePlayer() {
        Log.d(TAG, "initializePlayer: ")
        if (mPlayer != null) {
            releasePlayer()
        }
        mPlayer = ExoPlayer.Builder(this)
            .build()
            .also {
                val mediaItem = MediaItem.fromUri(Uri.parse(mActiveURL))
                it.setMediaItem(mediaItem)
                it.playWhenReady = playWhenReady
//                it.seekTo(currentItem, playbackPosition)
                val isFocusAvailable = AudioFocusManager.getInstance(applicationContext).isAudioFocusAvailable()
                it.playWhenReady = isFocusAvailable
                if (!isFocusAvailable) {
                    Log.d(TAG, "Audio Focus not available")
                    AudioFocusManager.getInstance(applicationContext).requestFocus()
                }
                it.prepare()
//                configureForeGroundService()
            }
        mPlayer?.addListener(object: Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "onIsPlayingChanged: $isPlaying")
                mActiveTrackPosition = mPlayer?.currentPosition?:0
                mIsPlayingLivedata.postValue(isPlaying)
                if (isPlaying) updateTimerData()
                else mHandler.removeCallbacks(mUpdateTimerRunnable)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "onPlaybackStateChanged: $playbackState")
                mPlayerStateLiveData.postValue(playbackState)
                if (playbackState == Player.STATE_READY) {
                    mHandler.removeCallbacks(mStopServiceRunnable)
                    mActiveTrackDuration = mPlayer?.duration?:0
                    Log.d(TAG, "Song Duration: $mActiveTrackDuration")
                } else if (playbackState == Player.STATE_ENDED) {
//                    detachForeGroundService()
                    if (!playNextMusic()) mHandler.postDelayed(mStopServiceRunnable, MAX_SERVICE_KILL_DELAY)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.d(TAG, "onPlayerError")
                Log.e(TAG, error.message.toString())
                Log.e(TAG, error.printStackTrace().toString())
            }

            // Check super class
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                Log.d(TAG, "Media title: ${mediaMetadata.title} artist: ${mediaMetadata.artist}")
            }
        })
    }

    inner class MusicPlayerServiceBinder: Binder() {

        fun updateActiveUrl(url: String, title: String) {
            Log.d(TAG, "updateActiveUrl: $url title: $title")
            mActiveURL = url
            mActiveMediaTitle = title
            initializePlayer()
        }

        fun updateNextTrackId(id: String) {
            Log.d(TAG, "updateNextTrackId: ")
            if (id.isEmpty()) {
                Log.d(TAG, "Ignoring empty id")
                return
            }
            mNextMediaId = id
        }

        fun play() {
            Log.d(TAG, "play: ")
            if (AudioFocusManager.getInstance(applicationContext).isAudioFocusAvailable()) {
                Log.d(TAG, "Audio Focus Available. Start Playing")
                mPlayer?.playWhenReady = true
            } else {
                Log.d(TAG, "Focus Not available. Wait for Audio Focus.")
                AudioFocusManager.getInstance(applicationContext).requestFocus()
            }
        }

        fun pause() {
            Log.d(TAG, "pause: ")
            mPlayer?.playWhenReady = false
        }

        fun handlePrev() {
            Log.d(TAG, "handlePrev:")
            mPlayer?.seekTo(0, 0)
            mPlayer?.playWhenReady = true
            mPlayer?.prepare()
        }

        fun handleNext() {
            Log.d(TAG, "handleNext:")
            playNextMusic()
        }

        fun getIsPlayingLiveData(): LiveData<Boolean> {
            return mIsPlayingLivedata
        }

        fun getPlayerStateLiveData(): LiveData<Int> {
            return mPlayerStateLiveData
        }

        fun getActiveMediaCurrentPositionLiveData(): LiveData<Long> {
            return mActiveTrackPositionLiveData
        }

        fun getActiveSongPosition(): Long {
            return mActiveTrackPosition
        }

        fun getActiveSongDuration(): Long {
            return mActiveTrackDuration
        }

        fun seekToPosition(position: Long) {
            val newPosition = position * 1000
            Log.d(TAG, "seekToPosition: $newPosition")
            Log.d(TAG, "Duration: $mActiveTrackDuration")
            Log.d(TAG, "Current Position: $mActiveTrackPosition")
            mPlayer?.seekTo(0, newPosition)
            mPlayer?.playWhenReady = true
            mPlayer?.prepare()
        }
    }
}