package jm.music.client.ui.home

import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import jm.music.client.MusicProxy
import jm.music.client.R
import jm.music.server.AbsMediaViewModel
import jm.music.server.MediaClientHelper
import jm.music.server.core.id
import jm.music.server.core.isPlaying

class HomeViewModel(mediaId: String, helper: MediaClientHelper) : AbsMediaViewModel(mediaId, helper) {


    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

}
