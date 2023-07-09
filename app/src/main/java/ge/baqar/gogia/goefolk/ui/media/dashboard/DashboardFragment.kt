package ge.baqar.gogia.goefolk.ui.media.dashboard

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import ge.baqar.gogia.goefolk.databinding.FragmentDashboardBinding
import ge.baqar.gogia.goefolk.model.Artist
import ge.baqar.gogia.goefolk.model.ArtistType
import ge.baqar.gogia.goefolk.model.Song
import ge.baqar.gogia.goefolk.model.SongType
import ge.baqar.gogia.goefolk.model.events.CurrentPlayingSong
import ge.baqar.gogia.goefolk.ui.media.MenuActivity
import ge.baqar.gogia.goefolk.ui.media.songs.SongsAdapter
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import kotlin.time.ExperimentalTime

class DashboardFragment: Fragment()  {

    private val viewModel: DashboardViewModel by viewModel()
    private var binding: FragmentDashboardBinding? = null

    @OptIn(InternalCoroutinesApi::class, ExperimentalTime::class)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        binding?.daySongLayout?.setOnClickListener {
            (activity as MenuActivity).playMediaPlayback(0,
                mutableListOf(viewModel.state.daySong!!),
                Artist(viewModel.state.daySong?.artistId!!, viewModel.state.daySong?.artistName!!, ArtistType.ENSEMBLE, true)
            )
        }
        binding?.dayChantLayout?.setOnClickListener {
            (activity as MenuActivity).playMediaPlayback(0,
                mutableListOf(viewModel.state.dayChant!!),
                Artist(viewModel.state.dayChant?.artistId!!, viewModel.state.dayChant?.artistName!!, ArtistType.ENSEMBLE, true)
            )
        }
        initializeIntents(flowOf(DashboardDataRequested()))
        return binding?.root!!
    }

    private fun initializeIntents(inputs: Flow<DashboardAction>) {
        viewModel.intents(inputs)
            .onEach { output ->
                when (output) {
                    is DashboardState -> render(output)
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun render(state: DashboardState) {
        if (state.error != null) {
            Toast.makeText(context, state.error, Toast.LENGTH_LONG).show()
            Timber.i(state.error)
            return
        }
        if (state.isInProgress) {

            return
        }

        binding?.daySong = state.daySong?.detailedName()
        binding?.dayChant = state.dayChant?.detailedName()
        binding?.holidayTitle = state.holdayData?.title

        binding?.holidaySongsListView?.adapter = SongsAdapter(state.holdayData?.holidaySongs!!) { song, index ->
            play(index, song)
        }
    }

    @SuppressLint("NewApi")
    @OptIn(InternalCoroutinesApi::class, ExperimentalTime::class)
    private fun play(position: Int, song: Song) {
        (activity as MenuActivity).playMediaPlayback(position, viewModel.state.holdayData?.holidaySongs!!, Artist(song.artistId, song.artistName, ArtistType.ENSEMBLE, true))
    }
}