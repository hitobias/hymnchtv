package org.cog.hymnchtv.reading

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.cog.hymnchtv.BaseActivity
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.background.BackgroundApplier
import org.cog.hymnchtv.reading.background.BackgroundChoice
import org.cog.hymnchtv.reading.background.BackgroundDrawables
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundPreset
import org.cog.hymnchtv.reading.background.BackgroundPrefs
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.PhotoBackgroundImporter
import org.cog.hymnchtv.reading.background.nextPhotoRevision
import org.cog.hymnchtv.reading.background.ReadingPalette
import org.cog.hymnchtv.reading.background.UiTokens

/** Grid of the 28 backgrounds plus "your photo" for one slot (plan A2); writes the slot's pref and finishes. */
class BackgroundPickerActivity : BaseActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var slot: BackgroundSlot
    private val choices: List<BackgroundChoice> =
        BackgroundPreset.entries.map { BackgroundChoice.Preset(it) } + BackgroundChoice.Photo

    private val thumbLoader: ExecutorService = Executors.newSingleThreadExecutor { r -> Thread(r, "bg-thumb").apply { isDaemon = true } }

    /** System photo picker: no storage permission, falls back to the document picker on older Android. */
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) importPhoto(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.background_picker)
        prefs = getSharedPreferences(MainActivity.PREF_SETTINGS, MODE_PRIVATE)
        slot = BackgroundSlot.fromName(intent.getStringExtra(EXTRA_SLOT))
        setTitle(if (slot == BackgroundSlot.MAIN) R.string.pref_main_background else R.string.pref_lyrics_background)

        val grid = findViewById<GridView>(R.id.backgroundGrid)
        val adapter = Adapter(BackgroundPrefs.resolve(prefs, slot))
        grid.adapter = adapter
        adapter.loadPhotoThumb()
        grid.setOnItemClickListener { _, _, position, _ ->
            val choice = choices[position]
            if (choice is BackgroundChoice.Photo) {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                choose(choice)
            }
        }
    }

    private fun importPhoto(uri: Uri) {
        // The callback may arrive after this Activity was recreated: the slot pref is written regardless, the UI only if alive
        val appPrefs = applicationContext.getSharedPreferences(MainActivity.PREF_SETTINGS, MODE_PRIVATE)
        val slotKey = slot.prefKey
        val revisionKey = slot.revisionKey
        PhotoBackgroundImporter.importAsync(this, uri) { ok ->
            if (ok) {
                val revision = nextPhotoRevision(runCatching { appPrefs.getLong(revisionKey, 0L) }.getOrDefault(0L), System.currentTimeMillis())
                appPrefs.edit()
                    .putString(slotKey, BackgroundPolicy.prefValue(BackgroundChoice.Photo))
                    .putLong(revisionKey, revision) // a changed value even when the slot was already "photo"
                    .apply()
            }
            if (isFinishing || isDestroyed) return@importAsync
            if (ok) {
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, R.string.bg_photo_import_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        thumbLoader.shutdownNow()
        super.onDestroy()
    }

    private fun choose(choice: BackgroundChoice) {
        prefs.edit().putString(slot.prefKey, BackgroundPolicy.prefValue(choice)).apply()
        setResult(RESULT_OK)
        finish()
    }

    private inner class Adapter(private val current: BackgroundChoice) : BaseAdapter() {
        // Tokens depend only on the (fixed) choice at a position; a cell is re-bound on every scroll
        private val tokenCache = HashMap<Int, UiTokens>()

        // System font first; the lyrics face replaces it (and the grid refreshes) once it has loaded off the main thread
        private var sampleFont: Typeface = Typeface.DEFAULT

        init {
            if (ReadingPrefs.lyricsFont(prefs) == LyricsFont.KAI) {
                LyricsTypefaces.request(this@BackgroundPickerActivity, false) { face ->
                    if (!isDestroyed) {
                        sampleFont = face
                        notifyDataSetChanged()
                    }
                }
            }
        }

        override fun getCount(): Int = choices.size
        override fun getItem(position: Int): Any = choices[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.background_picker_item, parent, false)
            val choice = choices[position]
            val palette = bind(position, view.findViewById(R.id.bgImage), choice)
            val tokens = tokenCache.getOrPut(position) { UiTokens.from(BackgroundPolicy.tokenInput(choice)) }
            bindMiniCard(view, tokens, choice is BackgroundChoice.Photo)
            view.findViewById<TextView>(R.id.bgSample).apply {
                setTextColor(tokens.onSurface)
                typeface = sampleFont
            }
            view.findViewById<TextView>(R.id.bgName).setText(BackgroundDrawables.nameRes(choice))
            view.findViewById<TextView>(R.id.bgCheck).apply {
                visibility = if (choice == current) View.VISIBLE else View.GONE
                setTextColor(palette.accentColor)
            }
            return view
        }

        /** The thumbnail's card and three buttons (two tone, one selected) drawn from the cell's own tokens. */
        private fun bindMiniCard(cell: View, t: UiTokens, isPhoto: Boolean) {
            val density = resources.displayMetrics.density
            fun shape(fill: Int, radiusDp: Float, stroke: Boolean = false) = GradientDrawable().apply {
                setColor(fill)
                cornerRadius = radiusDp * density
                if (stroke) setStroke(density.toInt().coerceAtLeast(1), t.outline)
            }
            cell.findViewById<View>(R.id.bgMiniCard).background = shape(t.surface, MINI_CARD_RADIUS_DP, stroke = isPhoto)
            cell.findViewById<View>(R.id.bgMiniButton1).background = shape(t.surfaceTone, MINI_BUTTON_RADIUS_DP)
            cell.findViewById<View>(R.id.bgMiniButton2).background = shape(t.surfaceTone, MINI_BUTTON_RADIUS_DP)
            cell.findViewById<View>(R.id.bgMiniButton3).background = shape(t.accent, MINI_BUTTON_RADIUS_DP)
        }

        /** Decoded once, off the main thread; the grid refreshes when it arrives. */
        private var photoThumb: Bitmap? = null
        private val presetDrawables = HashMap<Int, Drawable?>()

        fun loadPhotoThumb() {
            val file = BackgroundPrefs.photoFile(prefs) ?: return
            thumbLoader.execute {
                val bitmap = BackgroundApplier.decodePhoto(file, THUMB_PX, THUMB_PX)
                runOnUiThread {
                    if (!isDestroyed) {
                        photoThumb = bitmap
                        notifyDataSetChanged()
                    }
                }
            }
        }

        private fun bind(position: Int, image: ImageView, choice: BackgroundChoice): ReadingPalette {
            if (choice is BackgroundChoice.Preset) {
                image.setImageDrawable(null)
                image.background = presetDrawables.getOrPut(position) { BackgroundDrawables.create(this@BackgroundPickerActivity, choice.preset) }
                return BackgroundPolicy.palette(choice)
            }
            image.background = null
            // No photo yet (or still decoding): the grey cell background shows through
            image.setImageBitmap(photoThumb)
            return BackgroundPolicy.PHOTO_PALETTE
        }
    }

    companion object {
        private const val EXTRA_SLOT = "slot"
        private const val THUMB_PX = 360
        private const val MINI_CARD_RADIUS_DP = 8f
        private const val MINI_BUTTON_RADIUS_DP = 4f

        @JvmStatic
        fun intent(context: Context, slot: BackgroundSlot): Intent =
            Intent(context, BackgroundPickerActivity::class.java).putExtra(EXTRA_SLOT, slot.name)
    }
}
