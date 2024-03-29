package ge.baqar.gogia.gefolk.ui.media.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import ge.baqar.gogia.gefolk.R
import ge.baqar.gogia.gefolk.model.SearchedItem

class SearchedDataAdapter<Item : SearchedItem>(
    private val dataSource: MutableList<Item>,
    private val clicked: SearchedDataAdapter<Item>.(Int, Item) -> Unit
) :
    RecyclerView.Adapter<SearchedDataAdapter<Item>.SearchedItemViewHolder>() {
    inner class SearchedItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: AppCompatTextView by lazy {
            itemView.findViewById(R.id.title)
        }

        fun bind(position: Int, item: Item) {
            name.text = item.detailedName()
            itemView.setOnClickListener {
                clicked(position, item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchedItemViewHolder {
        return SearchedItemViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_result_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: SearchedItemViewHolder, position: Int) {
        holder.bind(position, dataSource[position])
    }

    override fun getItemCount(): Int {
        return dataSource.size
    }
}