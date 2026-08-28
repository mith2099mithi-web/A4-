package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfRendererHelper {

    suspend fun getPdfPageCount(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val pfd = getParcelFileDescriptor(context, uri) ?: return@withContext 0
            pfd.use {
                val renderer = PdfRenderer(it)
                val count = renderer.pageCount
                renderer.close()
                count
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun renderPdfPage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = 1190, // 2x of 595 for crisp rendering
        targetHeight: Int = 1684  // 2x of 842
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val pfd = getParcelFileDescriptor(context, uri) ?: return@withContext null
            pfd.use {
                val renderer = PdfRenderer(it)
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                    renderer.close()
                    return@withContext null
                }
                val page = renderer.openPage(pageIndex)
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getParcelFileDescriptor(context: Context, uri: Uri): ParcelFileDescriptor? {
        return try {
            if (uri.scheme == "content") {
                context.contentResolver.openFileDescriptor(uri, "r")
            } else {
                val file = File(uri.path ?: return null)
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
