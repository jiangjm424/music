package jm.music.client.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import jm.music.server.AbsMediaViewModel
import jm.music.server.MediaClientHelper

class NotificationsViewModel(
    mediaId: String,
    helper: MediaClientHelper
) : AbsMediaViewModel(mediaId, helper) {

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text
}
