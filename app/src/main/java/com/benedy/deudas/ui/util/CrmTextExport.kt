package com.benedy.deudas.ui.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.benedy.deudas.R
import com.benedy.deudas.data.local.entity.ClientEntity
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.data.local.entity.PaymentEntity
import com.benedy.deudas.data.local.entity.ProductEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a human-readable UTF-8 text report of CRM data and shares / saves it.
 */
object CrmTextExport {

    private val localeEs = Locale("es", "DO")
    private val stampFormat = SimpleDateFormat("yyyyMMdd-HHmm", localeEs)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", localeEs)
    private const val RECENT_PAYMENTS = 5

    data class Result(
        val shareUri: Uri,
        val fileName: String,
        val downloadsUri: Uri? = null
    )

    fun buildReport(
        clients: List<ClientEntity>,
        debts: List<DebtEntity>,
        payments: List<PaymentEntity>,
        products: List<ProductEntity> = emptyList()
    ): String {
        val debtsByClient = debts.groupBy { it.clientId }
        val paymentsByClient = payments.groupBy { it.clientId }
        val sortedClients = clients.sortedBy { it.name.lowercase(localeEs) }
        val grandTotal = debts.sumOf { it.remainingBalance.coerceAtLeast(0.0) }
        val now = System.currentTimeMillis()

        return buildString {
            appendLine("ContaFácil — Exportación")
            appendLine("Fecha: ${dateFormat.format(Date(now))}")
            appendLine("Clientes: ${sortedClients.size}")
            appendLine("Total general adeudado: ${rd(grandTotal)}")
            appendLine()

            if (products.isNotEmpty()) {
                appendLine("Productos (${products.size}):")
                products.sortedBy { it.name.lowercase(localeEs) }.forEach { p ->
                    appendLine("  - ${p.name} | ${rd(p.price)}")
                }
                appendLine()
            }

            if (sortedClients.isEmpty()) {
                appendLine("(Sin clientes)")
                return@buildString
            }

            for (client in sortedClients) {
                val clientDebts = debtsByClient[client.id].orEmpty()
                    .sortedByDescending { it.createdAt }
                val clientTotal = clientDebts.sumOf { it.remainingBalance.coerceAtLeast(0.0) }
                val clientPayments = paymentsByClient[client.id].orEmpty()
                    .sortedByDescending { it.createdAt }
                    .take(RECENT_PAYMENTS)

                appendLine("Cliente: ${client.name}")
                if (client.phone.isNotBlank()) {
                    appendLine("Tel: ${client.phone}")
                }
                client.direccionCasa?.takeIf { it.isNotBlank() }?.let {
                    appendLine("Dir. casa: $it")
                }
                client.lugarTrabajo?.takeIf { it.isNotBlank() }?.let {
                    appendLine("Trabajo: $it")
                }
                client.direccionTrabajo?.takeIf { it.isNotBlank() }?.let {
                    appendLine("Dir. trabajo: $it")
                }
                client.notes?.takeIf { it.isNotBlank() }?.let {
                    appendLine("Notas: $it")
                }
                appendLine("Total adeudado: ${rd(clientTotal)}")

                if (clientDebts.isEmpty()) {
                    appendLine("  (Sin deudas)")
                } else {
                    for (d in clientDebts) {
                        val fecha = dateFormat.format(Date(d.createdAt))
                        appendLine(
                            "  - Deuda: ${d.description} | Monto original: ${rd(d.originalAmount)}" +
                                " | Saldo: ${rd(d.remainingBalance)} | Fecha: $fecha"
                        )
                    }
                }

                if (clientPayments.isNotEmpty()) {
                    appendLine("Pagos recientes:")
                    for (p in clientPayments) {
                        val fecha = dateFormat.format(Date(p.createdAt))
                        val note = p.note?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                        appendLine("  - ${rd(p.amount)} | $fecha$note")
                    }
                }
                appendLine()
            }

            appendLine("— Fin de la exportación ContaFácil")
        }
    }

    /**
     * Writes the report to app cache (FileProvider), optionally copies to Downloads,
     * and returns a shareable content URI.
     */
    fun writeAndPrepareShare(context: Context, reportText: String): Result {
        val stamp = stampFormat.format(Date())
        val fileName = "ContaFacil-export-$stamp.txt"
        val bytes = reportText.toByteArray(Charsets.UTF_8)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            out.write(bytes)
            out.flush()
            out.fd.sync()
        }
        if (!file.exists() || file.length() == 0L) {
            throw IllegalStateException("Archivo de exportación vacío")
        }

        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val downloadsUri = try {
            insertIntoDownloads(context, fileName, bytes)
        } catch (_: Exception) {
            null
        }

        return Result(shareUri = shareUri, fileName = fileName, downloadsUri = downloadsUri)
    }

    fun shareTextFile(context: Context, uri: Uri, fileName: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            putExtra(Intent.EXTRA_TITLE, fileName)
            clipData = ClipData(
                ClipDescription(fileName, arrayOf("text/plain")),
                ClipData.Item(uri)
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(
            send,
            context.getString(R.string.export_text_chooser_title)
        ).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = send.clipData
        }
        val activity = context.findActivity()
        if (activity != null) {
            activity.startActivity(chooser)
        } else {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    fun openTextFile(context: Context, uri: Uri, fileName: String) {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/plain")
            putExtra(Intent.EXTRA_TITLE, fileName)
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val activity = context.findActivity()
        if (activity != null) {
            activity.startActivity(view)
        } else {
            view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(view)
        }
    }

    private fun insertIntoDownloads(context: Context, fileName: String, bytes: ByteArray): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // MediaStore Downloads collection is API 29+; skip on 26–28 without storage permission.
            return null
        }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: return null
        try {
            resolver.openOutputStream(uri)?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: run {
                resolver.delete(uri, null, null)
                return null
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (e: Exception) {
            try {
                resolver.delete(uri, null, null)
            } catch (_: Exception) {
            }
            throw e
        }
    }

    private fun rd(amount: Double): String {
        // Prefer app formatter (es_DO); ensure RD$ label is visible in plain text.
        val formatted = formatMoney(amount)
        return if (formatted.contains("RD", ignoreCase = true)) {
            formatted
        } else {
            "RD$ " + String.format(localeEs, "%,.2f", amount)
        }
    }

    private fun Context.findActivity(): android.app.Activity? {
        var ctx: Context? = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
