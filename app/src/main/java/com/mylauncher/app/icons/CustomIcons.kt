package com.mylauncher.app.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/** Draws a custom icon: shape, background color, and the picture inside the padding. */
object CustomIconRenderer {
    fun render(source: Bitmap, def: CustomIconDef, sizePx: Int): Bitmap {
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val path = Path()
        val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        when (def.shape) {
            CustomShape.CIRCLE -> path.addOval(rect, Path.Direction.CW)
            CustomShape.ROUNDED -> path.addRoundRect(rect, sizePx * 0.28f, sizePx * 0.28f, Path.Direction.CW)
            CustomShape.SQUARE -> path.addRect(rect, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(path)
        if (def.background != 0) canvas.drawColor(def.background)
        val pad = IconStudioMath.paddingPx(sizePx, def.paddingPercent)
        val target = Rect(pad, pad, sizePx - pad, sizePx - pad)
        canvas.drawBitmap(source, null, target, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        canvas.restore()
        return out
    }
}

/** The pictures behind custom icons live in the app's own private storage, so they survive restarts and reboots. */
class CustomIconStore(context: Context) {
    private val dir = File(context.applicationContext.filesDir, "custom_icons")

    private fun fileFor(packageName: String) = File(dir, IconExport.safeFileName(packageName))

    fun exists(packageName: String): Boolean = fileFor(packageName).isFile

    /** Saves the already-cropped square picture. Written to a temp file first, so a failure never leaves half a file. */
    fun save(packageName: String, picture: Bitmap): Boolean = try {
        dir.mkdirs()
        val target = fileFor(packageName)
        val temp = File(dir, target.name + ".tmp")
        FileOutputStream(temp).use { picture.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (target.exists()) target.delete()
        temp.renameTo(target)
    } catch (e: Exception) {
        false
    }

    fun load(packageName: String): Bitmap? = try {
        val file = fileFor(packageName)
        if (file.isFile) BitmapFactory.decodeFile(file.absolutePath) else null
    } catch (e: OutOfMemoryError) {
        null
    } catch (e: Exception) {
        null
    }

    fun readBytes(packageName: String): ByteArray? = try {
        val file = fileFor(packageName)
        if (file.isFile) file.readBytes() else null
    } catch (e: Exception) {
        null
    }

    fun delete(packageName: String) {
        try { fileFor(packageName).delete() } catch (e: Exception) { /* already gone */ }
    }

    fun deleteAll() {
        try { dir.listFiles()?.forEach { it.delete() } } catch (e: Exception) { /* nothing to clean */ }
    }
}

sealed interface PictureResult {
    data class Ok(val bitmap: Bitmap) : PictureResult
    data class Error(val message: String) : PictureResult
}

/** Reads a picture the user chose, safely: size and dimensions are checked and large pictures are downsampled. */
object PictureImport {
    fun load(context: Context, uri: Uri): PictureResult {
        val resolver = context.contentResolver
        return try {
            val length = try {
                resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            } catch (e: Exception) {
                -1L
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = resolver.openInputStream(uri) ?: return PictureResult.Error("Can't open this picture")
            // Reading only the size returns no bitmap by design, so the result is not checked here.
            boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
            val problem = IconStudioMath.validate(if (length >= 0) length else 0L, bounds.outWidth, bounds.outHeight)
            if (problem != null) return PictureResult.Error(problem)
            val options = BitmapFactory.Options().apply {
                inSampleSize = IconStudioMath.sampleSize(bounds.outWidth, bounds.outHeight)
            }
            val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return PictureResult.Error("This file is not a usable picture")
            PictureResult.Ok(bitmap)
        } catch (e: OutOfMemoryError) {
            PictureResult.Error("This picture is too large to open")
        } catch (e: Exception) {
            PictureResult.Error("Can't read this picture")
        }
    }

    /** The square part chosen with zoom and pan, scaled to the stored size. */
    fun cropSquare(
        source: Bitmap,
        zoom: Float,
        panX: Float,
        panY: Float,
        outputSide: Int = IconStudioMath.OUTPUT_SIDE,
    ): Bitmap {
        val (left, top, side) = IconStudioMath.cropRect(source.width, source.height, zoom, panX, panY).toList()
        val square = Bitmap.createBitmap(source, left, top, side, side)
        return if (side == outputSide) square else Bitmap.createScaledBitmap(square, outputSide, outputSide, true)
    }
}
