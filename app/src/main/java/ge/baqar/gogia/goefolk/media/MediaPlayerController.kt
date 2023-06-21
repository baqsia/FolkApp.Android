package ge.baqar.gogia.goefolk.media

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.viewModelScope
import ge.baqar.gogia.goefolk.R
import ge.baqar.gogia.goefolk.databinding.ActivityMenuBinding
import ge.baqar.gogia.goefolk.media.MediaPlaybackService.Companion.NEXT_MEDIA
import ge.baqar.gogia.goefolk.media.MediaPlaybackService.Companion.PAUSE_OR_MEDIA
import ge.baqar.gogia.goefolk.media.MediaPlaybackService.Companion.PLAY_MEDIA
import ge.baqar.gogia.goefolk.media.MediaPlaybackService.Companion.PREV_MEDIA
import ge.baqar.gogia.goefolk.media.MediaPlaybackService.Companion.STOP_MEDIA
import ge.baqar.gogia.goefolk.media.player.AudioPlayer
import ge.baqar.gogia.goefolk.model.Artist
import ge.baqar.gogia.goefolk.model.AutoPlayState
import ge.baqar.gogia.goefolk.model.Song
import ge.baqar.gogia.goefolk.model.events.ArtistChanged
import ge.baqar.gogia.goefolk.model.events.OpenArtistFragment
import ge.baqar.gogia.goefolk.model.events.SetTimerEvent
import ge.baqar.gogia.goefolk.model.events.SongsMarkedAsFavourite
import ge.baqar.gogia.goefolk.model.events.SongsUnmarkedAsFavourite
import ge.baqar.gogia.goefolk.model.events.UnSetTimerEvent
import ge.baqar.gogia.goefolk.storage.DownloadService
import ge.baqar.gogia.goefolk.storage.FolkAppPreferences
import ge.baqar.gogia.goefolk.ui.MenuActivity
import ge.baqar.gogia.goefolk.ui.songs.SongsViewModel
import ge.baqar.gogia.goefolk.utility.asDownloadable
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import kotlin.time.ExperimentalTime


@ExperimentalTime
@InternalCoroutinesApi
class MediaPlayerController(
    private val viewModel: SongsViewModel,
    private val folkAppPreferences: FolkAppPreferences,
    private val audioPlayer: AudioPlayer,
    private val activity: MenuActivity
) {
    var binding: ActivityMenuBinding? = null
    var artist: Artist? = null
    var playList: MutableList<Song>? = null

    private var position = 0
    private var autoPlayState = AutoPlayState.OFF
    private var timerSet = false

    private fun checkAutoPlayEnabled() {
        autoPlayState = folkAppPreferences.getAutoPlay()
        binding?.mediaPlayerView?.setAutoPlayState(autoPlayState)
    }

    private fun onPrepareListener() {
        val durationString = audioPlayer.getDurationString()
        val duration = audioPlayer.getDuration().toInt()
        binding?.mediaPlayerView?.setAutoPlayState(autoPlayState)
        binding?.mediaPlayerView?.setDuration(durationString, duration)
    }

    private fun initializeAudioPlayerChanges() {
        audioPlayer.completed {
            when (autoPlayState) {
                AutoPlayState.OFF -> {
                    stop()
                    binding?.mediaPlayerView?.setDuration(null, 0)
                    return@completed
                }

                AutoPlayState.REPEAT_ONE -> {
                    binding?.mediaPlayerView?.setDuration(null, 0)
                    val repeatedSong = playList!![position]
                    viewModel.viewModelScope.launch {
                        audioPlayer.play(
                            repeatedSong.path,
                            repeatedSong.data
                        ) { onPrepareListener() }
                        EventBus.getDefault().post(ArtistChanged(PLAY_MEDIA))
                    }
                    updateUI(repeatedSong)
                }

                AutoPlayState.REPEAT_ALBUM -> {
                    if (isThereAnythingNextToPlay()) {
                        next()
                        return@completed
                    }

                    stop()
                }
            }
        }
        audioPlayer.listenPlayer {
            binding?.mediaPlayerView?.isPlaying(it)
        }
        audioPlayer.updateTimeHandler { progress, time ->
            binding?.mediaPlayerView?.setProgress(time, progress.toInt())
        }
    }

    private fun initializeViewListeners() {
        binding?.mediaPlayerView?.setSeekListener = { progress ->
            viewModel.viewModelScope.launch {
                audioPlayer.playOn(progress)
            }
        }

        binding?.mediaPlayerView?.onNext = {
            if (isThereAnythingNextToPlay())
                next()
        }
        binding?.mediaPlayerView?.onPrev = {
            if (isThereAnythingPriorToPlay())
                previous()
        }
        binding?.mediaPlayerView?.onPlayPause = {
            if (audioPlayer.isPlaying()) {
                pause()
            } else {
                resume()
            }
        }
        binding?.mediaPlayerView?.onStop = {
            stop()
        }
        binding?.mediaPlayerView?.onAutoPlayChanged = {
            when (autoPlayState) {
                AutoPlayState.OFF -> {
                    autoPlayState = AutoPlayState.REPEAT_ALBUM
                }

                AutoPlayState.REPEAT_ALBUM -> {
                    autoPlayState = AutoPlayState.REPEAT_ONE
                }

                AutoPlayState.REPEAT_ONE -> {
                    autoPlayState = AutoPlayState.OFF
                }
            }
            folkAppPreferences.updateAutoPlay(autoPlayState)
            checkAutoPlayEnabled()
        }

        binding?.mediaPlayerView?.setOnCloseListener = {
            folkAppPreferences.setPlayerState(binding?.mediaPlayerView?.minimized!!)
        }
        binding?.mediaPlayerView?.openPlayListListener = {
            EventBus.getDefault().post(OpenArtistFragment(artist!!, getCurrentSong()))
            binding?.mediaPlayerView?.minimize()
        }

        timerSet = folkAppPreferences.getTimerSet()
        binding?.mediaPlayerView?.setTimer(timerSet)
        binding?.mediaPlayerView?.onTimerSetRequested = {
            timerSet = !timerSet
            folkAppPreferences.setTimerSet(timerSet)
            binding?.mediaPlayerView?.setTimer(timerSet)

            val array = arrayOf(activity.resources.getString(R.string.unset), "5", "10", "30", "60")
            var selectedPosition = 0
            val dialog = AlertDialog.Builder(activity)
                .setTitle(R.string.app_name_georgian)
                .setSingleChoiceItems(
                    array, 0
                ) { _, position -> selectedPosition = position }
                .setPositiveButton(
                    activity.resources.getString(R.string.set)
                ) { _, _ ->
                    val item = array[selectedPosition]
                    if (item == activity.resources.getString(R.string.unset)) {
                        EventBus.getDefault().post(UnSetTimerEvent)
                    } else {
                        val time = item.toLong()
                        EventBus.getDefault().post(SetTimerEvent(time))
                    }
                }
                .create()
            dialog.show()
        }

        binding?.mediaPlayerView?.setFavButtonClickListener = {
            val currentSong = getCurrentSong()!!
            viewModel.viewModelScope.launch {
                var isFav = viewModel.isSongFav(currentSong.id)
                val downloadableSongs = currentSong.asDownloadable()
                if (!isFav) {
                    val intent = Intent(activity, DownloadService::class.java).apply {
                        action = DownloadService.DOWNLOAD_SONGS
                        putExtra("ensemble", artist)
                        putParcelableArrayListExtra(
                            "songs",
                            arrayListOf(downloadableSongs)
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        activity.startForegroundService(intent)
                    } else {
                        activity.startService(intent)
                    }
                } else {
                    val intent = Intent(activity, DownloadService::class.java).apply {
                        action = DownloadService.STOP_DOWNLOADING
                        putExtra("ensemble", artist)
                        putParcelableArrayListExtra(
                            "songs",
                            arrayListOf(downloadableSongs)
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        activity.startForegroundService(intent)
                    } else {
                        activity.startService(intent)
                    }

                    updateFavouriteMarkFor(currentSong.also {
                        isFav = false
                    })
                }
            }
        }
    }

    fun songsMarkedAsFavourite(event: SongsMarkedAsFavourite) {
        val ids = event.songs
        playList?.filter { event.songs.map { song -> song.id }.contains(it.id) }?.forEach { song ->
            song.isFav = ids.map { it.id }.contains(song.id)
        }
        updateFavouriteMarkFor(getCurrentSong())
    }

    fun songsUnMarkedAsFavourite(event: SongsUnmarkedAsFavourite) {
        val ids = event.songs
        playList?.filter { event.songs.map { song -> song.id }.contains(it.id) }?.forEach { song ->
            song.isFav = !ids.map { it.id }.contains(song.id)
        }
        updateFavouriteMarkFor(getCurrentSong())
    }

    fun play() {
        playList?.let {
            val song = playList!![this.position]
            initializeAudioPlayerChanges()
            initializeViewListeners()
            viewModel.viewModelScope.launch {
                audioPlayer.play(song.path, song.data) { onPrepareListener() }
            }
            binding?.mediaPlayerView?.setTrackTitle(song.name, artist?.name)
            binding?.mediaPlayerView?.show()
            checkAutoPlayEnabled()
            updateFavouriteMarkFor(song)
        }
    }

    fun pause() {
        EventBus.getDefault().post(ArtistChanged(PAUSE_OR_MEDIA))
        audioPlayer.pause()
    }

    fun stop() {
        pause()
        EventBus.getDefault().post(ArtistChanged(STOP_MEDIA))
        binding?.mediaPlayerView?.setProgress(null, 0)
        binding?.mediaPlayerView?.minimize()
    }

    fun resume() {
        EventBus.getDefault().post(ArtistChanged(PAUSE_OR_MEDIA))
        audioPlayer.resume()
    }

    private fun isThereAnythingNextToPlay(): Boolean {
        return (position + 1) < playList?.size!!
    }

    private fun isThereAnythingPriorToPlay(): Boolean {
        return position > 0
    }

    fun next() {
        ++position
        val song = playList!![position]
        updateUI(song)
        viewModel.viewModelScope.launch {
            audioPlayer.play(song.path, song.data) { onPrepareListener() }
            EventBus.getDefault().post(ArtistChanged(NEXT_MEDIA))
        }
    }

    fun previous() {
        --position
        val song = playList!![position]
        updateUI(song)
        viewModel.viewModelScope.launch {
            audioPlayer.play(song.path, song.data) { onPrepareListener() }
            EventBus.getDefault().post(ArtistChanged(PREV_MEDIA))
        }
    }

    private fun updateFavouriteMarkFor(song: Song?) {
        song?.let {
            binding?.mediaPlayerView?.setIsFav(song.isFav)
        }
    }

    fun getCurrentSong(): Song? {
        return if (playList == null) null else playList!![position]
    }

    fun isPlaying(): Boolean {
        return audioPlayer.isPlaying()
    }

    fun updatePlayer() {
        playList?.let {
            val song = playList!![position]
            updateUI(song)
        }
    }

    private fun updateUI(song: Song) {
        checkAutoPlayEnabled()
        binding?.mediaPlayerView?.setTrackTitle(song.name, artist?.name)
        binding?.mediaPlayerView?.isPlaying(true)
        updateFavouriteMarkFor(song)
        onPrepareListener()
        binding?.mediaPlayerView?.show()
    }

    fun setInitialPosition(position: Int) {
        this.position = position
    }

    fun setCurrentSong(song: Song) {
        playList = mutableListOf(song)
        position = 0
        play()
        pause()
    }
}