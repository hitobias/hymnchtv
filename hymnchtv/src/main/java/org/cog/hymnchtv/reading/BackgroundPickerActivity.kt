package org.cog.hymnchtv.reading

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
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
import org.cog.hymnchtv.reading.background.ReadingPalette
import org.cog.hymnchtv.utils.WallPaperUtil

/** Grid of the 20 backgrounds plus "your photo" for one slot (plan A2); writes the slot's pref and finishes. */
class BackgroundPickerActivity : BaseActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var slot: BackgroundSlot
    private val choices: List<BackgroundChoice> =
        BackgroundPreset.entries.map { BackgroundChoice.Preset(it) } + BackgroundChoice.Photo

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && BackgroundPrefs.photoFile(prefs) != null) {
            choose(BackgroundChoice.Photo)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.background_picker)
        prefs = getSharedPreferences(MainActivity.PREF_SETTINGS, MODE_PRIVATE)
        slot = BackgroundSlot.fromName(intent.getStringExtra(EXTRA_SLOT))
        setTitle(if (slot == BackgroundSlot.MAIN) R.string.pref_main_background else R.string.pref_lyrics_background)

        val grid = findViewById<GridView>(R.id.backgroundGrid)
        grid.adapter = Adapter(BackgroundPrefs.resolve(prefs, slot))
        grid.setOnItemClickListener { _, _, position, _ ->
            val choice = choices[position]
            if (choice is BackgroundChoice.Photo) {
                // WallPaperUtil crops a new photo or re-uses the stored one; RESULT_OK means a photo file exists
                pickPhoto.launch(Intent(this, WallPaperUtil::class.java))
            } else {
                choose(choice)
            }
        }
    }

    private fun choose(choice: BackgroundChoice) {
        prefs.edit().putString(slot.prefKey, BackgroundPolicy.prefValue(choice)).apply()
        setResult(RESULT_OK)
        finish()
    }

    private inner class Adapter(private val current: BackgroundChoice) : BaseAdapter() {
        private val sampleFont: Typeface =
            (if (ReadingPrefs.lyricsFont(prefs) == LyricsFont.KAI) LyricsTypefaces.get(this@BackgroundPickerActivity, false) else null)
                ?: Typeface.DEFAULT

        override fun getCount(): Int = choices.size
        override fun getItem(position: Int): Any = choices[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.background_picker_item, parent, false)
            val choice = choices[position]
            val palette = bind(view.findViewById(R.id.bgImage), choice)
            view.findViewById<TextView>(R.id.bgSample).apply {
                setTextColor(palette.textColor)
                typeface = sampleFont
                // Photo cell: the sample sits on the same 85 % panel the lyrics use, so the preview tells the truth
                background = BackgroundDrawables.backdrop(this@BackgroundPickerActivity, palette)
            }
            view.findViewById<TextView>(R.id.bgName).setText(BackgroundDrawables.nameRes(choice))
            view.findViewById<TextView>(R.id.bgCheck).apply {
                visibility = if (choice == current) View.VISIBLE else View.GONE
                setTextColor(palette.accentColor)
            }
            return view
        }

        private fun bind(image: ImageView, choice: BackgroundChoice): ReadingPalette {
            if (choice is BackgroundChoice.Preset) {
                image.setImageDrawable(null)
                image.background = BackgroundDrawables.create(this@BackgroundPickerActivity, choice.preset)
                return BackgroundPolicy.palette(choice)
            }
            image.background = null
            // No photo yet: the grey cell background shows through
            image.setImageBitmap(BackgroundPrefs.photoFile(prefs)?.let { BackgroundApplier.decodePhoto(it, THUMB_PX, THUMB_PX) })
            return BackgroundPolicy.PHOTO_PALETTE
        }
    }

    companion object {
        private const val EXTRA_SLOT = "slot"
        private const val THUMB_PX = 360

        @JvmStatic
        fun intent(context: Context, slot: BackgroundSlot): Intent =
            Intent(context, BackgroundPickerActivity::class.java).putExtra(EXTRA_SLOT, slot.name)
    }
}
