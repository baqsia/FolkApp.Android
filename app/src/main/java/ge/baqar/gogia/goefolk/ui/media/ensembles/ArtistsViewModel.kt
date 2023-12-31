package ge.baqar.gogia.goefolk.ui.media.ensembles

import ge.baqar.gogia.goefolk.arch.ReactiveViewModel
import ge.baqar.gogia.goefolk.http.service_implementations.ArtistsServiceImpl
import ge.baqar.gogia.goefolk.model.Artist
import ge.baqar.gogia.goefolk.model.FailedResult
import ge.baqar.gogia.goefolk.model.SucceedResult
import ge.baqar.gogia.goefolk.storage.db.FolkApiDao
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@InternalCoroutinesApi
class ArtistsViewModel(
    private val artistsService: ArtistsServiceImpl,
    private val folkApiDao: FolkApiDao
) : ReactiveViewModel<ArtistsAction, ArtistsResult, ArtistsState>(ArtistsState.DEFAULT) {

    fun ensembles() = update {
        emit {
            state.copy(isInProgress = true)
        }
        artistsService.ensembles().collect { value ->
            if (value is SucceedResult) {
                emit {
                    value.value.sortBy { it.name }
                    state.copy(isInProgress = false, artists = value.value)
                }
            }
            if (value is FailedResult) {
                val cachedEnsembles = folkApiDao.ensembles()
                if (cachedEnsembles.isNotEmpty()) {
                    val mapped = cachedEnsembles.map {
                        Artist(
                            it.referenceId,
                            it.name,
                            it.artistType
                        )
                    }.toMutableList()
                    emit {
                        state.copy(isInProgress = false, artists = mapped)
                    }
                }
                emit { state.copy(isInProgress = false, error = value.value.message) }
            }
        }
    }

    fun oldRecordings() = update {
        emit {
            state.copy(isInProgress = true)
        }
        artistsService.oldRecordings().collect { result ->
            if (result is SucceedResult) {
                emit {
                    state.copy(isInProgress = false, artists = result.value)
                }
            }
            if (result is FailedResult) {
                emit { state.copy(isInProgress = false, error = result.value.message) }
            }
        }
    }

    override fun ArtistsAction.process(): Flow<() -> ArtistsResult> {
        return when (this) {
            is ArtistsRequested -> {
                ensembles()
            }
            is OldRecordingsRequested -> {
                oldRecordings()
            }
            else -> update {

            }
        }
    }
}