package com.simodev.gpspositionsaver.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.simodev.gpspositionsaver.geo.GeoUtils
import java.io.File

/** QR bitmap generation (replaces the JS `qrcode` library). */
object QrGenerator {
    fun generate(content: String, size: Int = 512): Bitmap? = try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        null
    }
}

/** Replaces `navigator.share` / `navigator.clipboard` / dialer / map intents. */
object IntentHelpers {

    fun shareText(context: Context, text: String, subject: String? = null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
        context.startActivity(Intent.createChooser(intent, subject ?: "Share"))
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun dial(context: Context, phone: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")))
        }
    }

    fun openMap(context: Context, lat: Double, lng: Double, label: String? = null) {
        val geo = Uri.parse(GeoUtils.getGeoUri(lat, lng, label))
        val intent = Intent(Intent.ACTION_VIEW, geo)
        runCatching { context.startActivity(intent) }
            .onFailure {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(GeoUtils.getOpenStreetMapUrl(lat, lng)))
                )
            }
    }

    fun openUrl(context: Context, url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
}

/** Copies a user-picked photo into app-private storage and hands back a stable path. */
object PhotoStore {

    fun importFromUri(context: Context, uri: Uri): String? = try {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val dest = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        dest.absolutePath
    } catch (e: Exception) {
        null
    }

    fun shareableUri(context: Context, path: String): Uri {
        val file = File(path)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
