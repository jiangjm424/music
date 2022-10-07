package com.example.android.mediasession.service.contentcatalogs

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat

class MusicSource {

    private val data = HashMap<String, List<MediaMetadataCompat>>()
    private var index: Int = 0

    private var playMode = PlaybackStateCompat.REPEAT_MODE_ALL

    init {
        data["/"] = root()
    }

    private fun root(): List<MediaMetadataCompat> {
        val a = mutableListOf<MediaMetadataCompat>()
        repeat(3) {
            a.add(
                MediaMetadataCompat.Builder().apply {
                    putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "mediaId$it")
                    putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "album$it")
                    putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "artist$it")
//                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
//                                 -1)
                    putString(
                        MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                        "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/01_-_Intro_-_The_Way_Of_Waking_Up_feat_Alan_Watts.mp3"
                    )
                    putString(MediaMetadataCompat.METADATA_KEY_GENRE, "genre$it")
//                    putString(
//                        MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
//                        getAlbumArtUri(albumArtResName)
//                    )
//                    putString(
//                        MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
//                        getAlbumArtUri(albumArtResName)
//                    )
                    putString(MediaMetadataCompat.METADATA_KEY_TITLE, "title$it")
                }.build()
//                )
            )
        }
        return a
    }

    val items: List<MediaMetadataCompat> = data["/"] ?: emptyList()
    fun prev(): MediaMetadataCompat? {
        return when (playMode) {
            PlaybackStateCompat.REPEAT_MODE_NONE -> null
            PlaybackStateCompat.REPEAT_MODE_ONE -> {
                items[index]
            }
            PlaybackStateCompat.REPEAT_MODE_ALL -> {
                var i = index - 1 + items.size
                i %= items.size
                index = i
                items[i]
            }
            else -> null
        }
    }

    fun next(): MediaMetadataCompat? {
        return when (playMode) {
            PlaybackStateCompat.REPEAT_MODE_NONE -> null
            PlaybackStateCompat.REPEAT_MODE_ONE -> {
                items[index]
            }
            PlaybackStateCompat.REPEAT_MODE_ALL -> {
                var i = index + 1
                i %= items.size
                index = i
                items[i]
            }
            else -> null
        }
    }

    fun currentItem() = items[index]
    fun isReady() = items.isNotEmpty()
    fun transform(item: MediaBrowserCompat.MediaItem): MediaMetadataCompat {
        val builder = MediaMetadataCompat.Builder()
//        builder.putString("")
        return builder.build()

    }

    fun getMediaItems(): List<MediaBrowserCompat.MediaItem>? {
        val result: MutableList<MediaBrowserCompat.MediaItem> = ArrayList()
        for (metadata in items) {
            result.add(
                MediaBrowserCompat.MediaItem(
                    metadata.description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                )
            )
        }
        return result
    }
}
