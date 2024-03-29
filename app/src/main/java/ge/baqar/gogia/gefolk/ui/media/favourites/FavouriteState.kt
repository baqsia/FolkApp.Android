package ge.baqar.gogia.gefolk.ui.media.favourites

import ge.baqar.gogia.gefolk.model.Song

open class FavouriteResultState

data class FavouriteState(
    val isInProgress: Boolean,
    val favSongs: MutableList<Song>,
    val error: String?
) : FavouriteResultState() {

    companion object {
        val DEFAULT = FavouriteState(
            isInProgress = true,
            error = null,
            favSongs = mutableListOf()
        )
    }
}
