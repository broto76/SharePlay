package com.broto.shareplay.activities

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.broto.shareplay.services.MusicPlayerService
import com.broto.shareplay.R

class PlayerActivity : AppCompatActivity() {

    private val TAG = "PlayerActivity"
    private var mBinder: MusicPlayerService.MusicPlayerServiceBinder? = null
    private val URL = "https://rr1---sn-h5576nee.googlevideo.com/videoplayback?expire=1662941578&ei=KSUeY9LtOuGkz7sP5pWByAk&ip=143.110.255.124&id=o-AE2_Oj7-n24YAWQCm8o9MWg-JWZoLRzmyeXg8Ul6DUR2&itag=140&source=youtube&requiressl=yes&vprv=1&mime=audio%2Fmp4&ns=YlsAibPIT4Y-PyK2wNz4OMIH&gir=yes&clen=8003776&dur=503.896&lmt=1492750985776219&keepalive=yes&fexp=24001373,24007246&c=WEB&rbqsm=fr&n=HHS7rrm6i48RaA&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cvprv%2Cmime%2Cns%2Cgir%2Cclen%2Cdur%2Clmt&sig=AOq0QJ8wRgIhAMlAP_Y6Qnn3_snZ4xzpcxNVjJaibnApm---hMDZLxxZAiEA-UE1xeRQd88v0yAyaImS2JuNIT9VFe7rgAa2aKmyN6E%3D&rm=sn-h55ek7e&req_id=dce875c19a97a3ee&cm2rm=sn-gwpa-cagl7e,sn-gwpa-h55y76&redirect_counter=3&cms_redirect=yes&cmsv=e&ipbypass=yes&mh=wV&mip=2405:201:d020:10b3:94bf:9c2b:fbb:2c7&mm=30&mn=sn-h5576nee&ms=nxu&mt=1662919952&mv=m&mvi=1&pl=50&lsparams=ipbypass,mh,mip,mm,mn,ms,mv,mvi,pl&lsig=AG3C_xAwRAIgRd8HtWPWT-q8ah2U_iHhPDAEkBigvptFnC98zBF4F4gCIFBAiQY9AR5-ZaHzTLUc5tMyDBA3tOoKPDzhNAY_rKnG"

    private val mServiceConnectionCallback = object: ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            Log.d(TAG, "Connected to Service")
            mBinder = p1 as MusicPlayerService.MusicPlayerServiceBinder
            mBinder?.updateActiveUrl(URL, "")
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            Log.d(TAG, "Disconnected from Service")
            mBinder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        findViewById<Button>(R.id.button).setOnClickListener {
            mBinder?.play()
        }
        findViewById<Button>(R.id.button2).setOnClickListener {
            mBinder?.pause()
        }
        val mServiceIntent = Intent(applicationContext, MusicPlayerService::class.java)
        startService(mServiceIntent)
        bindService(
            mServiceIntent,
            mServiceConnectionCallback,
            BIND_AUTO_CREATE
        )
    }

}