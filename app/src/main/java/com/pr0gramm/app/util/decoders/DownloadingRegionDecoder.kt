package com.pr0gramm.app.util.decoders

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder
import com.google.common.base.Preconditions.checkState
import com.google.common.io.ByteStreams
import com.pr0gramm.app.util.AndroidUtility.toFile
import com.squareup.picasso.Downloader
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * This decoder first downloads the image before starting to decode it.
 */
class DownloadingRegionDecoder(private val downloader: Downloader, private val decoder: ImageRegionDecoder) : ImageRegionDecoder {
    private var imageFile: File? = null
    private var deleteImageOnRecycle: Boolean = false

    @Throws(Exception::class)
    override fun init(context: Context, uri: Uri): Point {
        checkState(imageFile == null, "Can not call init twice.")

        val file = if ("file" == uri.scheme) {
            toFile(uri).also { imageFile = it }
        } else {
            val file = File.createTempFile("image", ".tmp", context.cacheDir)
            imageFile = file
            deleteImageOnRecycle = true

            try {
                downloadTo(uri, file)
                file

            } catch (error: IOException) {
                logger.warn("Could not download image to temp file")

                if (!file.delete())
                    logger.warn("Could not delete file")

                // re-raise exception
                throw IOException("Could not download image to temp file", error)
            }
        }

        return decoder.init(context, Uri.fromFile(file))
    }

    override fun decodeRegion(rect: Rect, sample: Int): Bitmap {
        try {
            val result = decoder.decodeRegion(rect, sample)
            if (result != null)
                return result

            throw RuntimeException("Could not decode")

        } catch (oom: OutOfMemoryError) {
            throw RuntimeException(oom)
        }
    }

    override fun isReady(): Boolean {
        return imageFile != null && decoder.isReady
    }

    override fun recycle() {
        try {
            decoder.recycle()
        } finally {
            cleanup()
        }
    }

    private fun cleanup() {
        imageFile?.let { file ->
            if (deleteImageOnRecycle && file.exists()) {
                file.delete()
            }
        }
    }

    protected fun finalize() {
        cleanup()
    }

    private fun downloadTo(uri: Uri, imageFile: File) {
        // download to temp file. not nice, but useful :/
        downloader.load(uri, 0).inputStream.use { inputStream ->
            FileOutputStream(imageFile).use { output ->
                ByteStreams.copy(inputStream, output)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger("DownloadingRegionDecoder")
    }
}
