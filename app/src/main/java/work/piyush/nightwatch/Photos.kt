package work.piyush.nightwatch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File

/** Selfies live in the app's private storage (not the gallery). File name = capture time. */
object Photos {
    fun dir(ctx: Context): File = File(ctx.filesDir, "selfies").apply { mkdirs() }

    fun all(ctx: Context): List<File> =
        dir(ctx).listFiles { f -> f.name.endsWith(".jpg") && f.length() > 0 }
            ?.sortedBy { ts(it) } ?: emptyList()

    fun ts(f: File): Long = f.nameWithoutExtension.toLongOrNull() ?: f.lastModified()

    fun forSession(all: List<File>, s: Session): List<File> =
        all.filter { ts(it) in (s.start - 3_000)..(s.end + 15_000) }

    fun deleteAll(ctx: Context) {
        dir(ctx).listFiles()?.forEach { it.delete() }
    }

    /** Decode a downscaled bitmap and apply EXIF rotation (CameraX stores rotation in EXIF). */
    fun load(f: File, maxPx: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxPx && bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
        val bmp = BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
        if (bmp == null) {
            null
        } else {
            val exif = ExifInterface(f.absolutePath)
            val m = Matrix()
            val deg = exif.rotationDegrees
            if (deg != 0) m.postRotate(deg.toFloat())
            if (exif.isFlipped) m.postScale(-1f, 1f)
            if (m.isIdentity) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
    } catch (_: Exception) {
        null
    }
}
