package com.broto.shareplay.activities

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.common.Player
import com.broto.shareplay.R
import com.broto.shareplay.Utility
import com.broto.shareplay.repository.SharePlayApiRepository
import com.broto.shareplay.services.MusicPlayerService

class LandingPageActivity : BaseActivity() {

    companion object {
        private const val TAG = "LandingPageActivity"
    }

    private var musicPlayerServiceBinder: MusicPlayerService.MusicPlayerServiceBinder? = null
    private var miniPlayerSeekBar: SeekBar? = null
    private var mIsSeeking = false

    private val mServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
            musicPlayerServiceBinder = binder as MusicPlayerService.MusicPlayerServiceBinder
            musicPlayerServiceBinder?.getIsPlayingLiveData()?.observe(this@LandingPageActivity) {
                Log.d(TAG, "Music Playing status updated: $it")
                if (it) {
                    showMiniPlayer()
                    findViewById<ImageView>(R.id.iv_play_pause_btn).setImageResource(android.R.drawable.ic_media_pause)
                    miniPlayerSeekBar?.min = 0
                    val duration = ((musicPlayerServiceBinder?.getActiveSongDuration()?:0L)/1000).toInt()
                    miniPlayerSeekBar?.max = duration
                    findViewById<TextView>(R.id.tv_media_duration).text = Utility.getFormattedStringFromTimeStamp(
                        duration
                    )
                } else {
                    findViewById<ImageView>(R.id.iv_play_pause_btn).setImageResource(android.R.drawable.ic_media_play)
                }
            }
            musicPlayerServiceBinder?.getPlayerStateLiveData()?.observe(this@LandingPageActivity) {
                when (it) {
                    Player.STATE_BUFFERING, Player.STATE_IDLE -> if (!mIsSeeking) hideMiniPlayer()
                    Player.STATE_READY, Player.STATE_ENDED -> if (!mIsSeeking) showMiniPlayer()
                }
            }
            musicPlayerServiceBinder?.getActiveMediaCurrentPositionLiveData()?.observe(this@LandingPageActivity) {
                Log.d(TAG, "Timestamp: $it")
                val progress = ( it / 1000).toInt()
                Log.d(TAG, "Progress Updating to: $progress")
                miniPlayerSeekBar?.progress = progress
                findViewById<TextView>(R.id.tv_media_position).text = Utility.getFormattedStringFromTimeStamp(
                    progress
                )
            }

        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            musicPlayerServiceBinder = null
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing_page)
        val mServiceIntent = Intent(applicationContext, MusicPlayerService::class.java)
        startService(mServiceIntent)
        bindService(
            mServiceIntent,
            mServiceConnection,
            BIND_AUTO_CREATE
        )
        SharePlayApiRepository.getInstance().mIsAudioUrlDirty.observe(this) {
            if (it == false) {
                val audioUrl = SharePlayApiRepository.getInstance().mActiveAudioUrl
                val nextAudioItem = SharePlayApiRepository.getInstance().mNextMusicId ?: ""
                Log.d(TAG, "Active Audio URL: $audioUrl")
                musicPlayerServiceBinder?.updateActiveUrl(audioUrl, SharePlayApiRepository.getInstance().mActiveAudioTitle)
                musicPlayerServiceBinder?.updateNextTrackId(nextAudioItem)
                showMiniPlayerTitle(SharePlayApiRepository.getInstance().mActiveAudioTitle)
            }
        }
        findViewById<ImageView>(R.id.iv_play_pause_btn).setOnClickListener {
            Log.d(TAG, "Play/Pause button clicked")
            val isPlaying = musicPlayerServiceBinder?.getIsPlayingLiveData()?.value
            Log.d(TAG, "Current Playing State: $isPlaying")
            if (isPlaying == true) {
                Log.d(TAG, "Performing action pause")
                musicPlayerServiceBinder?.pause()
            } else {
                Log.d(TAG, "Performing action play")
                musicPlayerServiceBinder?.play()
            }
        }
        findViewById<ImageView>(R.id.iv_player_prev).setOnClickListener {
            Log.d(TAG, "Prev Button Clicked ...")
            musicPlayerServiceBinder?.handlePrev()
        }
        findViewById<ImageView>(R.id.iv_player_next).setOnClickListener {
            Log.d(TAG, "Next Button Clicked ...")
            musicPlayerServiceBinder?.handleNext()
        }
        miniPlayerSeekBar = findViewById(R.id.sb_progress)
        miniPlayerSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.d(TAG, "Progress value: $progress fromUser: $fromUser max: ${seekbar?.max}")
                if (fromUser) musicPlayerServiceBinder?.seekToPosition(progress.toLong())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                Log.d(TAG, "onStartTrackingTouch: ")
                mIsSeeking = true
            }
            override fun onStopTrackingTouch(p0: SeekBar?) {
                Log.d(TAG, "onStopTrackingTouch: ")
                mIsSeeking = false
            }
        })
        hideMiniPlayer()
    }

    override fun onAudioFocusChanged(focus: Boolean) {
        Log.d(TAG, "onAudioFocusChanged: $focus")
        if (focus) {
            if (musicPlayerServiceBinder == null) {
                Log.d(TAG, "Service not yet ready. ")
                return
            }
            musicPlayerServiceBinder?.play()
        } else {
            musicPlayerServiceBinder?.pause()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)
        val searchView = menu.findItem(R.id.menu_search).actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query == null || query.isEmpty()) {
                    Log.e(TAG, "Empty or null search query. Ignore..")
                    return false
                }
                Log.d(TAG, "Text Submitted: $query")
                SharePlayApiRepository.getInstance().search(this@LandingPageActivity, query)
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d(TAG, "Text Changed: $newText")
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    private fun showMiniPlayer() {
        Log.d(TAG, "showMiniPlayer: ")
        findViewById<ConstraintLayout>(R.id.mini_player_controls).visibility = View.VISIBLE
    }

    private fun hideMiniPlayer() {
        Log.d(TAG, "hideMiniPlayer: ")
        findViewById<ConstraintLayout>(R.id.mini_player_controls).visibility = View.GONE
    }

    private fun showMiniPlayerTitle(text: String) {
        Log.d(TAG, "showMiniPlayerTitle: $text")
        val titleView = findViewById<TextView>(R.id.tv_mini_player_title)
        titleView.text = text
        titleView.visibility = View.VISIBLE
    }

    private fun hideMiniPlayerTitle() {
        Log.d(TAG, "hideMiniPlayerTitle: ")
        findViewById<TextView>(R.id.tv_mini_player_title).visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
    }

}