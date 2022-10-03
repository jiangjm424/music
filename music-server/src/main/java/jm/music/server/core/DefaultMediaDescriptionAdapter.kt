package jm.music.server.core

import android.app.PendingIntent
import android.graphics.Bitmap
import com.google.android.exoplayer2.Player

class DefaultMediaDescriptionAdapter(private val pendingIntent: PendingIntent?) :
    HnPlaybackNotificationManager.MediaDescriptionAdapter {
    override fun getCurrentContentTitle(player: Player?): CharSequence? {
        TODO("Not yet implemented")
    }

    override fun createCurrentContentIntent(player: Player?): PendingIntent? {
        TODO("Not yet implemented")
    }

    override fun getCurrentContentText(player: Player?): CharSequence? {
        TODO("Not yet implemented")
    }

    override fun getCurrentLargeIcon(
        player: Player?,
        callback: HnPlaybackNotificationManager.BitmapCallback?
    ): Bitmap? {
        TODO("Not yet implemented")
    }
}
