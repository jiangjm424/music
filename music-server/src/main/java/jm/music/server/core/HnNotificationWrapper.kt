package jm.music.server.core

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.Player
import jm.music.server.R
import kotlinx.coroutines.*

const val NOW_PLAYING_CHANNEL_ID = "com.example.android.uamp.media.NOW_PLAYING"
const val NOW_PLAYING_NOTIFICATION_ID = 0xb339 // Arbitrary number used to identify our notification

/**
 * A wrapper class for ExoPlayer's HnPlaybackNotificationManager. It sets up the notification shown to
 * the user during audio playback and provides track metadata, such as track title and icon image.
 */
internal class HnNotificationWrapper(
    context: Context,
    sessionToken: MediaSessionCompat.Token,
    private val serviceScope: ServiceCoroutineScope,
    notificationListener: HnPlaybackNotificationManager.NotificationListener
) {

    private var player: Player? = null
    private val serviceJob = SupervisorJob()
    private val notificationManager: HnPlaybackNotificationManager
    private val platformNotificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)

        val builder = HnPlaybackNotificationManager.Builder(
            context,
            NOW_PLAYING_NOTIFICATION_ID,
            NOW_PLAYING_CHANNEL_ID
        )
        with(builder) {
            setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
            setNotificationListener(notificationListener)
            setChannelNameResourceId(R.string.notify_1)
            setChannelDescriptionResourceId(R.string.notify_1)
        }
        notificationManager = builder.build()
        notificationManager.setMediaSessionToken(sessionToken)
        notificationManager.setSmallIcon(R.drawable.exo_media_action_repeat_off)
        notificationManager.setUseRewindAction(false)
        notificationManager.setUseFastForwardAction(false)
    }

    fun hideNotification() {
        notificationManager.setPlayer(null)
    }

    fun showNotificationForPlayer(player: Player) {
        notificationManager.setPlayer(player)
    }

    private inner class DescriptionAdapter(private val controller: MediaControllerCompat) :
        HnPlaybackNotificationManager.MediaDescriptionAdapter {

        var currentIconUri: Uri? = null
        var currentBitmap: Bitmap? = null

        override fun createCurrentContentIntent(player: Player?): PendingIntent? =
            controller.sessionActivity

        override fun getCurrentContentText(player: Player?) =
            controller.metadata.description.subtitle.toString()

        override fun getCurrentContentTitle(player: Player?) =
            controller.metadata.description.title.toString()

        override fun getCurrentLargeIcon(
            player: Player?,
            callback: HnPlaybackNotificationManager.BitmapCallback?
        ): Bitmap? {
            val iconUri = controller.metadata.description.iconUri
            return if (currentIconUri != iconUri || currentBitmap == null) {

                // Cache the bitmap for the current song so that successive calls to
                // `getCurrentLargeIcon` don't cause the bitmap to be recreated.
                currentIconUri = iconUri
                serviceScope.launch {
                    currentBitmap = iconUri?.let {
                        resolveUriAsBitmap(it)
                    }
                    currentBitmap?.let { callback?.onBitmap(it) }
                }
                null
            } else {
                currentBitmap
            }
        }

        private suspend fun resolveUriAsBitmap(uri: Uri): Bitmap? {
            return withContext(Dispatchers.IO) {
                // Block on downloading artwork.
//                Glide.with(context).applyDefaultRequestOptions(glideOptions)
//                    .asBitmap()
//                    .load(uri)
//                    .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
//                    .get()
                null
            }
        }
    }
}

const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px

//private val glideOptions = RequestOptions()
//    .fallback(R.drawable.default_art)
//    .diskCacheStrategy(DiskCacheStrategy.DATA)

private const val MODE_READ_ONLY = "r"
