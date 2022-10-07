package com.example.android.mediasession.service.players

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.android.mediasession.service.PlaybackInfoListener
import com.example.android.mediasession.service.PlayerAdapter
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util

class ExoPlayerAdapter(context: Context, private val playbackInfoListener: PlaybackInfoListener) :
    PlayerAdapter(context) {
    companion object {
        private const val TAG = "ExoPlayerAdapter"
    }

    private val dataSourceFactory: DefaultDataSourceFactory by lazy {
        DefaultDataSourceFactory(
            /* context= */ context,
            Util.getUserAgent(/* context= */ context, "health"), /* listener= */
            null
        )
    }
    private val mFilename: String? = null
    private var mCurrentMedia: MediaMetadataCompat? = null

    @PlaybackStateCompat.State
    private var mState = PlaybackStateCompat.STATE_NONE
    private val mCurrentMediaPlayedToCompletion = false

    private val playerListener = PlayerEventListener()
    private val exoPlayer: ExoPlayer by lazy {
        SimpleExoPlayer.Builder(context).build().apply {
            addListener(playerListener)
        }
    }

    override fun playFromMedia(metadata: MediaMetadataCompat) {
        mCurrentMedia = metadata
        val ds = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(metadata.description.mediaUri!!)
        exoPlayer.stop(true)
        exoPlayer.playWhenReady = false
        exoPlayer.prepare(ds)
        play()
    }

    override fun getCurrentMedia(): MediaMetadataCompat {
        return mCurrentMedia ?: MediaMetadataCompat.Builder().build()
    }

    override fun isPlaying(): Boolean {
        return exoPlayer.isPlaying
    }

    override fun onPlay() {
        exoPlayer.playWhenReady = true
        setNewState(PlaybackStateCompat.STATE_PLAYING)

    }

    override fun onPause() {
        exoPlayer.playWhenReady = false
        setNewState(PlaybackStateCompat.STATE_PAUSED)
    }

    override fun onStop() {
        exoPlayer.playWhenReady = false
        exoPlayer.seekTo(C.TIME_UNSET)
        setNewState(PlaybackStateCompat.STATE_STOPPED)
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        setNewState(mState)
    }

    override fun setVolume(volume: Float) {
        exoPlayer.audioComponent?.volume = volume
    }

    private fun setNewState(@PlaybackStateCompat.State newPlaybackState: Int) {
        mState = newPlaybackState
        val stateBuilder = PlaybackStateCompat.Builder()
        stateBuilder.setActions(getAvailableActions())
        stateBuilder.setState(
            mState,
            exoPlayer.currentPosition,
            1.0f,
            SystemClock.elapsedRealtime()
        )
        playbackInfoListener.onPlaybackStateChange(stateBuilder.build())
    }

    @PlaybackStateCompat.Actions
    private fun getAvailableActions(): Long {
        var actions = (PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        actions = when (mState) {
            PlaybackStateCompat.STATE_STOPPED -> actions or (PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_PAUSE)
            PlaybackStateCompat.STATE_PLAYING -> actions or (PlaybackStateCompat.ACTION_STOP
                    or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_SEEK_TO)
            PlaybackStateCompat.STATE_PAUSED -> actions or (PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_STOP)
            else -> actions or (PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_STOP
                    or PlaybackStateCompat.ACTION_PAUSE)
        }
        return actions
    }

    private inner class PlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.i(TAG, "onPlayerStateChanged:$playbackState, ready:$playWhenReady")
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    if (playbackState == Player.STATE_READY) {
                        if (!playWhenReady) {
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    setNewState(PlaybackStateCompat.STATE_PAUSED)
                    playbackInfoListener.onPlaybackCompleted()
                }
                else -> {
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
        }
    }

}