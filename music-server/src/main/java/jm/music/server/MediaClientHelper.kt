package jm.music.server

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media.MediaBrowserServiceCompat
import jm.music.server.core.id
import jm.music.server.core.isPlayEnabled
import jm.music.server.core.isPlaying
import jm.music.server.core.isPrepared

class MediaClientHelper(private val context: Context) {

    companion object {
        private const val TAG = "MediaClientHelper"
    }

    private val _isConnected = MutableLiveData<Boolean>().apply { postValue(false) }
    val isConnected: LiveData<Boolean> = _isConnected
    private val _networkFailure = MutableLiveData<Boolean>().apply { postValue(false) }
    val networkFailure: LiveData<Boolean> = _networkFailure
    val rootMediaId: String get() = mediaBrowser.root

    private val _playbackState =
        MutableLiveData<PlaybackStateCompat>().apply { postValue(EMPTY_PLAYBACK_STATE) }
    val playbackState: LiveData<PlaybackStateCompat> = _playbackState
    private val _nowPlaying =
        MutableLiveData<MediaMetadataCompat>().apply { postValue(NOTHING_PLAYING) }
    val nowPlaying: LiveData<MediaMetadataCompat> = _nowPlaying

    private val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)
    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(context, MusicService::class.java),
        mediaBrowserConnectionCallback,
        null
    ).apply {
        LLog.i(TAG, "connect")
        connect()
    }
    private lateinit var mediaController: MediaControllerCompat

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callback)
    }

    fun sendCommand(command: String, parameters: Bundle?) =
        sendCommand(command, parameters) { _, _ -> }

    fun sendCommand(
        command: String,
        parameters: Bundle?,
        resultCallback: ((Int, Bundle?) -> Unit)
    ) = if (mediaBrowser.isConnected) {
        mediaController.sendCommand(command, parameters, object : ResultReceiver(Handler()) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                LLog.i(TAG,"cmd:$command, rc:$resultCode, bundle:$resultData")
                resultCallback(resultCode, resultData)
            }
        })
        true
    } else {
        false
    }

    /**
     * This method takes a [MediaItemData] and does one of the following:
     * - If the item is *not* the active item, then play it directly.
     * - If the item *is* the active item, check whether "pause" is a permitted command. If it is,
     *   then pause playback, otherwise send "play" to resume playback.
     */
    fun playMedia(mediaId: String, pauseAllowed: Boolean = true) {
        val nowPlaying = _nowPlaying.value
        val controller = transportControls
        val isPrepared = playbackState.value?.isPrepared ?: false
        if (isPrepared && mediaId == nowPlaying?.id) {
            playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying -> {
                        if (pauseAllowed) controller.pause() else Unit
                    }
                    playbackState.isPlayEnabled -> {
                        controller.play()
                    }
                    else -> {
                        LLog.i(
                            TAG,
                            "Playable item clicked but neither play nor pause are enabled!(mediaId=${mediaId})"
                        )
                    }
                }
            }
        } else {
            controller.playFromMediaId(mediaId, null)
        }
    }

    //media operators
    fun play() = transportControls.play()
    fun pause() = transportControls.pause()
    fun skipToNext() = transportControls.skipToNext()
    fun skipToPrevious() = transportControls.skipToPrevious()
    fun seekTo(t: Long) = transportControls.seekTo(t)
    fun setRepeatMode(@PlaybackStateCompat.RepeatMode mode: Int) = transportControls.setRepeatMode(mode)

    private inner class MediaBrowserConnectionCallback(private val context: Context) :
        MediaBrowserCompat.ConnectionCallback() {
        /**
         * Invoked after [MediaBrowserCompat.connect] when the request has successfully
         * completed.
         */
        override fun onConnected() {
            // Get a MediaController for the MediaSession.
            LLog.i(TAG, "media browser onConnected")
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }

            _isConnected.postValue(true)
        }

        /**
         * Invoked when the client is disconnected from the media browser.
         */
        override fun onConnectionSuspended() {
            _isConnected.postValue(false)
        }

        /**
         * Invoked when the connection to the media browser failed.
         */
        override fun onConnectionFailed() {
            _isConnected.postValue(false)
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            LLog.i(TAG, "onPlaybackStateChanged:$state")
            _playbackState.postValue(state ?: EMPTY_PLAYBACK_STATE)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            LLog.i(TAG, "onMetadataChanged:${metadata?.id}")
            // When ExoPlayer stops we will receive a callback with "empty" metadata. This is a
            // metadata object which has been instantiated with default values. The default value
            // for media ID is null so we assume that if this value is null we are not playing
            // anything.
            _nowPlaying.postValue(
                if (metadata?.id == null) {
                    NOTHING_PLAYING
                } else {
                    metadata
                }
            )
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            LLog.i(TAG, "onQueueChanged:$queue")
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            LLog.i(TAG, "onSessionEvent:$event")
            super.onSessionEvent(event, extras)
            when (event) {
                NETWORK_FAILURE -> _networkFailure.postValue(true)
            }
        }

        /**
         * Normally if a [MediaBrowserServiceCompat] drops its connection the callback comes via
         * [MediaControllerCompat.Callback] (here). But since other connection status events
         * are sent to [MediaBrowserCompat.ConnectionCallback], we catch the disconnect here and
         * send it on to the other callback.
         */
        override fun onSessionDestroyed() {
            LLog.i(TAG, "onSessionDestroyed")
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }
}

@Suppress("PropertyName")
val EMPTY_PLAYBACK_STATE: PlaybackStateCompat = PlaybackStateCompat.Builder()
    .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
    .build()

@Suppress("PropertyName")
val NOTHING_PLAYING: MediaMetadataCompat = MediaMetadataCompat.Builder()
    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
    .build()
