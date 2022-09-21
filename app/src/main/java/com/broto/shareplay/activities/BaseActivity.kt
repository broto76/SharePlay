package com.broto.shareplay.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.broto.shareplay.AudioFocusManager

abstract class BaseActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AudioFocusManager.getInstance(this).mIsAudioFocusAvailable.observe(this) {
            onAudioFocusChanged(it)
        }
    }

    abstract fun onAudioFocusChanged(focus: Boolean)
}