package com.benedy.deudas.ui.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.benedy.deudas.data.repository.DebtCrmRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatementImageRenderer {

    private const val WIDTH = 1080
    private const val PADDING = 56f

    fun renderForShare(
        context: Context,
        statement: DebtCrmRepository.ClientStatement,
        companyName: String = "ContaFácil",
        companyPhone: String? = null
    ): Uri {
        val bitmap = renderBitmap(statement, companyName, companyPhone)
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    insertIntoMediaStore(context, bitmap)
                } catch (_: Exception) {
                    writeToCache(context, bitmap)
                }
            } else {
                writeToCache(context, bitmap)
            }
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun renderBitmap(
        statement: DebtCrmRepository.ClientStatement,
        companyName: String,
        companyPhone: String?
    ): Bitmap {
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es", "DO"))
        val brand = companyName.ifBlank { "ContaFácil" }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0D47A1.toInt()
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF212121.toInt()
            textSize = 34f
        }
        val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF616161.toInt()
            textSize = 28f
        }
        val credit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF2E7D32.toInt()
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val debit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFC62828.toInt()
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val lineH = 48f
        val headerH = 280f
        val footerH = 160f
        val linesH = (statement.lines.size.coerceAtLeast(1) * lineH) + 40f
        val height = (headerH + linesH + footerH + PADDING * 2).toInt().coerceAtLeast(900)

        val bmp = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(0xFFF5F7FA.toInt())
        canvas.drawRoundRect(
            PADDING / 2, PADDING / 2,
            WIDTH - PADDING / 2, height - PADDING / 2,
            24f, 24f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        )

        var y = PADDING + 48f
        canvas.drawText("Estado de cuenta", PADDING, y, titlePaint)
        y += 56f
        canvas.drawText(brand, PADDING, y, body)
        y += 40f
        companyPhone?.trim()?.takeIf { it.isNotEmpty() }?.let {
            canvas.drawText("Tel: $it", PADDING, y, muted)
            y += 36f
        }
        canvas.drawText("Cliente: ${statement.client.name}", PADDING, y, body)
        y += 40f
        canvas.drawText("Teléfono: ${statement.client.phone}", PADDING, y, muted)
        y += 40f
        canvas.drawText(
            "Emitido: ${dateFmt.format(Date())}",
            PADDING, y, muted
        )
        y += 56f

        canvas.drawText("Movimientos", PADDING, y, body.apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })
        y += 44f
        body.typeface = Typeface.DEFAULT

        if (statement.lines.isEmpty()) {
            canvas.drawText("Sin movimientos", PADDING, y, muted)
            y += lineH
        } else {
            statement.lines.take(40).forEach { line ->
                val date = dateFmt.format(Date(line.dateMs))
                val sign = if (line.isCredit) "+" else "−"
                val paint = if (line.isCredit) credit else debit
                val left = "$date  ${line.label}".take(42)
                canvas.drawText(left, PADDING, y, muted)
                val amountStr = "$sign${formatMoney(line.amount)}"
                val aw = paint.measureText(amountStr)
                canvas.drawText(amountStr, WIDTH - PADDING - aw, y, paint)
                y += lineH
            }
            if (statement.lines.size > 40) {
                canvas.drawText("… y más movimientos", PADDING, y, muted)
                y += lineH
            }
        }

        y += 24f
        canvas.drawText(
            "Total cargos: ${formatMoney(statement.totalDebt)}",
            PADDING, y, body
        )
        y += 40f
        canvas.drawText(
            "Total pagos: ${formatMoney(statement.totalPaid)}",
            PADDING, y, credit
        )
        y += 48f
        val remPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0D47A1.toInt()
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "Saldo pendiente: ${formatMoney(statement.remaining)}",
            PADDING, y, remPaint
        )
        return bmp
    }

    private fun insertIntoMediaStore(context: Context, bitmap: Bitmap): Uri {
        val name = "estado_cuenta_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Deudas")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("MediaStore insert failed")
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        } ?: error("No output stream")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    private fun writeToCache(context: Context, bitmap: Bitmap): Uri {
        val dir = File(context.cacheDir, "receipts").also { it.mkdirs() }
        val file = File(dir, "estado_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
