package jm.music.server

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.*
import jm.music.server.core.*
import jm.music.server.model.MediaItemData
import kotlin.math.floor

abstract class AbsMediaViewModel(
    private val mediaId: String,
    private val helper: MediaClientHelper
) : ViewModel() {
    companion object {
        private const val TAG = "AbsMediaViewModel"
    }

    private val subscriptionCallback = MediaItemsCallback()

    val isConnected = Transformations.map(helper.isConnected) {
        it
    }

    val rootMediaId = Transformations.map(helper.isConnected) {
        if (it) {
            helper.rootMediaId
        } else {
            null
        }
    }

    private val _mediaItems = MutableLiveData<List<MediaItemData>>()
    val mediaItems: LiveData<List<MediaItemData>> = _mediaItems

    //更新播放进度，开启主动的循环检测
    private val handler = ProgressHandler(Looper.getMainLooper())
    private var playbackState: PlaybackStateCompat = EMPTY_PLAYBACK_STATE
    private val _mediaMetadata = MutableLiveData<NowPlayingMetadata>()
    val mediaMetadata: LiveData<NowPlayingMetadata> = _mediaMetadata
    private val _mediaPosition = MutableLiveData<Long>().apply {
        postValue(0L)
    }
    val mediaPosition: LiveData<Long> = _mediaPosition
    private val _mediaState = MutableLiveData<Int>()
    val mediaState: LiveData<Int> = _mediaState

    fun play() {
        mediaItems.value?.firstOrNull()?.let {
            helper.playMedia(it.mediaId)
        }
    }

    fun prev() = helper.skipToPrevious()
    fun next() = helper.skipToNext()
    fun skipTo(pos: Long) = helper.seekTo(pos)
    fun setRepeatMode(@PlaybackStateCompat.RepeatMode mode: Int) = helper.setRepeatMode(mode)

    fun command(s: String) = helper.sendCommand(s, bundleOf("s" to 3, "b" to "bb"))

    /**
     * When the session's [PlaybackStateCompat] changes, the [mediaItems] need to be updated
     * so the correct [MediaItemData.playbackState] is displayed on the active item.
     * (i.e.: play/pause button or blank)
     */
    private val playbackStateObserver = Observer<PlaybackStateCompat> {
        val playbackState = it ?: EMPTY_PLAYBACK_STATE
        this.playbackState = playbackState
        val metadata = helper.nowPlaying.value ?: NOTHING_PLAYING
        LLog.i(
            TAG,
            "play back observer:playback:${playbackState.s()}--meta:${metadata.s()}, id:${metadata.id}"
        )
        //1 音乐状态发生变化，可能存在切歌的情况，更新一下列表中的数据
        if (!metadata.id.isNullOrEmpty()) {
            _mediaItems.postValue(updateMediasState(playbackState, metadata))
        }
        LLog.i(
            TAG,
            "play back observer equal nothing: meta: ${metadata == NOTHING_PLAYING}, play:${playbackState == EMPTY_PLAYBACK_STATE}"
        )
        LLog.i(TAG, "play back observer playIng:${playbackState.isPlaying}")
        //2 保存当前播放的音乐对象，可以从中拿到播放进度
        updatePlaybackState(playbackState, metadata)
        if (playbackState.isPlaying) {
            handler.start()
        } else {
            handler.stop()
        }
    }


    /**
     * When the session's [MediaMetadataCompat] changes, the [mediaItems] need to be updated
     * as it means the currently active item has changed. As a result, the new, and potentially
     * old item (if there was one), both need to have their [MediaItemData.playbackState]
     * changed. (i.e.: play/pause button or blank)
     */
    private val mediaMetadataObserver = Observer<MediaMetadataCompat> {
        val playbackState = helper.playbackState.value ?: EMPTY_PLAYBACK_STATE
        val metadata = it ?: NOTHING_PLAYING
        LLog.i(
            TAG,
            "meta data observer: playback:${playbackState.s()}--meta:${metadata.s()}, id:${metadata.id}"
        )
        LLog.i(
            TAG,
            "meta data observer equal nothing: meta: ${metadata == NOTHING_PLAYING}, play:${playbackState == EMPTY_PLAYBACK_STATE}"
        )
        LLog.i(TAG, "meta data observer playIng:${playbackState.isPlaying}")
        if (!metadata.id.isNullOrEmpty()) {
            _mediaItems.postValue(updateMediasState(playbackState, metadata))
        }
    }

    private val aa = helper.apply {
        helper.subscribe(mediaId, subscriptionCallback)
        helper.playbackState.observeForever(playbackStateObserver)
        helper.nowPlaying.observeForever(mediaMetadataObserver)
        LLog.i(TAG, "init:${this@AbsMediaViewModel}")
    }

    //查看播放的音乐的进度状态
    private fun updatePlaybackState(
        playbackState: PlaybackStateCompat,
        mediaMetadata: MediaMetadataCompat
    ) {

        // Only update media item once we have duration available
        if (mediaMetadata.duration != 0L && mediaMetadata.id != null) {
            val nowPlayingMetadata = NowPlayingMetadata(
                mediaMetadata.id!!,
                mediaMetadata.albumArtUri,
                mediaMetadata.title?.trim(),
                mediaMetadata.displaySubtitle?.trim(),
                mediaMetadata.duration
            )
            this._mediaMetadata.postValue(nowPlayingMetadata)
        }

        // Update the media button resource ID
        _mediaState.postValue(
            when (playbackState.isPlaying) {
                true -> MediaItemData.PLAYBACK_STATE_PLAY
                else -> MediaItemData.PLAYBACK_STATE_PAUSE
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        LLog.i(TAG, "onCleared:$this")
        helper.unsubscribe(mediaId, subscriptionCallback)
        helper.playbackState.removeObserver(playbackStateObserver)
        helper.nowPlaying.removeObserver(mediaMetadataObserver)
        handler.stop()
    }

    private fun getPlaybackState(mediaId: String): @MediaItemData.Companion.PlaybackState Int {
        val isActive = mediaId == helper.nowPlaying.value?.id
        val isPlaying = helper.playbackState.value?.isPlaying ?: false
        return when {
            !isActive -> MediaItemData.PLAYBACK_STATE_NONE
            isPlaying -> MediaItemData.PLAYBACK_STATE_PLAY
            else -> MediaItemData.PLAYBACK_STATE_PAUSE
        }
    }


    private fun updateMediasState(
        playbackState: PlaybackStateCompat,
        mediaMetadata: MediaMetadataCompat
    ): List<MediaItemData> {

        val newResId = when (playbackState.isPlaying) {
            true -> MediaItemData.PLAYBACK_STATE_PLAY
            else -> MediaItemData.PLAYBACK_STATE_PLAY
        }

        return mediaItems.value?.map {
            val useResId =
                if (it.mediaId == mediaMetadata.id) newResId else MediaItemData.PLAYBACK_STATE_NONE
            it.copy(playbackState = useResId)
        } ?: emptyList()
    }

    private inner class MediaItemsCallback : MediaBrowserCompat.SubscriptionCallback() {

        override fun onChildrenLoaded(
            parentId: String,
            children: List<MediaBrowserCompat.MediaItem>
        ) {
            val itemsList = children.map { child ->
                val subtitle = child.description.subtitle ?: ""
                MediaItemData(
                    child.mediaId!!,
                    child.description.title.toString(),
                    subtitle.toString(),
                    child.description.iconUri!!,
                    child.isBrowsable,
                    getPlaybackState(child.mediaId!!)
                )
            }
            _mediaItems.postValue(itemsList)
        }
    }

    class Factory(
        private val app: String,
        private val helper: MediaClientHelper
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(String::class.java, MediaClientHelper::class.java)
                .newInstance(app, helper) as T
        }
    }

    private inner class ProgressHandler(looper: Looper) : Handler(looper) {
        private var updatePositionEnable = false
        private var enable = true
        fun enable(e: Boolean) {
            if (enable == e) return
            enable = e
            if (enable && updatePositionEnable) {
                sendEmptyMessage(1)
            }
        }

        fun start() {
            if (updatePositionEnable) return
            removeCallbacksAndMessages(null)
            updatePositionEnable = true
            sendEmptyMessage(1)
        }

        fun stop() {
            if (!updatePositionEnable) return
            removeCallbacksAndMessages(null)
            updatePositionEnable = false
        }

        override fun handleMessage(msg: Message) {
            val currPosition = playbackState.currentPlayBackPosition
            if (_mediaPosition.value != currPosition)
                _mediaPosition.postValue(currPosition)
            if (enable && updatePositionEnable)
                sendEmptyMessageDelayed(1, 1000)
        }
    }
}

data class NowPlayingMetadata(
    val id: String,
    val albumArtUri: Uri,
    val title: String?,
    val subtitle: String?,
    val duration: Long
) {

    companion object {
        /**
         * Utility method to convert milliseconds to a display of minutes and seconds
         */
        fun timestampToMSS(context: Context, position: Long): String {
            val totalSeconds = floor(position / 1E3).toInt()
            val minutes = totalSeconds / 60
            val remainingSeconds = totalSeconds - (minutes * 60)
            return if (position < 0) "--：--"
            else "%d:%02d".format(minutes, remainingSeconds)
        }
    }
}
