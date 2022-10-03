package jm.music.client.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import jm.music.server.AbsMediaViewModel
import jm.music.server.MediaClientHelper

class DashboardViewModel(mediaId: String, helper: MediaClientHelper) : AbsMediaViewModel(mediaId,
    helper
) {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text
}
