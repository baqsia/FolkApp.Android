package ge.baqar.gogia.gefolk.ui.media.songs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ge.baqar.gogia.gefolk.R
import ge.baqar.gogia.gefolk.model.Song


class SongsAdapter(
    val dataSource: MutableList<Song>,
    val clicked: SongsAdapter.(Song, Int) -> Unit,
    val addToPlayListRequested: (Song) -> Unit
) : RecyclerView.Adapter<SongsAdapter.SongViewHolder>() {

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: AppCompatTextView by lazy {
            itemView.findViewById(R.id.songTitle)
        }
        private val availableOfflineIndicator: View by lazy {
            itemView.findViewById(R.id.availableOfflineIndicator)
        }
        private val songExpandMoreImageView: AppCompatImageView by lazy {
            itemView.findViewById(R.id.songExpandMoreImageView)
        }

        fun bind(song: Song, position: Int) {
            name.text = song.name
            if (song.isFav) {
                availableOfflineIndicator.visibility = View.VISIBLE
            } else {
                availableOfflineIndicator.visibility = View.GONE
            }
            if (song.isPlaying) {
                itemView.setBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.colorActiveSong
                    )
                )
            } else {
                itemView.setBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.white
                    )
                )
            }
            itemView.setOnClickListener {
                clicked.invoke(this@SongsAdapter, song, position)
                song.isPlaying = true
                notifyItemChanged(position)
            }
            songExpandMoreImageView.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    menuInflater.inflate(R.menu.popup_menu, menu)
                    setOnMenuItemClickListener { item ->
                        addToPlayListRequested.invoke(song)
                        true
                    }
                }.show()
            }
        }
    }

    fun applyNotPlayingState() {
        val oldPlayingOne = dataSource.firstOrNull { it.isPlaying }
        oldPlayingOne?.let {
            val index = dataSource.indexOf(it)
            it.isPlaying = false
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        )
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(dataSource[position], position)
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }
}