package org.cog.hymnchtv.notebook.data

import androidx.room.TypeConverter
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource

/** Explicit converters so unknown stored names degrade to OTHER/MANUAL instead of throwing. */
class NotebookConverters {
    @TypeConverter
    fun occasionToStorage(value: Occasion): String = value.name

    @TypeConverter
    fun occasionFromStorage(value: String): Occasion = Occasion.fromStorage(value) ?: Occasion.OTHER

    @TypeConverter
    fun sourceToStorage(value: SingSource): String = value.name

    @TypeConverter
    fun sourceFromStorage(value: String): SingSource = SingSource.fromStorage(value) ?: SingSource.MANUAL
}
