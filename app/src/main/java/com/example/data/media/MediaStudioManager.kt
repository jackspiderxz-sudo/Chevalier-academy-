package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class CropAspectRatioPreset(val fixedAspectRatio: Float?) {
    ORIGINAL(null),
    FREE(null),
    RATIO_16_9(16f / 9f),
    RATIO_4_3(4f / 3f),
    RATIO_1_1(1f)
}

data class MediaAdjustmentSpec(
    val aspectPreset: CropAspectRatioPreset = CropAspectRatioPreset.RATIO_16_9,
    val customAspectRatio: Float? = null,
    val zoom: Float = 1f,
    val panFractionX: Float = 0f,
    val panFractionY: Float = 0f,
    val rotationDegrees: Int = 0
)

data class ProcessedMediaResult(
    val originalUri: String,
    val optimizedUri: String,
    val mimeType: String,
    val widthPx: Int,
    val heightPx: Int,
    val originalSizeBytes: Long,
    val optimizedSizeBytes: Long,
    val usedOriginalAsDisplay: Boolean
)

class MediaStudioManager(private val context: Context) {

    private val originalsDir: File
        get() = File(context.filesDir, "chevalier_media/originals").apply { mkdirs() }

    private val optimizedDir: File
        get() = File(context.filesDir, "chevalier_media/optimized").apply { mkdirs() }

    private val adjustedDir: File
        get() = File(context.filesDir, "chevalier_media/adjusted").apply { mkdirs() }

    suspend fun importAndOptimizeImage(sourceUri: Uri): Result<ProcessedMediaResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val detectedMime = resolver.getType(sourceUri)?.lowercase()
                    ?: guessMimeFromPath(sourceUri.toString())

                val ext = when {
                    detectedMime.contains("png") -> "png"
                    detectedMime.contains("webp") -> "webp"
                    detectedMime.contains("jpeg") || detectedMime.contains("jpg") -> "jpg"
                    else -> throw IllegalArgumentException("Unsupported image format. Supported: JPG, JPEG, PNG, WEBP.")
                }

                val baseId = UUID.randomUUID().toString()
                val originalFile = File(originalsDir, "${baseId}_orig.$ext")
                resolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(originalFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("Unable to read selected image file.")

                if (originalFile.length() == 0L) {
                    originalFile.delete()
                    throw IllegalStateException("Selected image file is empty or unreadable.")
                }

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(originalFile.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    originalFile.delete()
                    throw IllegalArgumentException("Corrupted or invalid image file.")
                }

                val originalSize = originalFile.length()
                val maxDimension = 1600
                val needsResize = max(bounds.outWidth, bounds.outHeight) > maxDimension

                if (!needsResize && ext == "png") {
                    // Already display-sized PNG: preserve original losslessly without re-encoding
                    return@runCatching ProcessedMediaResult(
                        originalUri = Uri.fromFile(originalFile).toString(),
                        optimizedUri = Uri.fromFile(originalFile).toString(),
                        mimeType = "image/png",
                        widthPx = bounds.outWidth,
                        heightPx = bounds.outHeight,
                        originalSizeBytes = originalSize,
                        optimizedSizeBytes = originalSize,
                        usedOriginalAsDisplay = true
                    )
                }

                val sampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = BitmapFactory.decodeFile(originalFile.absolutePath, decodeOpts)
                    ?: return@runCatching ProcessedMediaResult(
                        originalUri = Uri.fromFile(originalFile).toString(),
                        optimizedUri = Uri.fromFile(originalFile).toString(),
                        mimeType = normalizedMime(ext),
                        widthPx = bounds.outWidth,
                        heightPx = bounds.outHeight,
                        originalSizeBytes = originalSize,
                        optimizedSizeBytes = originalSize,
                        usedOriginalAsDisplay = true
                    )

                val oriented = applyExifOrientation(decoded, originalFile.absolutePath)
                val scaled = if (max(oriented.width, oriented.height) > maxDimension) {
                    val ratio = maxDimension.toFloat() / max(oriented.width, oriented.height).toFloat()
                    val targetW = (oriented.width * ratio).roundToInt().coerceAtLeast(1)
                    val targetH = (oriented.height * ratio).roundToInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(oriented, targetW, targetH, true).also {
                        if (it !== oriented) oriented.recycle()
                    }
                } else {
                    oriented
                }

                val optimizedFile = File(optimizedDir, "${baseId}_opt.$ext")
                val compressFormat = compressFormatForExt(ext)
                val quality = if (ext == "png") 100 else 90

                FileOutputStream(optimizedFile).use { out ->
                    scaled.compress(compressFormat, quality, out)
                }
                val outW = scaled.width
                val outH = scaled.height
                scaled.recycle()

                val optSize = optimizedFile.length()
                val keepOriginal = optSize <= 0L || (optSize >= originalSize && !needsResize)
                if (keepOriginal) {
                    optimizedFile.delete()
                }

                ProcessedMediaResult(
                    originalUri = Uri.fromFile(originalFile).toString(),
                    optimizedUri = if (keepOriginal) {
                        Uri.fromFile(originalFile).toString()
                    } else {
                        Uri.fromFile(optimizedFile).toString()
                    },
                    mimeType = normalizedMime(ext),
                    widthPx = if (keepOriginal) bounds.outWidth else outW,
                    heightPx = if (keepOriginal) bounds.outHeight else outH,
                    originalSizeBytes = originalSize,
                    optimizedSizeBytes = if (keepOriginal) originalSize else optSize,
                    usedOriginalAsDisplay = keepOriginal
                )
            }
        }

    suspend fun saveAdjustedMedia(
        sourceImageUri: String,
        spec: MediaAdjustmentSpec,
        outputTag: String = "adj"
    ): Result<ProcessedMediaResult> = withContext(Dispatchers.IO) {
        runCatching {
            val resolvedOriginalUri = resolveOriginalUri(sourceImageUri)
            val parsedUri = Uri.parse(resolvedOriginalUri)
            val sourcePath = if (parsedUri.scheme == "file" && !parsedUri.path.isNullOrBlank()) {
                parsedUri.path!!
            } else {
                val imported = importAndOptimizeImage(parsedUri).getOrThrow()
                Uri.parse(imported.originalUri).path
                    ?: throw IllegalStateException("Unable to locate imported original image.")
            }

            val sourceFile = File(sourcePath)
            if (!sourceFile.exists() || sourceFile.length() <= 0L) {
                throw IllegalStateException("Original image file is unavailable for adjustment.")
            }

            val ext = guessExtFromPath(sourcePath)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourcePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw IllegalArgumentException("Unable to decode image dimensions for crop.")
            }

            // Decode with high detail while guarding memory on extreme megapixels
            val sampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, 2048)
            val decoded = BitmapFactory.decodeFile(
                sourcePath,
                BitmapFactory.Options().apply { inSampleSize = sampleSize }
            ) ?: throw IllegalStateException("Failed to decode image for crop.")

            val exifOriented = applyExifOrientation(decoded, sourcePath)
            val quarterTurns = (((spec.rotationDegrees.toFloat() / 90f).roundToInt() % 4) + 4) % 4
            val workingBitmap = if (quarterTurns != 0) {
                val matrix = Matrix().apply { postRotate(quarterTurns * 90f) }
                Bitmap.createBitmap(
                    exifOriented,
                    0,
                    0,
                    exifOriented.width,
                    exifOriented.height,
                    matrix,
                    true
                ).also {
                    if (it !== exifOriented) exifOriented.recycle()
                }
            } else {
                exifOriented
            }

            val imgW = workingBitmap.width.toFloat()
            val imgH = workingBitmap.height.toFloat()
            val naturalAspect = (imgW / imgH).coerceAtLeast(0.1f)
            val resolvedAspectRatio = when (spec.aspectPreset) {
                CropAspectRatioPreset.ORIGINAL -> naturalAspect
                CropAspectRatioPreset.FREE -> spec.customAspectRatio?.takeIf { it > 0f } ?: naturalAspect
                else -> spec.aspectPreset.fixedAspectRatio ?: naturalAspect
            }.coerceIn(0.1f, 10f)

            val viewW = resolvedAspectRatio
            val viewH = 1f
            val baseScale = max(viewW / imgW, viewH / imgH)
            val clampedZoom = spec.zoom.coerceIn(1f, 4f)
            val totalScale = baseScale * clampedZoom

            val cropW = (viewW / totalScale).coerceIn(1f, imgW)
            val cropH = (viewH / totalScale).coerceIn(1f, imgH)
            val maxShiftX = max(0f, imgW - cropW)
            val maxShiftY = max(0f, imgH - cropH)

            val cropLeftF = (maxShiftX / 2f) * (1f - spec.panFractionX.coerceIn(-1f, 1f))
            val cropTopF = (maxShiftY / 2f) * (1f - spec.panFractionY.coerceIn(-1f, 1f))

            val cropLeft = cropLeftF.roundToInt().coerceIn(0, (workingBitmap.width - 1).coerceAtLeast(0))
            val cropTop = cropTopF.roundToInt().coerceIn(0, (workingBitmap.height - 1).coerceAtLeast(0))
            val cropWidthPx = cropW.roundToInt().coerceIn(1, workingBitmap.width - cropLeft)
            val cropHeightPx = cropH.roundToInt().coerceIn(1, workingBitmap.height - cropTop)

            val cropped = Bitmap.createBitmap(workingBitmap, cropLeft, cropTop, cropWidthPx, cropHeightPx)
            if (cropped !== workingBitmap) {
                workingBitmap.recycle()
            }

            val targetMaxDim = 1600
            val finalBitmap = if (max(cropped.width, cropped.height) > targetMaxDim) {
                val (targetW, targetH) = if (cropped.width >= cropped.height) {
                    targetMaxDim to (targetMaxDim / viewW).roundToInt().coerceAtLeast(1)
                } else {
                    (targetMaxDim * viewW).roundToInt().coerceAtLeast(1) to targetMaxDim
                }
                Bitmap.createScaledBitmap(cropped, targetW, targetH, true).also {
                    if (it !== cropped) cropped.recycle()
                }
            } else {
                cropped
            }

            val safeTag = outputTag.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { "adj" }
            val adjustedFile = File(adjustedDir, "${UUID.randomUUID()}_$safeTag.$ext")
            val format = compressFormatForExt(ext)
            val quality = if (ext == "png") 100 else 92
            FileOutputStream(adjustedFile).use { out ->
                finalBitmap.compress(format, quality, out)
            }
            val finalW = finalBitmap.width
            val finalH = finalBitmap.height
            finalBitmap.recycle()

            val canonicalOriginalUri = Uri.fromFile(sourceFile).toString()
            // Sidecar mapping allows future edits / Reset to always recover the preserved original photo
            File(adjustedDir, "${adjustedFile.name}.orig").writeText(canonicalOriginalUri)

            ProcessedMediaResult(
                originalUri = canonicalOriginalUri,
                optimizedUri = Uri.fromFile(adjustedFile).toString(),
                mimeType = normalizedMime(ext),
                widthPx = finalW,
                heightPx = finalH,
                originalSizeBytes = sourceFile.length(),
                optimizedSizeBytes = adjustedFile.length(),
                usedOriginalAsDisplay = false
            )
        }
    }

    suspend fun saveAdjustedHeroCrop(
        sourceImageUri: String,
        zoom: Float,
        panFractionX: Float,
        panFractionY: Float,
        viewportAspectRatio: Float = HERO_BANNER_ASPECT_RATIO
    ): Result<ProcessedMediaResult> = saveAdjustedMedia(
        sourceImageUri = sourceImageUri,
        spec = MediaAdjustmentSpec(
            aspectPreset = if (abs(viewportAspectRatio - HERO_BANNER_ASPECT_RATIO) < 0.01f) {
                CropAspectRatioPreset.RATIO_16_9
            } else {
                CropAspectRatioPreset.FREE
            },
            customAspectRatio = viewportAspectRatio,
            zoom = zoom,
            panFractionX = panFractionX,
            panFractionY = panFractionY,
            rotationDegrees = 0
        ),
        outputTag = "hero_adj"
    )

    fun resolveOriginalUri(uriString: String): String {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                val file = File(uri.path!!)
                val sidecar = File(adjustedDir, "${file.name}.orig")
                if (sidecar.exists()) {
                    val recordedOriginal = sidecar.readText().trim()
                    if (recordedOriginal.isNotEmpty() && isManagedMediaFileReadable(recordedOriginal)) {
                        return recordedOriginal
                    }
                }
                // Also support optimized -> original fallback if inside chevalier_media/optimized
                if (file.parentFile?.name == "optimized") {
                    val base = file.nameWithoutExtension.removeSuffix("_opt")
                    val candidate = originalsDir.listFiles()?.firstOrNull {
                        it.name.startsWith("${base}_orig") && it.exists() && it.length() > 0L
                    }
                    if (candidate != null) {
                        return Uri.fromFile(candidate).toString()
                    }
                }
            }
            uriString
        } catch (_: Throwable) {
            uriString
        }
    }

    fun isManagedMediaFileReadable(uriString: String): Boolean {
        if (uriString.isBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file" || (uri.scheme == null && uriString.startsWith("/"))) {
                val path = uri.path ?: uriString
                val f = File(path)
                f.exists() && f.length() > 0L
            } else {
                true
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun deleteManagedMediaFiles(originalUri: String, optimizedUri: String?) {
        listOfNotNull(originalUri, optimizedUri).distinct().forEach { uriStr ->
            try {
                val uri = Uri.parse(uriStr)
                if (uri.scheme == "file") {
                    uri.path?.let { path ->
                        val file = File(path)
                        if (file.absolutePath.contains("chevalier_media") && file.exists()) {
                            File(adjustedDir, "${file.name}.orig").takeIf { it.exists() }?.delete()
                            file.delete()
                        }
                    }
                }
            } catch (_: Throwable) {
                // Ignore safely
            }
        }
    }

    fun listAllManagedFiles(): List<File> {
        val list = mutableListOf<File>()
        originalsDir.listFiles()?.filter { it.isFile }?.let { list.addAll(it) }
        optimizedDir.listFiles()?.filter { it.isFile }?.let { list.addAll(it) }
        adjustedDir.listFiles()?.filter { it.isFile }?.let { list.addAll(it) }
        return list
    }

    private fun applyExifOrientation(bitmap: Bitmap, filePath: String): Bitmap {
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                if (it !== bitmap) bitmap.recycle()
            }
        } catch (_: Throwable) {
            bitmap
        }
    }

    private fun compressFormatForExt(ext: String): Bitmap.CompressFormat = when (ext) {
        "png" -> Bitmap.CompressFormat.PNG
        "webp" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        else -> Bitmap.CompressFormat.JPEG
    }

    companion object {
        const val HERO_BANNER_ASPECT_RATIO: Float = 16f / 9f

        fun rotateQuarterTurns(currentDegrees: Int, clockwise: Boolean): Int {
            val delta = if (clockwise) 90 else -90
            return (((currentDegrees + delta) % 360) + 360) % 360
        }

        fun isValidYouTubeUrl(url: String): Boolean {
            val trimmed = url.trim()
            if (trimmed.isBlank()) return false
            return try {
                val uri = Uri.parse(trimmed)
                val scheme = uri.scheme?.lowercase()
                val host = uri.host?.lowercase() ?: return false
                (scheme == "https" || scheme == "http") &&
                    (host == "youtube.com" ||
                        host == "www.youtube.com" ||
                        host == "m.youtube.com" ||
                        host == "youtu.be")
            } catch (_: Throwable) {
                false
            }
        }

        private fun guessMimeFromPath(path: String): String {
            val lower = path.lowercase()
            return when {
                lower.endsWith(".png") -> "image/png"
                lower.endsWith(".webp") -> "image/webp"
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
                else -> "application/octet-stream"
            }
        }

        private fun guessExtFromPath(path: String): String {
            val lower = path.lowercase()
            return when {
                lower.endsWith(".png") -> "png"
                lower.endsWith(".webp") -> "webp"
                else -> "jpg"
            }
        }

        private fun normalizedMime(ext: String): String = when (ext) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }

        private fun computeInSampleSize(width: Int, height: Int, maxDim: Int): Int {
            var inSampleSize = 1
            while ((width / inSampleSize) > maxDim * 2 || (height / inSampleSize) > maxDim * 2) {
                inSampleSize *= 2
            }
            return inSampleSize
        }
    }
}
