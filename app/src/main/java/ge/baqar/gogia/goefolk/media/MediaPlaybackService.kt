package ge.baqar.gogia.goefolk.media

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.provider.MediaStore.Audio.Media
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ge.baqar.gogia.goefolk.R
import ge.baqar.gogia.goefolk.http.service_implementations.SongServiceImpl
import ge.baqar.gogia.goefolk.media.player.AudioPlayer
import ge.baqar.gogia.goefolk.model.events.ArtistChanged
import ge.baqar.gogia.goefolk.model.events.CurrentPlayingSong
import ge.baqar.gogia.goefolk.model.events.GetCurrentSong
import ge.baqar.gogia.goefolk.model.events.RequestMediaControllerInstance
import ge.baqar.gogia.goefolk.model.events.ServiceCreatedEvent
import ge.baqar.gogia.goefolk.model.events.SetTimerEvent
import ge.baqar.gogia.goefolk.model.events.SongsMarkedAsFavourite
import ge.baqar.gogia.goefolk.model.events.SongsUnmarkedAsFavourite
import ge.baqar.gogia.goefolk.model.events.UnSetTimerEvent
import ge.baqar.gogia.goefolk.storage.FolkAppPreferences
import ge.baqar.gogia.goefolk.ui.MenuActivity
import ge.baqar.gogia.goefolk.ui.songs.SongsViewModel
import kotlinx.coroutines.InternalCoroutinesApi
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.inject
import org.koin.core.component.inject
import kotlin.time.ExperimentalTime


@ExperimentalTime
@InternalCoroutinesApi
class MediaPlaybackService : Service() {

    private var notificationManager: NotificationManager? = null
    private val notificationId: Int = 1024
    private val binder: MediaPlaybackServiceBinder by lazy { MediaPlaybackServiceBinder(this) }
    val mediaPlayerController: MediaPlayerController by lazy {
        MediaPlayerController(
            inject<SongsViewModel>().value,
            inject<SongServiceImpl>().value,
            inject<FolkAppPreferences>().value,
            inject<AudioPlayer>().value,
            inject<MenuActivity>().value,
        )
    }
    private var timer: CountDownTimer? = null

    override fun onCreate() {
        super.onCreate()
        EventBus.getDefault().register(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent?): IBinder {
        handleMediaAction(intent?.action)
        return binder
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleMediaAction(intent?.action)
        return START_STICKY
    }

    @Subscribe
    fun songChanged(event: ArtistChanged) {
        handleMediaAction(event.action, false)
    }

    @Subscribe
    fun timerSet(event: SetTimerEvent) {
        val time = event.time * 60 * 1000
        timer = object : CountDownTimer(time, 1000L) {
            override fun onTick(p0: Long) {

            }

            override fun onFinish() {
                handleMediaAction(STOP_MEDIA, true)
            }
        }
        timer?.start()
    }

    @Subscribe
    fun timerUnSet(event: UnSetTimerEvent) {
        timer?.cancel()
        timer = null
    }

    fun handleMediaAction(action: String?, useMediaController: Boolean = true) {
        MediaPlaybackServiceManager.isRunning = true
        when (action) {
            PLAY_MEDIA -> {
                mediaPlayerController.play()
                showNotification(true)
            }

            PAUSE_OR_MEDIA -> {
                if (useMediaController) {
                    if (mediaPlayerController.isPlaying()) {
                        mediaPlayerController.pause()
                        MediaPlaybackServiceManager.isRunning = false
                    } else
                        mediaPlayerController.resume()
                }

                if (!mediaPlayerController.isPlaying())
                    MediaPlaybackServiceManager.isRunning = false
                showNotification()
            }

            STOP_MEDIA -> {
                MediaPlaybackServiceManager.isRunning = false
                if (useMediaController)
                    mediaPlayerController.stop()
                stopForeground(true)
            }

            PREV_MEDIA -> {
                if (useMediaController)
                    mediaPlayerController.previous()
                showNotification(true)
                EventBus.getDefault()
                    .post(CurrentPlayingSong(mediaPlayerController.getCurrentSong()))
            }

            NEXT_MEDIA -> {
                if (useMediaController)
                    mediaPlayerController.next()
                showNotification(true)
                EventBus.getDefault()
                    .post(CurrentPlayingSong(mediaPlayerController.getCurrentSong()))
            }

            null
            -> {
                if (mediaPlayerController.isPlaying()) {
                    showNotification()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun songsMarkedAsFavourite(event: SongsMarkedAsFavourite) {
        mediaPlayerController.songsMarkedAsFavourite(event)
    }

    @Subscribe
    fun songsUnMarkedAsFavourite(event: SongsUnmarkedAsFavourite) {
        mediaPlayerController.songsUnMarkedAsFavourite(event)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun getCurrentSong(event: GetCurrentSong) {
        EventBus.getDefault().post(CurrentPlayingSong(mediaPlayerController.getCurrentSong()))
    }

//    @Subscribe(threadMode = ThreadMode.BACKGROUND, sticky = true)
//    fun onMessageEvent(event: RequestMediaControllerInstance?) {
//        mediaPlayerController = event?.mediaPlayerController
//    }

    @SuppressLint("RemoteViewLayout", "UnspecifiedImmutableFlag")
    private fun showNotification(showResumeIcon: Boolean = false) {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MenuActivity::class.java),
            flag
        )
        val currentSong = mediaPlayerController.getCurrentSong()
        currentSong?.let {
            val notificationLayout = RemoteViews(packageName, R.layout.view_notification_small)
            val notificationLayoutExpanded =
                RemoteViews(packageName, R.layout.view_notification_large)

            notificationLayout.setTextViewText(R.id.notification_title, currentSong.name)
            notificationLayoutExpanded.setTextViewText(
                R.id.notification_title,
                currentSong.name
            )

            notificationLayout.setOnClickPendingIntent(
                R.id.notification_view_small,
                contentIntent
            )
            notificationLayoutExpanded.setOnClickPendingIntent(
                R.id.notification_view_small,
                contentIntent
            )

            initRemoteViewClicks(notificationLayoutExpanded, contentIntent, showResumeIcon, flag)

            val notificationBuilder: NotificationCompat.Builder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    getString(R.string.app_name_notification_channel),
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_NONE
                )
                channel.enableVibration(false)
                notificationBuilder = NotificationCompat.Builder(
                    this,
                    getString(R.string.app_name_notification_channel)
                )
                notificationManager?.createNotificationChannel(channel)
            } else {
                notificationBuilder = NotificationCompat.Builder(this)
            }
            val notification: NotificationCompat.Builder = notificationBuilder
                .setSmallIcon(R.drawable.ic_launcher_notification)
                .setCustomContentView(notificationLayout)
                .setCustomBigContentView(notificationLayoutExpanded)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setColorized(true)
                .setOngoing(true)
                .setColor(ContextCompat.getColor(this, R.color.colorAccentLighter))

            startForeground(notificationId, notification.build())
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun initRemoteViewClicks(
        notificationLayoutExpanded: RemoteViews,
        contentIntent: PendingIntent,
        showResumeIcon: Boolean = false,
        flag: Int
    ) {
        notificationLayoutExpanded.setOnClickPendingIntent(
            R.id.notification_view_large,
            contentIntent
        )
        notificationLayoutExpanded.setOnClickPendingIntent(
            R.id.playStopButton, PendingIntent.getService(
                this, 0,
                Intent(this, MediaPlaybackService::class.java).apply {
                    action = STOP_MEDIA
                },
                flag
            )
        )

        if (mediaPlayerController.isPlaying()!! || showResumeIcon) {
            notificationLayoutExpanded.setImageViewResource(
                R.id.playPauseButton,
                R.drawable.ic_baseline_pause_circle_outline_24
            )
        } else {
            notificationLayoutExpanded.setImageViewResource(
                R.id.playPauseButton,
                R.drawable.ic_baseline_play_circle_outline_24
            )
        }

        notificationLayoutExpanded.setOnClickPendingIntent(
            R.id.playPauseButton,
            PendingIntent.getService(
                this, 0,
                Intent(this, MediaPlaybackService::class.java).apply {
                    action = PAUSE_OR_MEDIA
                },
                flag
            )
        )
        notificationLayoutExpanded.setOnClickPendingIntent(
            R.id.playPrevButton, PendingIntent.getService(
                this, 0,
                Intent(this, MediaPlaybackService::class.java).apply {
                    action = PREV_MEDIA
                },
                flag
            )
        )
        notificationLayoutExpanded.setOnClickPendingIntent(
            R.id.playNextButton, PendingIntent.getService(
                this, 0,
                Intent(this, MediaPlaybackService::class.java).apply {
                    action = NEXT_MEDIA
                },
                flag
            )
        )
    }


    public class MediaPlaybackServiceBinder(val service: MediaPlaybackService) : Binder() {
    }

    companion object {
        const val PLAY_MEDIA = "PLAY_MEDIA"
        const val PAUSE_OR_MEDIA = "PAUSE_OR_MEDIA"
        const val STOP_MEDIA = "STOP_MEDIA"
        const val PREV_MEDIA = "PREV_MEDIA"
        const val NEXT_MEDIA = "NEXT_MEDIA"
        const val TIMER_SET = "TIMER_SET"
    }
}