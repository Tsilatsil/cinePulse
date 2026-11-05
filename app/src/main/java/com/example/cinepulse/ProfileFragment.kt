package com.example.cinepulse

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.cinepulse.databinding.FragmentProfileBinding

/** Profile tab: name, level, points progress, stats and a button to open Settings. */
class ProfileFragment : Fragment() {

    private var _b: FragmentProfileBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentProfileBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.btnSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (_b != null) refresh()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _b != null) refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    private fun refresh() {
        val app = context?.applicationContext ?: return
        val points = RewardManager.points(app)
        val name = Session.userName()

        b.tvAvatar.text = name.firstOrNull()?.uppercase() ?: "?"
        b.tvName.text = name
        b.tvEmail.text = Session.email()
        b.tvLevel.setText(RewardManager.levelNameRes(points))
        b.tvPointsTotal.text = getString(R.string.profile_points_total, points)
        b.tvStatPoints.text = points.toString()

        // Progress bar towards the next level
        val next = RewardManager.nextLevelAt(points)
        if (next == null) {
            b.progressLevel.max = 1
            b.progressLevel.progress = 1
            b.tvNextLevel.setText(R.string.profile_max_level)
        } else {
            val start = RewardManager.levelStart(points)
            b.progressLevel.max = next - start
            b.progressLevel.progress = points - start
            b.tvNextLevel.text = getString(
                R.string.profile_next_level, next - points, getString(RewardManager.levelNameRes(next))
            )
        }

        // Stats come from the local SQLite database
        val uid = Session.uid()
        Bg.run({
            val db = LocalDb.get(app)
            Pair(db.watchedCount(uid), db.getActiveWatchlist(uid).size)
        }) { (watched, listed) ->
            if (_b != null) {
                b.tvStatWatched.text = watched.toString()
                b.tvStatWatchlist.text = listed.toString()
            }
        }
    }
}