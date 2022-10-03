package jm.music.client

import android.annotation.SuppressLint
import android.content.Context
import jm.music.server.MediaClientHelper

object MusicProxy {
    @SuppressLint("StaticFieldLeak")
    lateinit var helper: MediaClientHelper
        private set

    fun init(context: Context) {
        helper = MediaClientHelper(context.applicationContext)
    }
}
