package org.cog.hymnchtv.reading.background

import androidx.exifinterface.media.ExifInterface
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class PhotoBackgroundImporterTest {
    private val files = File("/data/user/0/app/files")

    @Test
    fun photoAndTempShareADirectoryUnderFilesDir() {
        assertThat(PhotoBackgroundImporter.photoFileIn(files).path).isEqualTo("/data/user/0/app/files/backgrounds/photo.jpg")
        assertThat(PhotoBackgroundImporter.tempFileIn(files).parentFile).isEqualTo(PhotoBackgroundImporter.photoFileIn(files).parentFile)
        assertThat(PhotoBackgroundImporter.tempFileIn(files)).isNotEqualTo(PhotoBackgroundImporter.photoFileIn(files))
    }

    @Test
    fun orientationTagsMapToRotationAndMirror() {
        fun t(o: Int) = PhotoBackgroundImporter.orientationTransform(o)
        assertThat(t(ExifInterface.ORIENTATION_NORMAL).isIdentity).isTrue()
        assertThat(t(ExifInterface.ORIENTATION_UNDEFINED).isIdentity).isTrue()
        assertThat(t(ExifInterface.ORIENTATION_ROTATE_90)).isEqualTo(PhotoBackgroundImporter.Transform(90, false))
        assertThat(t(ExifInterface.ORIENTATION_ROTATE_180)).isEqualTo(PhotoBackgroundImporter.Transform(180, false))
        assertThat(t(ExifInterface.ORIENTATION_ROTATE_270)).isEqualTo(PhotoBackgroundImporter.Transform(270, false))
        assertThat(t(ExifInterface.ORIENTATION_FLIP_HORIZONTAL)).isEqualTo(PhotoBackgroundImporter.Transform(0, true))
        assertThat(t(ExifInterface.ORIENTATION_FLIP_VERTICAL)).isEqualTo(PhotoBackgroundImporter.Transform(180, true))
        assertThat(t(ExifInterface.ORIENTATION_TRANSPOSE)).isEqualTo(PhotoBackgroundImporter.Transform(90, true))
        assertThat(t(ExifInterface.ORIENTATION_TRANSVERSE)).isEqualTo(PhotoBackgroundImporter.Transform(270, true))
        assertThat(t(99).isIdentity).isTrue()
    }

    @Test
    fun sourceIsRecycledOnlyWhenTheTransformMadeACopy() {
        val same = Any()
        assertThat(PhotoBackgroundImporter.shouldRecycleSource(same, same)).isFalse()
        assertThat(PhotoBackgroundImporter.shouldRecycleSource(same, Any())).isTrue()
    }
}
