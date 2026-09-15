package com.benedy.deudas.ui.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ReceiptImageRenderer {

    private const val WIDTH = 1080
    private const val PADDING = 64f
    private const val CORNER = 28f

    /**
     * Prefers MediaStore content:// (more reliable with WhatsApp) on Q+.
     * Falls back to cache FileProvider URI on older APIs or if MediaStore fails.
     */
    fun renderForShare(
        context: Context,
        clientName: String,
        amount: Double,
        debtDescription: String,
        dateMs: Long,
        remaining: Double,
        companyName: String = "Deudas",
        companyPhone: String? = null,
        footerNote: String = "Gracias por su pago"
    ): Uri {
        val bitmap = renderBitmap(
            clientName = clientName,
            amount = amount,
            debtDescription = debtDescription,
            dateMs = dateMs,
            remaining = remaining,
            companyName = companyName,
            companyPhone = companyPhone,
            footerNote = footerNote
        )
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    insertIntoMediaStore(context, bitmap)
                } catch (_: Exception) {
                    writeToCacheFileProvider(context, bitmap)
                }
            } else {
                writeToCacheFileProvider(context, bitmap)
            }
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    /** Legacy name — delegates to [renderForShare]. */
    fun renderToCacheFile(
        context: Context,
        clientName: String,
        amount: Double,
        debtDescription: String,
        dateMs: Long,
        remaining: Double,
        companyName: String = "Deudas",
        companyPhone: String? = null,
        footerNote: String = "Gracias por su pago"
    ): Uri = renderForShare(
        context, clientName, amount, debtDescription, dateMs, remaining,
        companyName, companyPhone, footerNote
    )

    /**
     * Saves a PNG under Pictures/Deudas via MediaStore and returns content:// URI.
     * Keeps the bitmap (caller may recycle). Clears IS_PENDING on Q+.
     */
    fun insertIntoMediaStore(context: Context, bitmap: Bitmap): Uri {
        val resolver = context.contentResolver
        val displayName = "recibo_deudas_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/Deudas"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val uri = resolver.insert(collection, values)
            ?: throw IllegalStateException("MediaStore no devolvió URI")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IllegalStateException("No se pudo comprimir el PNG")
                }
                out.flush()
            } ?: throw IllegalStateException("No se pudo abrir el stream de MediaStore")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            return uri
        } catch (e: Exception) {
            try {
                resolver.delete(uri, null, null)
            } catch (_: Exception) {
            }
            throw e
        }
    }

    fun writeToCacheFileProvider(context: Context, bitmap: Bitmap): Uri {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "recibo_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw IllegalStateException("No se pudo generar la imagen del recibo")
            }
            out.flush()
            out.fd.sync()
        }
        if (!file.exists() || file.length() == 0L) {
            throw IllegalStateException("Archivo de recibo vacío")
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun renderBitmap(
        clientName: String,
        amount: Double,
        debtDescription: String,
        dateMs: Long,
        remaining: Double,
        companyName: String = "Deudas",
        companyPhone: String? = null,
        footerNote: String = "Gracias por su pago"
    ): Bitmap {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1B5E20.toInt()
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val appPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF52634F.toInt()
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF72796F.toInt()
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        val labelCenterPaint = Paint(labelPaint).apply {
            textAlign = Paint.Align.CENTER
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF181D18.toInt()
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1B5E20.toInt()
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFDEE5D8.toInt()
            strokeWidth = 3f
        }
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
        }
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x22000000
            style = Paint.Style.FILL
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFF7FBF2.toInt()
            style = Paint.Style.FILL
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFA5D6A7.toInt()
            style = Paint.Style.FILL
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF72796F.toInt()
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }

        val rows = listOf(
            "Cliente" to clientName,
            "Concepto" to debtDescription,
            "Saldo restante" to formatMoney(remaining),
            "Fecha" to formatDate(dateMs)
        )

        val phoneLine = companyPhone?.trim()?.takeIf { it.isNotEmpty() }
        var contentHeight = PADDING + 80f + 48f + 36f +
            (if (phoneLine != null) 40f else 0f) + 28f + 100f + 40f
        rows.forEach { (_, value) ->
            contentHeight += 36f
            contentHeight += measureMultilineHeight(value, valuePaint, WIDTH - PADDING * 4)
            contentHeight += 36f
        }
        val footerPreview = buildString {
            append(footerNote.ifBlank { "Gracias por su pago" })
            append(" · ")
            append(companyName.ifBlank { "Deudas" })
        }
        contentHeight += 48f + measureMultilineHeight(footerPreview, footerPaint, WIDTH - PADDING * 4) + 24f + PADDING

        val height = contentHeight.toInt().coerceAtLeast(1200)
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), height.toFloat(), bgPaint)

        val cardLeft = PADDING
        val cardTop = PADDING
        val cardRight = WIDTH - PADDING
        val cardBottom = height - PADDING
        canvas.drawRoundRect(
            RectF(cardLeft + 8f, cardTop + 10f, cardRight + 8f, cardBottom + 10f),
            CORNER,
            CORNER,
            shadowPaint
        )
        canvas.drawRoundRect(
            RectF(cardLeft, cardTop, cardRight, cardBottom),
            CORNER,
            CORNER,
            cardPaint
        )
        canvas.drawRoundRect(
            RectF(cardLeft, cardTop, cardRight, cardTop + 24f),
            CORNER,
            CORNER,
            accentPaint
        )
        canvas.drawRect(cardLeft, cardTop + 12f, cardRight, cardTop + 24f, accentPaint)

        var y = cardTop + 80f
        canvas.drawText("Comprobante de pago", WIDTH / 2f, y, titlePaint)
        y += 48f
        canvas.drawText(companyName.ifBlank { "Deudas" }, WIDTH / 2f, y, appPaint)
        y += 36f
        if (phoneLine != null) {
            canvas.drawText(phoneLine, WIDTH / 2f, y, labelCenterPaint)
            y += 40f
        }
        canvas.drawLine(cardLeft + PADDING, y, cardRight - PADDING, y, dividerPaint)
        y += 56f

        canvas.drawText("Monto pagado", WIDTH / 2f, y, labelCenterPaint)
        y += 72f
        canvas.drawText(formatMoney(amount), WIDTH / 2f, y, amountPaint)
        y += 48f
        canvas.drawLine(cardLeft + PADDING, y, cardRight - PADDING, y, dividerPaint)
        y += 48f

        val contentLeft = cardLeft + PADDING
        val maxValueWidth = WIDTH - PADDING * 4
        rows.forEach { (label, value) ->
            canvas.drawText(label, contentLeft, y, labelPaint)
            y += 44f
            y = drawMultiline(canvas, value, contentLeft, y, valuePaint, maxValueWidth)
            y += 36f
        }

        val footer = buildString {
            append(footerNote.ifBlank { "Gracias por su pago" })
            append(" · ")
            append(companyName.ifBlank { "Deudas" })
        }
        val footerLines = wrapLines(footer, footerPaint, WIDTH - PADDING * 4)
        val fm = footerPaint.fontMetrics
        val lineH = fm.descent - fm.ascent + fm.leading
        y = cardBottom - 24f - (footerLines.size - 1) * lineH
        footerLines.forEach { line ->
            canvas.drawText(line, WIDTH / 2f, y, footerPaint)
            y += lineH
        }
        return bitmap
    }

    private fun measureMultilineHeight(text: String, paint: Paint, maxWidth: Float): Float {
        val lines = wrapLines(text, paint, maxWidth)
        val fm = paint.fontMetrics
        val lineHeight = fm.descent - fm.ascent + fm.leading
        return lines.size * lineHeight
    }

    private fun drawMultiline(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        paint: Paint,
        maxWidth: Float
    ): Float {
        val lines = wrapLines(text, paint, maxWidth)
        val fm = paint.fontMetrics
        val lineHeight = fm.descent - fm.ascent + fm.leading
        var y = startY
        lines.forEach { line ->
            canvas.drawText(line, x, y, paint)
            y += lineHeight
        }
        return y
    }

    private fun wrapLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("—")
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines += current.toString()
                if (paint.measureText(word) <= maxWidth) {
                    current = StringBuilder(word)
                } else {
                    var remaining = word
                    while (remaining.isNotEmpty()) {
                        var cut = remaining.length
                        while (cut > 1 && paint.measureText(remaining.substring(0, cut)) > maxWidth) {
                            cut--
                        }
                        lines += remaining.substring(0, cut)
                        remaining = remaining.substring(cut)
                    }
                    current = StringBuilder()
                }
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines.ifEmpty { listOf("—") }
    }
}
