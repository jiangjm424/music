package jm.music.client

import android.app.Application
import jm.music.server.LLog

class App : Application() {
    companion object {
        private const val TAG = "App"
    }
    override fun onCreate() {
        super.onCreate()
        LLog.i(TAG,"app oncreate ")
        MusicProxy.init(this)
    }
}
