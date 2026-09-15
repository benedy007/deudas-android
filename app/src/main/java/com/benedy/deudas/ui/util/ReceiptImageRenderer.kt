package com.benedy.deudas.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ReceiptImageRenderer {

    private const val WIDTH = 1080
    private const val PADDING = 64f
    private const val CORNER = 28f

    fun renderToCacheFile(
        context: Context,
        clientName: String,
        amount: Double,
        debtDescription: String,
        dateMs: Long,
        remaining: Double
    ): Uri {
        val bitmap = renderBitmap(
            clientName = clientName,
            amount = amount,
            debtDescription = debtDescription,
            dateMs = dateMs,
            remaining = remaining
        )
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "recibo_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                throw IllegalStateException("No se pudo generar la imagen del recibo")
            }
            out.flush()
            out.fd.sync()
        }
        bitmap.recycle()
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
        remaining: Double
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

        var contentHeight = PADDING + 80f + 48f + 36f + 28f + 100f + 40f
        rows.forEach { (_, value) ->
            contentHeight += 36f
            contentHeight += measureMultilineHeight(value, valuePaint, WIDTH - PADDING * 4)
            contentHeight += 36f
        }
        contentHeight += 48f + 40f + PADDING

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
        canvas.drawText("Deudas", WIDTH / 2f, y, appPaint)
        y += 36f
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

        y = cardBottom - 48f
        canvas.drawText("Gracias por su pago · Deudas", WIDTH / 2f, y, footerPaint)
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
