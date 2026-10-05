package org.cog.hymnchtv.ui.notebook

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.cog.hymnchtv.ui.notes.HymnNotesFragment
import org.cog.hymnchtv.ui.playlist.PlaylistDetailFragment

/** Intents of the notebook pages hosted by [NotebookPageActivity], and the fragment each one shows. */
object NotebookPages {
    const val EXTRA_PAGE = "notebook_page"
    const val PAGE_NOTES = "notes"
    const val PAGE_PLAYLIST = "playlist"
    const val ARG_HYMN_TYPE = "hymn_type"
    const val ARG_HYMN_NO = "hymn_no"
    const val ARG_PLAYLIST_ID = "playlist_id"

    /** The notes page of [key] (opened from the lyrics page's More menu). */
    @JvmStatic
    fun notes(context: Context, key: HymnKey): Intent = Intent(context, NotebookPageActivity::class.java)
        .putExtra(EXTRA_PAGE, PAGE_NOTES)
        .putExtra(ARG_HYMN_TYPE, key.hymnType)
        .putExtra(ARG_HYMN_NO, key.hymnNo)

    /** A playlist's page (opened from the playlists tab). */
    @JvmStatic
    fun playlist(context: Context, playlistId: String): Intent = Intent(context, NotebookPageActivity::class.java)
        .putExtra(EXTRA_PAGE, PAGE_PLAYLIST)
        .putExtra(ARG_PLAYLIST_ID, playlistId)

    /** The page for [extras], or null when they name no page or an invalid target (the activity then closes). */
    fun fragmentFor(extras: Bundle?): Fragment? = when (extras?.getString(EXTRA_PAGE)) {
        PAGE_NOTES -> HymnKey.ofOrNull(extras.getString(ARG_HYMN_TYPE), extras.getInt(ARG_HYMN_NO, -1))
            ?.let { HymnNotesFragment.newInstance(it) }
        PAGE_PLAYLIST -> extras.getString(ARG_PLAYLIST_ID)
            ?.takeIf { runCatching { NotebookValidation.uuid(it) }.isSuccess }
            ?.let { PlaylistDetailFragment.newInstance(it) }
        else -> null
    }
}
