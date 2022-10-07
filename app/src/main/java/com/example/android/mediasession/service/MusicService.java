/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.mediasession.service;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;

import com.example.android.mediasession.service.contentcatalogs.MusicLibrary;
import com.example.android.mediasession.service.contentcatalogs.MusicSource;
import com.example.android.mediasession.service.notifications.MediaNotificationManager;
import com.example.android.mediasession.service.players.ExoPlayerAdapter;
import com.example.android.mediasession.service.players.MediaPlayerAdapter;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends MediaBrowserServiceCompat {

    private static final String TAG = MusicService.class.getSimpleName();

    private MediaSessionCompat mSession;
    private PlayerAdapter mPlayback;
    private MediaNotificationManager mMediaNotificationManager;
    private MediaSessionCallback mCallback;
    private boolean mServiceInStartedState;
    private MusicSource musicSource = new MusicSource();

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a new MediaSession.
        mSession = new MediaSessionCompat(this, "MusicService");
        mCallback = new MediaSessionCallback(musicSource);
        mSession.setCallback(mCallback);
        mSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        setSessionToken(mSession.getSessionToken());

        mMediaNotificationManager = new MediaNotificationManager(this);

        mPlayback = new ExoPlayerAdapter(this, new MediaPlayerListener(musicSource));
        Log.d(TAG, "onCreate: MusicService creating MediaSession, and MediaNotificationManager");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        mMediaNotificationManager.onDestroy();
        mPlayback.stop();
        mSession.release();
        Log.d(TAG, "onDestroy: MediaPlayerAdapter stopped, and MediaSession released");
    }

    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName,
                                 int clientUid,
                                 Bundle rootHints) {
        return new BrowserRoot(MusicLibrary.getRoot(), null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result, @NonNull Bundle options) {
        Log.i(TAG, "onLoadChildren:" + parentId + " opt:" + options);
        super.onLoadChildren(parentId, result, options);
    }

    @Override
    public void onLoadChildren(
            final String parentMediaId,
            final Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.i(TAG, "onLoadChildren:" + parentMediaId);
        result.sendResult(musicSource.getMediaItems());
    }

    // MediaSession Callback: Transport Controls -> MediaPlayerAdapter
    // client 方法调用时对应的响应

    public class MediaSessionCallback extends MediaSessionCompat.Callback {
        private MusicSource source;

        public MediaSessionCallback(MusicSource s) {
            source = s;
        }

        private MediaMetadataCompat mPreparedMedia;

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
        }

        @Override
        public void onPrepare() {
            Log.i(TAG, "onPrepare");
            if (!source.isReady()) {
                // Nothing to play.
                return;
            }

            mPreparedMedia = source.currentItem();
            mSession.setMetadata(mPreparedMedia);

            if (!mSession.isActive()) {
                mSession.setActive(true);
            }
        }

        @Override
        public void onPlay() {
            Log.i(TAG, "onPlay");
            if (!isReadyToPlay()) {
                // Nothing to play.
                return;
            }

            if (mPreparedMedia == null) {
                onPrepare();
            }

            mPlayback.playFromMedia(mPreparedMedia);
            Log.d(TAG, "onPlayFromMediaId: MediaSession active");
        }

        @Override
        public void onPause() {
            Log.i(TAG, "onPause");
            mPlayback.pause();
        }

        @Override
        public void onStop() {
            Log.i(TAG, "onStop");
            mPlayback.stop();
            mSession.setActive(false);
        }

        @Override
        public void onSkipToNext() {
            Log.i(TAG, "onSkipToNext");
            mPreparedMedia = source.next();
            if (mPreparedMedia != null) {
                mSession.setMetadata(mPreparedMedia);
            }
            onPlay();
        }

        @Override
        public void onSkipToPrevious() {
            Log.i(TAG, "onSkipToPrevious");
            mPreparedMedia = source.prev();if (mPreparedMedia != null) {
                mSession.setMetadata(mPreparedMedia);
            }
            onPlay();
        }

        @Override
        public void onSeekTo(long pos) {
            Log.i(TAG, "onSeekTo:" + pos);
            mPlayback.seekTo(pos);
        }

        private boolean isReadyToPlay() {
            Log.i(TAG, "isReadyToPlay");
            return (musicSource.isReady());
        }
    }

    // MediaPlayerAdapter Callback: MediaPlayerAdapter state -> MusicService.
    public class MediaPlayerListener extends PlaybackInfoListener {
        private MusicSource source;

        private final ServiceManager mServiceManager;

        public MediaPlayerListener(MusicSource s) {
            source = s;
            mServiceManager = new ServiceManager();
        }

        @Override
        public void onPlaybackCompleted() {
            Log.i(TAG, "play complete");
//            mPlayback.play();
            MediaMetadataCompat a = source.next();
            if (a!=null) {
                Log.i("jiang","play next:"+a.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                mSession.setMetadata(a);
                mPlayback.playFromMedia(a);
            }

        }

        @Override
        public void onPlaybackStateChange(PlaybackStateCompat state) {
            // Report the state to the MediaSession.
            Log.i("jiang","service onPlaybackStateChange:"+state);
            mSession.setPlaybackState(state);

            // Manage the started state of this service.
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    mServiceManager.moveServiceToStartedState(state);
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    mServiceManager.updateNotificationForPause(state);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    mServiceManager.moveServiceOutOfStartedState(state);
                    break;
            }
        }

        class ServiceManager {

            private void moveServiceToStartedState(PlaybackStateCompat state) {
                Notification notification =
                        mMediaNotificationManager.getNotification(
                                mPlayback.getCurrentMedia(), state, getSessionToken());

                if (!mServiceInStartedState) {
                    ContextCompat.startForegroundService(
                            MusicService.this,
                            new Intent(MusicService.this, MusicService.class));
                    mServiceInStartedState = true;
                }

                startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);
            }

            private void updateNotificationForPause(PlaybackStateCompat state) {
                stopForeground(false);
                Notification notification =
                        mMediaNotificationManager.getNotification(
                                mPlayback.getCurrentMedia(), state, getSessionToken());
                mMediaNotificationManager.getNotificationManager()
                        .notify(MediaNotificationManager.NOTIFICATION_ID, notification);
            }

            private void moveServiceOutOfStartedState(PlaybackStateCompat state) {
                stopForeground(true);
                stopSelf();
                mServiceInStartedState = false;
            }
        }

    }

}