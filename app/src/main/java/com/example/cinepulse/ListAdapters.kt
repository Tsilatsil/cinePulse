package com.example.cinepulse

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cinepulse.databinding.ItemCommentBinding
import com.example.cinepulse.databinding.ItemWatchlistBinding

// ---------------------------------------------------------------------------
// WATCHLIST rows: poster, title, "Mark watched" and "Remove" buttons
// ---------------------------------------------------------------------------
class WatchlistAdapter(
    private val onOpen: (WatchItem) -> Unit,
    private val onWatched: (WatchItem) -> Unit,
    private val onRemove: (WatchItem) -> Unit
) : RecyclerView.Adapter<WatchlistAdapter.VH>() {

    private val items = mutableListOf<WatchItem>()

    class VH(val b: ItemWatchlistBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<WatchItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemWatchlistBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val m = item.movie
        val b = holder.b

        b.tvTitle.text = m.title
        b.tvMeta.text = metaLine(b.root.context, m) + " • " + voteText(m)
        b.ivPoster.contentDescription = m.title
        loadImage(b.ivPoster, m.posterUrl)

        // Watched titles are dimmed and their button is locked (prevents farming points)
        b.root.alpha = if (item.watched) 0.65f else 1f
        b.btnWatched.isEnabled = !item.watched
        b.btnWatched.setText(if (item.watched) R.string.btn_watched else R.string.btn_mark_watched)

        // "Waiting to sync" label for changes made offline
        b.tvPending.visibility = if (item.pending != 0) View.VISIBLE else View.GONE

        b.root.setOnClickListener { onOpen(item) }
        b.btnWatched.setOnClickListener { onWatched(item) }
        b.btnRemove.setOnClickListener { onRemove(item) }
    }
}

// ---------------------------------------------------------------------------
// COMMENTS on the detail screen
// ---------------------------------------------------------------------------
class CommentAdapter : RecyclerView.Adapter<CommentAdapter.VH>() {

    private val items = mutableListOf<Comment>()

    class VH(val b: ItemCommentBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<Comment>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        val b = holder.b
        b.tvAvatar.text = c.userName.firstOrNull()?.uppercase() ?: "?"
        b.tvName.text = c.userName
        b.tvTime.text = DateUtils.getRelativeTimeSpanString(
            c.createdAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        )
        b.tvText.text = c.text
        b.tvPending.visibility = if (c.pending) View.VISIBLE else View.GONE
    }
}