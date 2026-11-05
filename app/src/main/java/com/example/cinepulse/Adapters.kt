package com.example.cinepulse

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cinepulse.databinding.ItemMovieGridBinding
import com.example.cinepulse.databinding.ItemMovieHeroBinding
import com.example.cinepulse.databinding.ItemMoviePosterBinding
import java.util.Locale

// ---------------------------------------------------------------------------
// Small helpers shared by all adapters
// ---------------------------------------------------------------------------

/** "★ 7.8" (or "★ –" when TMDB has no rating yet) */
fun voteText(m: Movie): String =
    if (m.voteAverage <= 0.0) "★ –" else String.format(Locale.US, "★ %.1f", m.voteAverage)

/** "Movie • 2024" or "Series • 2021" */
fun metaLine(ctx: Context, m: Movie): String {
    val type = ctx.getString(if (m.isSeries) R.string.type_series else R.string.type_movie)
    return listOf(type, m.year).filter { it.isNotEmpty() }.joinToString(" • ")
}

/** Loads an image from the internet with Glide (grey placeholder while loading). */
fun loadImage(view: ImageView, url: String?) {
    Glide.with(view)
        .load(url)
        .placeholder(R.color.cp_surface_alt)
        .error(R.color.cp_surface_alt)
        .centerCrop()
        .into(view)
}

// ---------------------------------------------------------------------------
// HERO: big backdrop cards (top of the Home screen)
// ---------------------------------------------------------------------------
class HeroAdapter(private val onClick: (Movie) -> Unit) :
    RecyclerView.Adapter<HeroAdapter.VH>() {

    private val items = mutableListOf<Movie>()

    class VH(val b: ItemMovieHeroBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<Movie>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMovieHeroBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.b.tvTitle.text = m.title
        holder.b.tvMeta.text = metaLine(holder.b.root.context, m) + " • " + voteText(m)
        holder.b.ivBackdrop.contentDescription = m.title
        loadImage(holder.b.ivBackdrop, m.backdropUrl)
        holder.b.root.setOnClickListener { onClick(m) }
    }
}

// ---------------------------------------------------------------------------
// POSTER: small poster cards used in the horizontal rows on Home
// ---------------------------------------------------------------------------
class PosterAdapter(private val onClick: (Movie) -> Unit) :
    RecyclerView.Adapter<PosterAdapter.VH>() {

    private val items = mutableListOf<Movie>()

    /** Optional caption under the title, e.g. "by Thabo" (key = movie.key) */
    var subtitles: Map<String, String> = emptyMap()

    class VH(val b: ItemMoviePosterBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<Movie>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMoviePosterBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.b.tvTitle.text = m.title
        holder.b.tvVote.text = voteText(m)
        holder.b.tvSub.text = subtitles[m.key] ?: m.year
        holder.b.ivPoster.contentDescription = m.title
        loadImage(holder.b.ivPoster, m.posterUrl)
        holder.b.root.setOnClickListener { onClick(m) }
    }
}

// ---------------------------------------------------------------------------
// GRID: two-column list used by Trending (with rank badge) and Search results
// ---------------------------------------------------------------------------
class GridAdapter(
    private val showRank: Boolean,
    private val onClick: (Movie) -> Unit
) : RecyclerView.Adapter<GridAdapter.VH>() {

    private val items = mutableListOf<Movie>()

    class VH(val b: ItemMovieGridBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<Movie>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMovieGridBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        holder.b.tvTitle.text = m.title
        holder.b.tvMeta.text = metaLine(holder.b.root.context, m)
        holder.b.tvVote.text = voteText(m)
        holder.b.tvRank.visibility = if (showRank) View.VISIBLE else View.GONE
        holder.b.tvRank.text = "#${position + 1}"
        holder.b.ivPoster.contentDescription = m.title
        loadImage(holder.b.ivPoster, m.posterUrl)
        holder.b.root.setOnClickListener { onClick(m) }
    }
}