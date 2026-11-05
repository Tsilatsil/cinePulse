package com.example.cinepulse

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cinepulse.databinding.ActivitySearchBinding

/** Search movies and series on TMDB (replaces the old search box from the first prototype). */
class SearchActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SearchActivity"
    }

    private lateinit var b: ActivitySearchBinding
    private val adapter = GridAdapter(showRank = false) { MovieDetailActivity.start(this, it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Open the keyboard straight away
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        b.rvResults.layoutManager = GridLayoutManager(this, 2)
        b.rvResults.adapter = adapter

        b.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        b.etSearch.requestFocus()
        b.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch()
                true
            } else {
                false
            }
        }
    }

    private fun runSearch() {
        val query = b.etSearch.text.toString().trim()
        if (query.length < 2) {
            b.tilSearch.error = getString(R.string.err_query_short)
            return
        }
        b.tilSearch.error = null
        hideKeyboard()

        Log.d(TAG, "Searching for '$query'")
        b.progress.visibility = View.VISIBLE
        b.tvEmpty.visibility = View.GONE

        TmdbApi.search(this, query) { list, _ ->
            if (isFinishing || isDestroyed) return@search
            b.progress.visibility = View.GONE
            adapter.submit(list.orEmpty())
            if (list.isNullOrEmpty()) {
                b.tvEmpty.setText(
                    if (NetworkUtil.isOnline(this)) R.string.search_empty else R.string.search_offline
                )
                b.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(b.etSearch.windowToken, 0)
    }
}