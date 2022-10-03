package jm.music.server.core

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BadgeIconType
import androidx.core.app.NotificationManagerCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.NotificationUtil
import com.google.android.exoplayer2.util.NotificationUtil.Importance
import com.google.android.exoplayer2.util.Util
import jm.music.server.LLog
import jm.music.server.R
import jm.music.server.core.HnPlaybackNotificationManager.Builder
import jm.music.server.core.HnPlaybackNotificationManager.MediaDescriptionAdapter
import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.*


/**
 * Starts, updates and cancels a media style notification reflecting the player state. The actions
 * included in the notification can be customized along with their drawables, as described below.
 *
 *
 * The notification is cancelled when `null` is passed to [.setPlayer] or
 * when the notification is dismissed by the user.
 *
 *
 * If the player is released it must be removed from the manager by calling `setPlayer(null)`.
 *
 * <h2>Action customization</h2>
 *
 * Playback actions can be included or omitted as follows:
 *
 *
 *  * **`usePlayPauseActions`** - Sets whether the play and pause actions are used.
 *
 *  * Corresponding setter: [.setUsePlayPauseActions]
 *  * Default: `true`
 *
 *  * **`useRewindAction`** - Sets whether the rewind action is used.
 *
 *  * Corresponding setter: [.setUseRewindAction]
 *  * Default: `true`
 *
 *  * **`useRewindActionInCompactView`** - If `useRewindAction` is `true`,
 * sets whether the rewind action is also used in compact view (including the lock screen
 * notification). Else does nothing.
 *
 *  * Corresponding setter: [.setUseRewindActionInCompactView]
 *  * Default: `false`
 *
 *  * **`useFastForwardAction`** - Sets whether the fast forward action is used.
 *
 *  * Corresponding setter: [.setUseFastForwardAction]
 *  * Default: `true`
 *
 *  * **`useFastForwardActionInCompactView`** - If `useFastForwardAction` is
 * `true`, sets whether the fast forward action is also used in compact view (including
 * the lock screen notification). Else does nothing.
 *
 *  * Corresponding setter: [.setUseFastForwardActionInCompactView]
 *  * Default: `false`
 *
 *  * **`usePreviousAction`** - Whether the previous action is used.
 *
 *  * Corresponding setter: [.setUsePreviousAction]
 *  * Default: `true`
 *
 *  * **`usePreviousActionInCompactView`** - If `usePreviousAction` is `true`, sets whether the previous action is also used in compact view (including the lock
 * screen notification). Else does nothing.
 *
 *  * Corresponding setter: [.setUsePreviousActionInCompactView]
 *  * Default: `false`
 *
 *  * **`useNextAction`** - Whether the next action is used.
 *
 *  * Corresponding setter: [.setUseNextAction]
 *  * Default: `true`
 *
 *  * **`useNextActionInCompactView`** - If `useNextAction` is `true`, sets
 * whether the next action is also used in compact view (including the lock screen
 * notification). Else does nothing.
 *
 *  * Corresponding setter: [.setUseNextActionInCompactView]
 *  * Default: `false`
 *
 *  * **`useStopAction`** - Sets whether the stop action is used.
 *
 *  * Corresponding setter: [.setUseStopAction]
 *  * Default: `false`
 *
 *
 *
 * <h2>Overriding drawables</h2>
 *
 * The drawables used by HnPlaybackNotificationManager can be overridden by drawables with the same
 * names defined in your application. The drawables that can be overridden are:
 *
 *
 *  * **`exo_notification_small_icon`** - The icon passed by default to [       ][NotificationCompat.Builder.setSmallIcon]. A different icon can also be specified
 * programmatically by calling [.setSmallIcon].
 *  * **`exo_notification_play`** - The play icon.
 *  * **`exo_notification_pause`** - The pause icon.
 *  * **`exo_notification_rewind`** - The rewind icon.
 *  * **`exo_notification_fastforward`** - The fast forward icon.
 *  * **`exo_notification_previous`** - The previous icon.
 *  * **`exo_notification_next`** - The next icon.
 *  * **`exo_notification_stop`** - The stop icon.
 *
 *
 *
 * Alternatively, the action icons can be set programatically by using the [Builder].
 *
 *
 * Unlike the drawables above, the large icon (i.e. the icon passed to [ ][NotificationCompat.Builder.setLargeIcon] cannot be overridden in this way. Instead, the
 * large icon is obtained from the [MediaDescriptionAdapter] passed to [ ][Builder.Builder].
 */
class HnPlaybackNotificationManager internal constructor(
    context: Context,
    channelId: String?,
    notificationId: Int,
    mediaDescriptionAdapter: MediaDescriptionAdapter,
    notificationListener: NotificationListener?,
    customActionReceiver: CustomActionReceiver?,
    smallIconResourceId: Int,
    playActionIconResourceId: Int,
    pauseActionIconResourceId: Int,
    stopActionIconResourceId: Int,
    rewindActionIconResourceId: Int,
    fastForwardActionIconResourceId: Int,
    previousActionIconResourceId: Int,
    nextActionIconResourceId: Int,
    groupKey: String?
) {
    /** An adapter to provide content assets of the media currently playing.  */
    interface MediaDescriptionAdapter {
        /**
         * Gets the content title for the current media item.
         *
         *
         * See [NotificationCompat.Builder.setContentTitle].
         *
         * @param player The [Player] for which a notification is being built.
         * @return The content title for the current media item.
         */
        fun getCurrentContentTitle(player: Player?): CharSequence?

        /**
         * Creates a content intent for the current media item.
         *
         *
         * See [NotificationCompat.Builder.setContentIntent].
         *
         * @param player The [Player] for which a notification is being built.
         * @return The content intent for the current media item, or null if no intent should be fired.
         */
        fun createCurrentContentIntent(player: Player?): PendingIntent?

        /**
         * Gets the content text for the current media item.
         *
         *
         * See [NotificationCompat.Builder.setContentText].
         *
         * @param player The [Player] for which a notification is being built.
         * @return The content text for the current media item, or null if no context text should be
         * displayed.
         */
        fun getCurrentContentText(player: Player?): CharSequence?

        /**
         * Gets the content sub text for the current media item.
         *
         *
         * See [NotificationCompat.Builder.setSubText].
         *
         * @param player The [Player] for which a notification is being built.
         * @return The content subtext for the current media item, or null if no subtext should be
         * displayed.
         */
        fun getCurrentSubText(player: Player?): CharSequence? {
            return null
        }

        /**
         * Gets the large icon for the current media item.
         *
         *
         * When a bitmap needs to be loaded asynchronously, a placeholder bitmap (or null) should be
         * returned. The actual bitmap should be passed to the [BitmapCallback] once it has been
         * loaded. Because the adapter may be called multiple times for the same media item, bitmaps
         * should be cached by the app and returned synchronously when possible.
         *
         *
         * See [NotificationCompat.Builder.setLargeIcon].
         *
         * @param player The [Player] for which a notification is being built.
         * @param callback A [BitmapCallback] to provide a [Bitmap] asynchronously.
         * @return The large icon for the current media item, or null if the icon will be returned
         * through the [BitmapCallback] or if no icon should be displayed.
         */
        fun getCurrentLargeIcon(player: Player?, callback: BitmapCallback?): Bitmap?
    }

    /** Defines and handles custom actions.  */
    interface CustomActionReceiver {
        /**
         * Gets the actions handled by this receiver.
         *
         *
         * If multiple [HnPlaybackNotificationManager] instances are in use at the same time, the
         * `instanceId` must be set as an intent extra with key [ ][HnPlaybackNotificationManager.EXTRA_INSTANCE_ID] to avoid sending the action to every custom
         * action receiver. It's also necessary to ensure something is different about the actions. This
         * may be any of the [Intent] attributes considered by [Intent.filterEquals], or
         * different request code integers when creating the [PendingIntent]s with [ ][PendingIntent.getBroadcast]. The easiest approach is to use the `instanceId` as the
         * request code.
         *
         * @param context The [Context].
         * @param instanceId The instance id of the [HnPlaybackNotificationManager].
         * @return A map of custom actions.
         */
        fun createCustomActions(
            context: Context?,
            instanceId: Int
        ): Map<String, NotificationCompat.Action>

        /**
         * Gets the actions to be included in the notification given the current player state.
         *
         * @param player The [Player] for which a notification is being built.
         * @return The actions to be included in the notification.
         */
        fun getCustomActions(player: Player?): List<String>?

        /**
         * Called when a custom action has been received.
         *
         * @param player The player.
         * @param action The action from [Intent.getAction].
         * @param intent The received [Intent].
         */
        fun onCustomAction(player: Player?, action: String?, intent: Intent?)
    }

    /** A listener for changes to the notification.  */
    interface NotificationListener {
        /**
         * Called after the notification has been cancelled.
         *
         * @param notificationId The id of the notification which has been cancelled.
         * @param dismissedByUser `true` if the notification is cancelled because the user
         * dismissed the notification.
         */
        fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {}

        /**
         * Called each time after the notification has been posted.
         *
         *
         * For a service, the `ongoing` flag can be used as an indicator as to whether it
         * should be in the foreground.
         *
         * @param notificationId The id of the notification which has been posted.
         * @param notification The [Notification].
         * @param ongoing Whether the notification is ongoing.
         */
        fun onNotificationPosted(
            notificationId: Int, notification: Notification, ongoing: Boolean
        ) {
        }
    }

    /** A builder for [HnPlaybackNotificationManager] instances.  */
    class Builder(context: Context, @IntRange(from = 1) notificationId: Int, channelId: String?) {
        protected val context: Context
        protected val notificationId: Int
        protected val channelId: String?
        protected var notificationListener: NotificationListener? = null
        protected var customActionReceiver: CustomActionReceiver? = null
        protected var mediaDescriptionAdapter: MediaDescriptionAdapter
        protected var channelNameResourceId = 0
        protected var channelDescriptionResourceId = 0
        protected var channelImportance: Int
        protected var smallIconResourceId: Int
        protected var rewindActionIconResourceId: Int
        protected var playActionIconResourceId: Int
        protected var pauseActionIconResourceId: Int
        protected var stopActionIconResourceId: Int
        protected var fastForwardActionIconResourceId: Int
        protected var previousActionIconResourceId: Int
        protected var nextActionIconResourceId: Int
        protected var groupKey: String? = null

        @Deprecated(
            """Use {@link #Builder(Context, int, String)} instead, then call {@link
     *     #setMediaDescriptionAdapter(MediaDescriptionAdapter)}."""
        )
        constructor(
            context: Context,
            notificationId: Int,
            channelId: String?,
            mediaDescriptionAdapter: MediaDescriptionAdapter
        ) : this(context, notificationId, channelId) {
            this.mediaDescriptionAdapter = mediaDescriptionAdapter
        }

        /**
         * The name of the channel. If set to a value other than `0`, the channel is automatically
         * created when [.build] is called. If the application has already created the
         * notification channel, then this method should not be called.
         *
         *
         * The default is `0`.
         *
         * @return This builder.
         */
        fun setChannelNameResourceId(channelNameResourceId: Int): HnPlaybackNotificationManager.Builder {
            this.channelNameResourceId = channelNameResourceId
            return this
        }

        /**
         * The description of the channel. Ignored if [.setChannelNameResourceId] is not
         * called with a value other than `0`. If the application has already created the
         * notification channel, then this method should not be called.
         *
         *
         * The default is `0`.
         *
         * @return This builder.
         */
        fun setChannelDescriptionResourceId(channelDescriptionResourceId: Int): HnPlaybackNotificationManager.Builder {
            this.channelDescriptionResourceId = channelDescriptionResourceId
            return this
        }

        /**
         * The importance of the channel. Ignored if [.setChannelNameResourceId] is not
         * called with a value other than `0`. If the application has already created the
         * notification channel, then this method should not be called.
         *
         *
         * The default is [NotificationUtil.IMPORTANCE_LOW].
         *
         * @return This builder.
         */
        fun setChannelImportance(channelImportance: Int): HnPlaybackNotificationManager.Builder {
            this.channelImportance = channelImportance
            return this
        }

        /**
         * The [NotificationListener] to be used.
         *
         *
         * The default is `null`.
         *
         * @return This builder.
         */
        fun setNotificationListener(notificationListener: NotificationListener?): HnPlaybackNotificationManager.Builder {
            this.notificationListener = notificationListener
            return this
        }

        /**
         * The [CustomActionReceiver] to be used.
         *
         *
         * The default is `null`.
         *
         * @return This builder.
         */
        fun setCustomActionReceiver(customActionReceiver: CustomActionReceiver?): HnPlaybackNotificationManager.Builder {
            this.customActionReceiver = customActionReceiver
            return this
        }

        /**
         * The resource id of the small icon of the notification shown in the status bar. See [ ][NotificationCompat.Builder.setSmallIcon].
         *
         *
         * The default is `R.drawable#exo_notification_small_icon`.
         *
         * @return This builder.
         */
        fun setSmallIconResourceId(smallIconResourceId: Int): HnPlaybackNotificationManager.Builder {
            this.smallIconResourceId = smallIconResourceId
            return this
        }

        /**
         * The resource id of the drawable to be used as the icon of action [.ACTION_PLAY].
         *
         *
         * The default is `R.drawable#exo_notification_play`.
         *
         * @return This builder.
         */
        fun setPlayActionIconResourceId(playActionIconResourceId: Int): HnPlaybackNotificationManager.Builder {
            this.playActionIconResourceId = playActionIconResourceId
            return this
        }

        /**
         * The resource id of the drawable to be used as the icon of action [.ACTION_PAUSE].
         *
         *
         * The default is `R.drawable#exo_notification_pause`.
         *
         * @return This builder.
         */
        fun setPauseActionIconResourceId(pauseActionIconResourceId: Int): HnPlaybackNotificationManager.Builder {
            this.pauseActionIconResourceId = pauseActionIconResourceId
            return this
        }

        /**
         * The resource id of the drawable to be used as the icon of action [.ACTION_STOP].
         *
         *
         * The default is `R.drawable#exo_notification_stop`.
         *
         * @return This builder.
         */
        fun setStopActionIconResourceId(stopActionIconResourceId: Int): HnPlaybackNotificationManager.Builder {
            this.stopActionIconResourceId = stopActionIconResourceId
            return this
        }

        /**
         * The resource id of the drawable to be used as the icon of action [.ACTION_REWIND].
         *
         *
         * The default is `R.drawable#exo_notification_rewind`.
         *
         * @return This builder.
         */
        fun setRewindActionIconResourceId(rewindActionIconResourceId: Int): HnPlaybackNotificationManager.Builder {
            this.rewindActionIconResourceId = rewindActionIconResourceId
            return this
        }

        /**
         * The resource id of the drawable to be used as the icon of action [ ][.ACTION_FAST_FORWARD].
         *
         *
         * The default is `R.drawable#exo_notification_fastforward`.
         *
         * @return This builder.
         */
        fun setFastForwardActionIconResourceId(fastForwardActionIconResourceId: Int): HnPlaybackNotificationManager.Builder {
            this.fastForwardActionIconResourceId = fastForwardActionIconResourceId
            return this
        }

        /**
         * The resource id of the drawable to be used as the icon of action [.ACTION_PREVIOUS].
         *
         *
         * The default is `R.drawable#exo_notification_previous`.
         *
         * @return This builder.
         */
        fun setPreviousActionIconResourceId(previousActionIconResourceId: Int): HnPlaybackNotificationManager.Builder {
            this.previousActionIconResourceId = previousActionIconResourceId
            return this
        }

        /**
         * The resource id of the drawable to be used as the icon of action [.ACTION_NEXT].
         *
         *
         * The default is `R.drawable#exo_notification_next`.
         *
         * @return This builder.
         */
        fun setNextActionIconResourceId(nextActionIconResourceId: Int): HnPlaybackNotificationManager.Builder {
            this.nextActionIconResourceId = nextActionIconResourceId
            return this
        }

        /**
         * The key of the group the media notification should belong to.
         *
         *
         * The default is `null`
         *
         * @return This builder.
         */
        fun setGroup(groupKey: String?): HnPlaybackNotificationManager.Builder {
            this.groupKey = groupKey
            return this
        }

        /**
         * The [MediaDescriptionAdapter] to be queried for the notification contents.
         *
         *
         * The default is [DefaultMediaDescriptionAdapter] with no [PendingIntent]
         *
         * @return This builder.
         */
        fun setMediaDescriptionAdapter(mediaDescriptionAdapter: MediaDescriptionAdapter): HnPlaybackNotificationManager.Builder {
            this.mediaDescriptionAdapter = mediaDescriptionAdapter
            return this
        }

        /** Builds the [HnPlaybackNotificationManager].  */
        fun build(): HnPlaybackNotificationManager {
            if (channelNameResourceId != 0) {
                NotificationUtil.createNotificationChannel(
                    context,
                    channelId!!,
                    channelNameResourceId,
                    channelDescriptionResourceId,
                    channelImportance
                )
            }
            return HnPlaybackNotificationManager(
                context,
                channelId,
                notificationId,
                mediaDescriptionAdapter,
                notificationListener,
                customActionReceiver,
                smallIconResourceId,
                playActionIconResourceId,
                pauseActionIconResourceId,
                stopActionIconResourceId,
                rewindActionIconResourceId,
                fastForwardActionIconResourceId,
                previousActionIconResourceId,
                nextActionIconResourceId,
                groupKey
            )
        }

        /**
         * Creates an instance.
         *
         * @param context The [Context].
         * @param notificationId The id of the notification to be posted. Must be greater than 0.
         * @param channelId The id of the notification channel.
         */
        init {
            Assertions.checkArgument(notificationId > 0)
            this.context = context
            this.notificationId = notificationId
            this.channelId = channelId
            channelImportance = NotificationUtil.IMPORTANCE_LOW
            mediaDescriptionAdapter = DefaultMediaDescriptionAdapter( /* pendingIntent= */null)
            smallIconResourceId = R.drawable.exo_media_action_repeat_off
            playActionIconResourceId = R.drawable.exo_media_action_repeat_off
            pauseActionIconResourceId = R.drawable.exo_media_action_repeat_off
            stopActionIconResourceId = R.drawable.exo_media_action_repeat_off
            rewindActionIconResourceId = R.drawable.exo_media_action_repeat_off
            fastForwardActionIconResourceId = R.drawable.exo_media_action_repeat_off
            previousActionIconResourceId = R.drawable.exo_media_action_repeat_off
            nextActionIconResourceId = R.drawable.exo_media_action_repeat_off
        }
    }

    /** Receives a [Bitmap].  */
    inner class BitmapCallback
    /** Create the receiver.  */ internal constructor(private val notificationTag: Int) {
        /**
         * Called when [Bitmap] is available.
         *
         * @param bitmap The bitmap to use as the large icon of the notification.
         */
        fun onBitmap(bitmap: Bitmap?) {
            bitmap?.let { postUpdateNotificationBitmap(it, notificationTag) }
        }
    }

    /**
     * Visibility of notification on the lock screen. One of [ ][NotificationCompat.VISIBILITY_PRIVATE], [NotificationCompat.VISIBILITY_PUBLIC] or [ ][NotificationCompat.VISIBILITY_SECRET].
     */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        NotificationCompat.VISIBILITY_PRIVATE,
        NotificationCompat.VISIBILITY_PUBLIC,
        NotificationCompat.VISIBILITY_SECRET
    )
    annotation class Visibility

    /**
     * Priority of the notification (required for API 25 and lower). One of [ ][NotificationCompat.PRIORITY_DEFAULT], [NotificationCompat.PRIORITY_MAX], [ ][NotificationCompat.PRIORITY_HIGH], [NotificationCompat.PRIORITY_LOW]or [ ][NotificationCompat.PRIORITY_MIN].
     */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        NotificationCompat.PRIORITY_DEFAULT,
        NotificationCompat.PRIORITY_MAX,
        NotificationCompat.PRIORITY_HIGH,
        NotificationCompat.PRIORITY_LOW,
        NotificationCompat.PRIORITY_MIN
    )
    annotation class Priority

    private val context: Context
    private val channelId: String?
    private val notificationId: Int
    private val mediaDescriptionAdapter: MediaDescriptionAdapter
    private val notificationListener: NotificationListener?
    private val customActionReceiver: CustomActionReceiver?
    private val mainHandler: Handler
    private val notificationManager: NotificationManagerCompat
    private val intentFilter: IntentFilter
    private val playerListener: Player.Listener
    private val notificationBroadcastReceiver: NotificationBroadcastReceiver
    private val playbackActions: Map<String, NotificationCompat.Action>
    private val customActions: Map<String, NotificationCompat.Action>
    private val dismissPendingIntent: PendingIntent
    private val instanceId: Int
    private var builder: NotificationCompat.Builder? = null
    private var builderActions: List<NotificationCompat.Action>? = null
    private var player: Player? = null
    private var isNotificationStarted = false
    private var currentNotificationTag = 0
    private var mediaSessionToken: MediaSessionCompat.Token? = null
    private var usePreviousAction: Boolean
    private var useNextAction: Boolean
    private var usePreviousActionInCompactView = false
    private var useNextActionInCompactView = false
    private var useRewindAction: Boolean
    private var useFastForwardAction: Boolean
    private var useRewindActionInCompactView = false
    private var useFastForwardActionInCompactView = false
    private var usePlayPauseActions: Boolean
    private var useStopAction = false
    private var badgeIconType: Int
    private var colorized: Boolean
    private var defaults: Int
    private var color: Int

    @DrawableRes
    private var smallIconResourceId: Int
    private var visibility: Int

    @HnPlaybackNotificationManager.Priority
    private var priority: Int
    private var useChronometer: Boolean
    private val groupKey: String?

    /**
     * Sets the [Player].
     *
     *
     * Setting the player starts a notification immediately unless the player is in [ ][Player.STATE_IDLE], in which case the notification is started as soon as the player transitions
     * away from being idle.
     *
     *
     * If the player is released it must be removed from the manager by calling `setPlayer(null)`. This will cancel the notification.
     *
     * @param player The [Player] to use, or `null` to remove the current player. Only
     * players which are accessed on the main thread are supported (`player.getApplicationLooper() == Looper.getMainLooper()`).
     */
    fun setPlayer(player: Player?) {
        Assertions.checkState(Looper.myLooper() == Looper.getMainLooper())
        Assertions.checkArgument(player == null || player.applicationLooper == Looper.getMainLooper())
        if (this.player === player) {
            return
        }
        if (this.player != null) {
            this.player!!.removeListener(playerListener)
            if (player == null) {
                stopNotification( /* dismissedByUser= */false)
            }
        }
        this.player = player
        if (player != null) {
            player.addListener(playerListener)
            postStartOrUpdateNotification()
        }
    }

    /**
     * Sets whether the next action should be used.
     *
     * @param useNextAction Whether to use the next action.
     */
    fun setUseNextAction(useNextAction: Boolean) {
        if (this.useNextAction != useNextAction) {
            this.useNextAction = useNextAction
            invalidate()
        }
    }

    /**
     * Sets whether the previous action should be used.
     *
     * @param usePreviousAction Whether to use the previous action.
     */
    fun setUsePreviousAction(usePreviousAction: Boolean) {
        if (this.usePreviousAction != usePreviousAction) {
            this.usePreviousAction = usePreviousAction
            invalidate()
        }
    }

    /**
     * If [useNextAction][.setUseNextAction] is `true`, sets whether the next action should
     * also be used in compact view. Has no effect if [useNextAction][.setUseNextAction] is
     * `false`.
     *
     *
     * If set to `true`, [ setUseFastForwardActionInCompactView][.setUseFastForwardActionInCompactView] is set to false.
     *
     * @param useNextActionInCompactView Whether to use the next action in compact view.
     */
    fun setUseNextActionInCompactView(useNextActionInCompactView: Boolean) {
        if (this.useNextActionInCompactView != useNextActionInCompactView) {
            this.useNextActionInCompactView = useNextActionInCompactView
            if (useNextActionInCompactView) {
                useFastForwardActionInCompactView = false
            }
            invalidate()
        }
    }

    /**
     * If [usePreviousAction][.setUsePreviousAction] is `true`, sets whether the previous
     * action should also be used in compact view. Has no effect if [ usePreviousAction][.setUsePreviousAction] is `false`.
     *
     *
     * If set to `true`, [ setUseRewindActionInCompactView][.setUseRewindActionInCompactView] is set to false.
     *
     * @param usePreviousActionInCompactView Whether to use the previous action in compact view.
     */
    fun setUsePreviousActionInCompactView(usePreviousActionInCompactView: Boolean) {
        if (this.usePreviousActionInCompactView != usePreviousActionInCompactView) {
            this.usePreviousActionInCompactView = usePreviousActionInCompactView
            if (usePreviousActionInCompactView) {
                useRewindActionInCompactView = false
            }
            invalidate()
        }
    }

    /**
     * Sets whether the fast forward action should be used.
     *
     * @param useFastForwardAction Whether to use the fast forward action.
     */
    fun setUseFastForwardAction(useFastForwardAction: Boolean) {
        if (this.useFastForwardAction != useFastForwardAction) {
            this.useFastForwardAction = useFastForwardAction
            invalidate()
        }
    }

    /**
     * Sets whether the rewind action should be used.
     *
     * @param useRewindAction Whether to use the rewind action.
     */
    fun setUseRewindAction(useRewindAction: Boolean) {
        if (this.useRewindAction != useRewindAction) {
            this.useRewindAction = useRewindAction
            invalidate()
        }
    }

    /**
     * Sets whether the fast forward action should also be used in compact view. Has no effect if
     * [.ACTION_FAST_FORWARD] is not enabled, for instance if the media is not seekable.
     *
     *
     * If set to `true`, [ setUseNextActionInCompactView][.setUseNextActionInCompactView] is set to false.
     *
     * @param useFastForwardActionInCompactView Whether to use the fast forward action in compact
     * view.
     */
    fun setUseFastForwardActionInCompactView(
        useFastForwardActionInCompactView: Boolean
    ) {
        if (this.useFastForwardActionInCompactView != useFastForwardActionInCompactView) {
            this.useFastForwardActionInCompactView = useFastForwardActionInCompactView
            if (useFastForwardActionInCompactView) {
                useNextActionInCompactView = false
            }
            invalidate()
        }
    }

    /**
     * Sets whether the rewind action should also be used in compact view. Has no effect if [ ][.ACTION_REWIND] is not enabled, for instance if the media is not seekable.
     *
     *
     * If set to `true`, [ setUsePreviousActionInCompactView][.setUsePreviousActionInCompactView] is set to false.
     *
     * @param useRewindActionInCompactView Whether to use the rewind action in compact view.
     */
    fun setUseRewindActionInCompactView(useRewindActionInCompactView: Boolean) {
        if (this.useRewindActionInCompactView != useRewindActionInCompactView) {
            this.useRewindActionInCompactView = useRewindActionInCompactView
            if (useRewindActionInCompactView) {
                usePreviousActionInCompactView = false
            }
            invalidate()
        }
    }

    /**
     * Sets whether the play and pause actions should be used.
     *
     * @param usePlayPauseActions Whether to use play and pause actions.
     */
    fun setUsePlayPauseActions(usePlayPauseActions: Boolean) {
        if (this.usePlayPauseActions != usePlayPauseActions) {
            this.usePlayPauseActions = usePlayPauseActions
            invalidate()
        }
    }

    /**
     * Sets whether the stop action should be used.
     *
     * @param useStopAction Whether to use the stop action.
     */
    fun setUseStopAction(useStopAction: Boolean) {
        if (this.useStopAction == useStopAction) {
            return
        }
        this.useStopAction = useStopAction
        invalidate()
    }

    /**
     * Sets the [MediaSessionCompat.Token].
     *
     * @param token The [MediaSessionCompat.Token].
     */
    fun setMediaSessionToken(token: MediaSessionCompat.Token?) {
        if (!Util.areEqual(mediaSessionToken, token)) {
            mediaSessionToken = token
            invalidate()
        }
    }

    /**
     * Sets the badge icon type of the notification.
     *
     *
     * See [NotificationCompat.Builder.setBadgeIconType].
     *
     * @param badgeIconType The badge icon type.
     */
    fun setBadgeIconType(@BadgeIconType badgeIconType: Int) {
        if (this.badgeIconType == badgeIconType) {
            return
        }
        when (badgeIconType) {
            NotificationCompat.BADGE_ICON_NONE, NotificationCompat.BADGE_ICON_SMALL, NotificationCompat.BADGE_ICON_LARGE -> this.badgeIconType =
                badgeIconType
            else -> throw IllegalArgumentException()
        }
        invalidate()
    }

    /**
     * Sets whether the notification should be colorized. When set, the color set with [ ][.setColor] will be used as the background color for the notification.
     *
     *
     * See [NotificationCompat.Builder.setColorized].
     *
     * @param colorized Whether to colorize the notification.
     */
    fun setColorized(colorized: Boolean) {
        if (this.colorized != colorized) {
            this.colorized = colorized
            invalidate()
        }
    }

    /**
     * Sets the defaults.
     *
     *
     * See [NotificationCompat.Builder.setDefaults].
     *
     * @param defaults The default notification options.
     */
    fun setDefaults(defaults: Int) {
        if (this.defaults != defaults) {
            this.defaults = defaults
            invalidate()
        }
    }

    /**
     * Sets the accent color of the notification.
     *
     *
     * See [NotificationCompat.Builder.setColor].
     *
     * @param color The color, in ARGB integer form like the constants in [Color].
     */
    fun setColor(color: Int) {
        if (this.color != color) {
            this.color = color
            invalidate()
        }
    }

    /**
     * Sets the priority of the notification required for API 25 and lower.
     *
     *
     * See [NotificationCompat.Builder.setPriority].
     *
     *
     * To set the priority for API levels above 25, you can create your own [ ] with a given importance level and pass the id of the channel to [ ][Builder.Builder].
     *
     * @param priority The priority which can be one of [NotificationCompat.PRIORITY_DEFAULT],
     * [NotificationCompat.PRIORITY_MAX], [NotificationCompat.PRIORITY_HIGH], [     ][NotificationCompat.PRIORITY_LOW] or [NotificationCompat.PRIORITY_MIN]. If not set
     * [NotificationCompat.PRIORITY_LOW] is used by default.
     */
    fun setPriority(@HnPlaybackNotificationManager.Priority priority: Int) {
        if (this.priority == priority) {
            return
        }
        when (priority) {
            NotificationCompat.PRIORITY_DEFAULT, NotificationCompat.PRIORITY_MAX, NotificationCompat.PRIORITY_HIGH, NotificationCompat.PRIORITY_LOW, NotificationCompat.PRIORITY_MIN -> this.priority =
                priority
            else -> throw IllegalArgumentException()
        }
        invalidate()
    }

    /**
     * Sets the small icon of the notification which is also shown in the system status bar.
     *
     *
     * See [NotificationCompat.Builder.setSmallIcon].
     *
     * @param smallIconResourceId The resource id of the small icon.
     */
    fun setSmallIcon(@DrawableRes smallIconResourceId: Int) {
        if (this.smallIconResourceId != smallIconResourceId) {
            this.smallIconResourceId = smallIconResourceId
            invalidate()
        }
    }

    /**
     * Sets whether the elapsed time of the media playback should be displayed.
     *
     *
     * Note that this setting only works if all of the following are true:
     *
     *
     *  * The media is [actively playing][Player.isPlaying].
     *  * The media is not [dynamically changing its][Player.isCurrentMediaItemDynamic] (like for example a live stream).
     *  * The media is not [interrupted by an ad][Player.isPlayingAd].
     *  * The media is played at [regular speed][Player.getPlaybackParameters].
     *  * The device is running at least API 21 (Lollipop).
     *
     *
     *
     * See [NotificationCompat.Builder.setUsesChronometer].
     *
     * @param useChronometer Whether to use chronometer.
     */
    fun setUseChronometer(useChronometer: Boolean) {
        if (this.useChronometer != useChronometer) {
            this.useChronometer = useChronometer
            invalidate()
        }
    }

    /**
     * Sets the visibility of the notification which determines whether and how the notification is
     * shown when the device is in lock screen mode.
     *
     *
     * See [NotificationCompat.Builder.setVisibility].
     *
     * @param visibility The visibility which must be one of [     ][NotificationCompat.VISIBILITY_PUBLIC], [NotificationCompat.VISIBILITY_PRIVATE] or
     * [NotificationCompat.VISIBILITY_SECRET].
     */
    fun setVisibility(@HnPlaybackNotificationManager.Visibility visibility: Int) {
        if (this.visibility == visibility) {
            return
        }
        when (visibility) {
            NotificationCompat.VISIBILITY_PRIVATE, NotificationCompat.VISIBILITY_PUBLIC, NotificationCompat.VISIBILITY_SECRET -> this.visibility =
                visibility
            else -> throw IllegalStateException()
        }
        invalidate()
    }

    /** Forces an update of the notification if already started.  */
    fun invalidate() {
        if (isNotificationStarted) {
            postStartOrUpdateNotification()
        }
    }

    private fun startOrUpdateNotification(player: Player, bitmap: Bitmap?) {
        val ongoing = getOngoing(player)
        builder = createNotification(player, builder, ongoing, bitmap)
        if (builder == null) {
            stopNotification( /* dismissedByUser= */false)
            return
        }
        val notification = builder!!.build()
        notificationManager.notify(notificationId, notification)
        if (!isNotificationStarted) {
            context.registerReceiver(notificationBroadcastReceiver, intentFilter)
        }
        if (notificationListener != null) {
            // Always pass true for ongoing with the first notification to tell a service to go into
            // foreground even when paused.
            notificationListener.onNotificationPosted(
                notificationId, notification, ongoing || !isNotificationStarted
            )
        }
        isNotificationStarted = true
    }

    private fun stopNotification(dismissedByUser: Boolean) {
        if (isNotificationStarted) {
            isNotificationStarted = false
            mainHandler.removeMessages(MSG_START_OR_UPDATE_NOTIFICATION)
            notificationManager.cancel(notificationId)
            context.unregisterReceiver(notificationBroadcastReceiver)
            notificationListener?.onNotificationCancelled(notificationId, dismissedByUser)
        }
    }

    /**
     * Creates the notification given the current player state.
     *
     * @param player The player for which state to build a notification.
     * @param builder The builder used to build the last notification, or `null`. Re-using the
     * builder when possible can prevent notification flicker when `Util#SDK_INT` &lt; 21.
     * @param ongoing Whether the notification should be ongoing.
     * @param largeIcon The large icon to be used.
     * @return The [NotificationCompat.Builder] on which to call [     ][NotificationCompat.Builder.build] to obtain the notification, or `null` if no
     * notification should be displayed.
     */
    private fun createNotification(
        player: Player,
        builder: NotificationCompat.Builder?,
        ongoing: Boolean,
        largeIcon: Bitmap?
    ): NotificationCompat.Builder? {
        var builder = builder
        var largeIcon = largeIcon
        if (player.playbackState == Player.STATE_IDLE && player.currentTimeline.isEmpty) {
            builderActions = null
            return null
        }
        val actionNames = getActions(player)
        val actions: MutableList<NotificationCompat.Action> = ArrayList(actionNames.size)
        for (i in actionNames.indices) {
            val actionName = actionNames[i]
            val action =
                if (playbackActions.containsKey(actionName)) playbackActions[actionName] else customActions[actionName]
            if (action != null) {
                actions.add(action)
            }
        }
        if (builder == null || actions != builderActions) {
            builder = NotificationCompat.Builder(context, channelId!!)
            builderActions = actions
            for (i in actions.indices) {
                builder.addAction(actions[i])
            }
        }
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
        if (mediaSessionToken != null) {
            mediaStyle.setMediaSession(mediaSessionToken)
        }
        mediaStyle.setShowActionsInCompactView(*getActionIndicesForCompactView(actionNames, player))
        // Configure dismiss action prior to API 21 ('x' button).
        mediaStyle.setShowCancelButton(!ongoing)
        mediaStyle.setCancelButtonIntent(dismissPendingIntent)
        builder.setStyle(mediaStyle)

        // Set intent which is sent if the user selects 'clear all'
        builder.setDeleteIntent(dismissPendingIntent)

        // Set notification properties from getters.
        builder
            .setBadgeIconType(badgeIconType)
            .setOngoing(ongoing)
            .setColor(color)
            .setColorized(colorized)
            .setSmallIcon(smallIconResourceId)
            .setVisibility(visibility)
            .setPriority(priority)
            .setDefaults(defaults)

        // Changing "showWhen" causes notification flicker if SDK_INT < 21.
        if (Util.SDK_INT >= 21 && useChronometer
            && player.isPlaying
            && !player.isPlayingAd
            && !player.isCurrentMediaItemDynamic
            && player.playbackParameters.speed == 1f
        ) {
            builder
                .setWhen(System.currentTimeMillis() - player.contentPosition)
                .setShowWhen(true)
                .setUsesChronometer(true)
        } else {
            builder.setShowWhen(false).setUsesChronometer(false)
        }

        // Set media specific notification properties from MediaDescriptionAdapter.
        builder.setContentTitle(mediaDescriptionAdapter.getCurrentContentTitle(player))
        builder.setContentText(mediaDescriptionAdapter.getCurrentContentText(player))
        builder.setSubText(mediaDescriptionAdapter.getCurrentSubText(player))
        if (largeIcon == null) {
            largeIcon = mediaDescriptionAdapter.getCurrentLargeIcon(
                player, BitmapCallback(++currentNotificationTag)
            )
        }
        setLargeIcon(builder, largeIcon)
        builder.setContentIntent(mediaDescriptionAdapter.createCurrentContentIntent(player))
        if (groupKey != null) {
            builder.setGroup(groupKey)
        }
        builder.setOnlyAlertOnce(true)
        return builder
    }

    /**
     * Gets the names and order of the actions to be included in the notification at the current
     * player state.
     *
     *
     * The playback and custom actions are combined and placed in the following order if not
     * omitted:
     *
     * <pre>
     * +------------------------------------------------------------------------+
     * | prev | &lt;&lt; | play/pause | &gt;&gt; | next | custom actions | stop |
     * +------------------------------------------------------------------------+
    </pre> *
     *
     *
     * This method can be safely overridden. However, the names must be of the playback actions
     * [.ACTION_PAUSE], [.ACTION_PLAY], [.ACTION_FAST_FORWARD], [ ][.ACTION_REWIND], [.ACTION_NEXT] or [.ACTION_PREVIOUS], or a key contained in the
     * map returned by [CustomActionReceiver.createCustomActions]. Otherwise the
     * action name is ignored.
     */
    protected fun getActions(player: Player): List<String> {
        val enablePrevious = player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS)
        val enableRewind = player.isCommandAvailable(Player.COMMAND_SEEK_BACK)
        val enableFastForward = player.isCommandAvailable(Player.COMMAND_SEEK_FORWARD)
        val enableNext = player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT)
        val stringActions: MutableList<String> = ArrayList()
        if (usePreviousAction && enablePrevious) {
            stringActions.add(ACTION_PREVIOUS)
        }
        if (useRewindAction && enableRewind) {
            stringActions.add(ACTION_REWIND)
        }
        if (usePlayPauseActions) {
            if (shouldShowPauseButton(player)) {
                stringActions.add(ACTION_PAUSE)
            } else {
                stringActions.add(ACTION_PLAY)
            }
        }
        if (useFastForwardAction && enableFastForward) {
            stringActions.add(ACTION_FAST_FORWARD)
        }
        if (useNextAction && enableNext) {
            stringActions.add(ACTION_NEXT)
        }
        if (customActionReceiver != null) {
            stringActions.addAll(customActionReceiver.getCustomActions(player)!!)
        }
        if (useStopAction) {
            stringActions.add(ACTION_STOP)
        }
        return stringActions
    }

    /**
     * Gets an array with the indices of the buttons to be shown in compact mode.
     *
     *
     * This method can be overridden. The indices must refer to the list of actions passed as the
     * first parameter.
     *
     * @param actionNames The names of the actions included in the notification.
     * @param player The player for which a notification is being built.
     */
    protected fun getActionIndicesForCompactView(
        actionNames: List<String>,
        player: Player
    ): IntArray {
        val pauseActionIndex = actionNames.indexOf(ACTION_PAUSE)
        val playActionIndex = actionNames.indexOf(ACTION_PLAY)
        val leftSideActionIndex = if (usePreviousActionInCompactView) actionNames.indexOf(
            ACTION_PREVIOUS
        ) else if (useRewindActionInCompactView) actionNames.indexOf(ACTION_REWIND) else -1
        val rightSideActionIndex =
            if (useNextActionInCompactView) actionNames.indexOf(ACTION_NEXT) else if (useFastForwardActionInCompactView) actionNames.indexOf(
                ACTION_FAST_FORWARD
            ) else -1
        val actionIndices = IntArray(3)
        var actionCounter = 0
        if (leftSideActionIndex != -1) {
            actionIndices[actionCounter++] = leftSideActionIndex
        }
        val shouldShowPauseButton = shouldShowPauseButton(player)
        if (pauseActionIndex != -1 && shouldShowPauseButton) {
            actionIndices[actionCounter++] = pauseActionIndex
        } else if (playActionIndex != -1 && !shouldShowPauseButton) {
            actionIndices[actionCounter++] = playActionIndex
        }
        if (rightSideActionIndex != -1) {
            actionIndices[actionCounter++] = rightSideActionIndex
        }
        return Arrays.copyOf(actionIndices, actionCounter)
    }

    /** Returns whether the generated notification should be ongoing.  */
    protected fun getOngoing(player: Player): Boolean {
        val playbackState = player.playbackState
        return ((playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY)
            && player.playWhenReady)
    }

    private fun shouldShowPauseButton(player: Player): Boolean {
        return player.playbackState != Player.STATE_ENDED && player.playbackState != Player.STATE_IDLE && player.playWhenReady
    }

    private fun postStartOrUpdateNotification() {
        if (!mainHandler.hasMessages(MSG_START_OR_UPDATE_NOTIFICATION)) {
            mainHandler.sendEmptyMessage(MSG_START_OR_UPDATE_NOTIFICATION)
        }
    }

    private fun postUpdateNotificationBitmap(bitmap: Bitmap, notificationTag: Int) {
        mainHandler
            .obtainMessage(
                MSG_UPDATE_NOTIFICATION_BITMAP, notificationTag, C.INDEX_UNSET /* ignored */, bitmap
            )
            .sendToTarget()
    }

    private fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            MSG_START_OR_UPDATE_NOTIFICATION -> if (player != null) {
                startOrUpdateNotification(player!!,  /* bitmap= */null)
            }
            MSG_UPDATE_NOTIFICATION_BITMAP -> if (player != null && isNotificationStarted && currentNotificationTag == msg.arg1) {
                startOrUpdateNotification(player!!, msg.obj as Bitmap)
            }
            else -> return false
        }
        return true
    }

    private inner class PlayerListener : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_MEDIA_METADATA_CHANGED
                )
            ) {
                postStartOrUpdateNotification()
            }
        }
    }

    private inner class NotificationBroadcastReceiver : BroadcastReceiver() {
        private var t = 0L
        override fun onReceive(context: Context, intent: Intent) {
            val player = player
            LLog.i(TAG,"get receive from notify")
            val tt = System.currentTimeMillis()
            if (tt - t <1000) return
            t = tt
            if (player == null || !isNotificationStarted
                || intent.getIntExtra(EXTRA_INSTANCE_ID, instanceId) != instanceId
            ) {
                return
            }
            val action = intent.action
            LLog.i(TAG,"action:$action")
            if (ACTION_PLAY == action) {
                if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                } else if (player.playbackState == Player.STATE_ENDED) {
                    player.seekToDefaultPosition(player.currentMediaItemIndex)
                }
                player.play()
            } else if (ACTION_PAUSE == action) {
                player.pause()
            } else if (ACTION_PREVIOUS == action) {
                player.seekToPreviousMediaItem()
            } else if (ACTION_REWIND == action) {
                player.seekBack()
            } else if (ACTION_FAST_FORWARD == action) {
                player.seekForward()
            } else if (ACTION_NEXT == action) {
                player.seekToNextMediaItem()
            } else if (ACTION_STOP == action) {
                player.stop( /* reset= */true)
            } else if (ACTION_DISMISS == action) {
                stopNotification( /* dismissedByUser= */true)
            } else if (action != null && customActionReceiver != null && customActions.containsKey(
                    action
                )
            ) {
                customActionReceiver.onCustomAction(player, action, intent)
            }
        }
    }

    companion object {
        private const val TAG = "HnPlaybackNotificationManager"
        /** The action which starts playback.  */
        const val ACTION_PLAY = "com.google.android.exoplayer.play"

        /** The action which pauses playback.  */
        const val ACTION_PAUSE = "com.google.android.exoplayer.pause"

        /** The action which skips to the previous media item.  */
        const val ACTION_PREVIOUS = "com.google.android.exoplayer.prev"

        /** The action which skips to the next media item.  */
        const val ACTION_NEXT = "com.google.android.exoplayer.next"

        /** The action which fast forwards.  */
        const val ACTION_FAST_FORWARD = "com.google.android.exoplayer.ffwd"

        /** The action which rewinds.  */
        const val ACTION_REWIND = "com.google.android.exoplayer.rewind"

        /** The action which stops playback.  */
        const val ACTION_STOP = "com.google.android.exoplayer.stop"

        /** The extra key of the instance id of the player notification manager.  */
        const val EXTRA_INSTANCE_ID = "INSTANCE_ID"

        /**
         * The action which is executed when the notification is dismissed. It cancels the notification
         * and calls [NotificationListener.onNotificationCancelled].
         */
        private const val ACTION_DISMISS = "com.google.android.exoplayer.dismiss"

        // Internal messages.
        private const val MSG_START_OR_UPDATE_NOTIFICATION = 0
        private const val MSG_UPDATE_NOTIFICATION_BITMAP = 1
        private var instanceIdCounter = 0
        private fun createPlaybackActions(
            context: Context,
            instanceId: Int,
            playActionIconResourceId: Int,
            pauseActionIconResourceId: Int,
            stopActionIconResourceId: Int,
            rewindActionIconResourceId: Int,
            fastForwardActionIconResourceId: Int,
            previousActionIconResourceId: Int,
            nextActionIconResourceId: Int
        ): Map<String, NotificationCompat.Action> {
            val actions: MutableMap<String, NotificationCompat.Action> = HashMap()
            actions[ACTION_PLAY] = NotificationCompat.Action(
                playActionIconResourceId,
                context.getString(R.string.notify_1),
                createBroadcastIntent(ACTION_PLAY, context, instanceId)
            )
            actions[ACTION_PAUSE] = NotificationCompat.Action(
                pauseActionIconResourceId,
                context.getString(R.string.notify_1),
                createBroadcastIntent(ACTION_PAUSE, context, instanceId)
            )
            actions[ACTION_STOP] = NotificationCompat.Action(
                stopActionIconResourceId,
                context.getString(R.string.notify_1),
                createBroadcastIntent(ACTION_STOP, context, instanceId)
            )
            actions[ACTION_REWIND] = NotificationCompat.Action(
                rewindActionIconResourceId,
                context.getString(R.string.notify_1),
                createBroadcastIntent(ACTION_REWIND, context, instanceId)
            )
            actions[ACTION_FAST_FORWARD] = NotificationCompat.Action(
                fastForwardActionIconResourceId,
                context.getString(R.string.notify_1),
                createBroadcastIntent(ACTION_FAST_FORWARD, context, instanceId)
            )
            actions[ACTION_PREVIOUS] = NotificationCompat.Action(
                previousActionIconResourceId,
                context.getString(R.string.notify_1),
                createBroadcastIntent(ACTION_PREVIOUS, context, instanceId)
            )
            actions[ACTION_NEXT] = NotificationCompat.Action(
                nextActionIconResourceId,
                context.getString(R.string.notify_1),
                createBroadcastIntent(ACTION_NEXT, context, instanceId)
            )
            return actions
        }

        private fun createBroadcastIntent(
            action: String, context: Context, instanceId: Int
        ): PendingIntent {
            val intent = Intent(action).setPackage(context.packageName)
            intent.putExtra(EXTRA_INSTANCE_ID, instanceId)
            val pendingFlags: Int = if (Util.SDK_INT >= 23) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(context, instanceId, intent, pendingFlags)
        }

        private fun setLargeIcon(builder: NotificationCompat.Builder, largeIcon: Bitmap?) {
            builder.setLargeIcon(largeIcon)
        }
    }

    init {
        var context = context
        context = context.applicationContext
        this.context = context
        this.channelId = channelId
        this.notificationId = notificationId
        this.mediaDescriptionAdapter = mediaDescriptionAdapter
        this.notificationListener = notificationListener
        this.customActionReceiver = customActionReceiver
        this.smallIconResourceId = smallIconResourceId
        this.groupKey = groupKey
        instanceId = instanceIdCounter++
        // This fails the nullness checker because handleMessage() is 'called' while `this` is still
        // @UnderInitialization. No tasks are scheduled on mainHandler before the constructor completes,
        // so this is safe and we can suppress the warning.
        val mainHandler = Util.createHandler(
            Looper.getMainLooper()
        ) { msg: Message -> handleMessage(msg) }
        this.mainHandler = mainHandler
        notificationManager = NotificationManagerCompat.from(context)
        playerListener = PlayerListener()
        notificationBroadcastReceiver = NotificationBroadcastReceiver()
        intentFilter = IntentFilter()
        usePreviousAction = true
        useNextAction = true
        usePlayPauseActions = true
        useRewindAction = true
        useFastForwardAction = true
        colorized = true
        useChronometer = true
        color = Color.TRANSPARENT
        defaults = 0
        priority = NotificationCompat.PRIORITY_LOW
        badgeIconType = NotificationCompat.BADGE_ICON_SMALL
        visibility = NotificationCompat.VISIBILITY_PUBLIC

        // initialize actions
        playbackActions = createPlaybackActions(
            context,
            instanceId,
            playActionIconResourceId,
            pauseActionIconResourceId,
            stopActionIconResourceId,
            rewindActionIconResourceId,
            fastForwardActionIconResourceId,
            previousActionIconResourceId,
            nextActionIconResourceId
        )
        for (action in playbackActions.keys) {
            intentFilter.addAction(action)
        }
        customActions = customActionReceiver?.createCustomActions(context, instanceId)
            ?: emptyMap()
        for (action in customActions.keys) {
            intentFilter.addAction(action)
        }
        dismissPendingIntent = createBroadcastIntent(ACTION_DISMISS, context, instanceId)
        intentFilter.addAction(ACTION_DISMISS)
    }
}
