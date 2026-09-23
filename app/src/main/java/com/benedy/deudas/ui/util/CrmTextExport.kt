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
import java.util.TimeZone

/**
 * Builds a human-readable UTF-8 text report of CRM data and shares / saves it.
 * Professional "ficha" layout for Cuentas por Cobrar (plain text / markdown-friendly).
 */
object CrmTextExport {

    private val localeEs = Locale("es", "DO")
    private val zoneDo = TimeZone.getTimeZone("America/Santo_Domingo")
    private val stampFormat = SimpleDateFormat("yyyyMMdd-HHmm", localeEs).apply {
        timeZone = zoneDo
    }
    private val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", localeEs).apply {
        timeZone = zoneDo
    }
    private const val SEPARATOR = "========================================"

    data class Result(
        val shareUri: Uri,
        val fileName: String,
        val downloadsUri: Uri? = null
    )

    private data class ClientBucket(
        val client: ClientEntity,
        val pendingDebts: List<DebtEntity>,
        val paidDebts: List<DebtEntity>,
        val clientPayments: List<PaymentEntity>,
        val balance: Double
    )

    fun buildReport(
        clients: List<ClientEntity>,
        debts: List<DebtEntity>,
        payments: List<PaymentEntity>,
        products: List<ProductEntity> = emptyList()
    ): String {
        val debtsByClient = debts.groupBy { it.clientId }
        val paymentsByClient = payments.groupBy { it.clientId }
        val debtsById = debts.associateBy { it.id }

        val buckets = clients.map { client ->
            val clientDebts = debtsByClient[client.id].orEmpty()
            val pending = clientDebts
                .filter { it.remainingBalance > 0.0 }
                .sortedByDescending { it.createdAt }
            val paid = clientDebts
                .filter { it.remainingBalance <= 0.0 }
                .sortedByDescending { it.createdAt }
            val balance = pending.sumOf { it.remainingBalance.coerceAtLeast(0.0) }
            val clientPayments = paymentsByClient[client.id].orEmpty()
                .sortedByDescending { it.createdAt }
            ClientBucket(client, pending, paid, clientPayments, balance)
        }

        val withBalance = buckets
            .filter { it.balance > 0.0 }
            .sortedBy { it.client.name.lowercase(localeEs) }
        val upToDate = buckets
            .filter { it.balance <= 0.0 }
            .sortedBy { it.client.name.lowercase(localeEs) }

        val grandTotal = withBalance.sumOf { it.balance }
        val now = System.currentTimeMillis()

        return buildString {
            appendLine("Reporte de Cuentas por Cobrar — ContaFácil")
            appendLine("Fecha de emisión: ${formatDateOnly(now)}")
            appendLine("Clientes con saldo pendiente: ${withBalance.size}")
            appendLine("Total General Pendiente: ${rd(grandTotal)}")
            appendLine()

            if (withBalance.isEmpty()) {
                appendLine("(Ningún cliente con saldo pendiente)")
                appendLine()
            } else {
                withBalance.forEachIndexed { index, bucket ->
                    appendClientFicha(index + 1, bucket, debtsById)
                }
            }

            if (upToDate.isNotEmpty()) {
                appendLine(SEPARATOR)
                appendLine("Clientes al día")
                for (b in upToDate) {
                    appendLine("  - ${toTitleCase(b.client.name)}")
                }
                appendLine()
            }

            if (products.isNotEmpty()) {
                appendLine(SEPARATOR)
                appendLine("Precios de referencia")
                products.sortedBy { it.name.lowercase(localeEs) }.forEach { p ->
                    appendLine("  - ${p.name} | ${rd(p.price)}")
                }
                appendLine()
            }

            appendLine("— Fin del reporte ContaFácil")
        }
    }

    private fun StringBuilder.appendClientFicha(
        index: Int,
        bucket: ClientBucket,
        debtsById: Map<Long, DebtEntity>
    ) {
        val client = bucket.client
        appendLine(SEPARATOR)
        appendLine("$index. ${toTitleCase(client.name)}")
        if (client.phone.isNotBlank()) {
            appendLine("Teléfono: ${client.phone}")
        }
        client.direccionCasa?.takeIf { it.isNotBlank() }?.let {
            appendLine("Dirección (Casa): $it")
        }
        cleanOccupation(client.lugarTrabajo, client.direccionTrabajo)?.let {
            appendLine("Ocupación: $it")
        }
        client.notes?.takeIf { it.isNotBlank() }?.let {
            appendLine("Nota: $it")
        }
        appendLine("BALANCE PENDIENTE: ${rd(bucket.balance)}")
        appendLine()

        appendLine("Deudas pendientes")
        appendLine("Concepto / Producto | Fecha | Monto original | Saldo pendiente")
        if (bucket.pendingDebts.isEmpty()) {
            appendLine("(ninguna)")
        } else {
            for (d in bucket.pendingDebts) {
                appendLine(
                    "${d.description} | ${formatDateOnly(d.createdAt)} | " +
                        "${rd(d.originalAmount)} | ${rd(d.remainingBalance)}"
                )
            }
        }
        appendLine()

        val hasHistory = bucket.clientPayments.isNotEmpty() || bucket.paidDebts.isNotEmpty()
        if (hasHistory) {
            appendLine("--- Historial ---")
            if (bucket.clientPayments.isNotEmpty()) {
                appendLine("Pagos recibidos:")
                for (p in bucket.clientPayments) {
                    val concept = p.note?.takeIf { it.isNotBlank() }
                        ?: debtsById[p.debtId]?.description?.takeIf { it.isNotBlank() }
                    val suffix = concept?.let { " ($it)" } ?: ""
                    appendLine("  ${rd(p.amount)} — ${formatDateOnly(p.createdAt)}$suffix")
                }
            }
            if (bucket.paidDebts.isNotEmpty()) {
                appendLine("Deudas saldadas (RD$ 0.00):")
                for (d in bucket.paidDebts) {
                    appendLine("  ${d.description} (Original: ${rd(d.originalAmount)})")
                }
            }
            appendLine()
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

    /** RD$ with US-style thousands comma and 2 decimals, e.g. RD$ 13,870.00 */
    private fun rd(amount: Double): String =
        "RD$ " + String.format(Locale.US, "%,.2f", amount)

    private fun formatDateOnly(epochMs: Long): String =
        dateOnlyFormat.format(Date(epochMs))

    fun toTitleCase(raw: String): String {
        val trimmed = raw.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isEmpty()) return trimmed
        return trimmed.split(" ").joinToString(" ") { word ->
            if (word.isEmpty()) word
            else word.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(localeEs) else ch.toString()
            }
        }
    }

    /**
     * Combines workplace + work address, dropping near-duplicates
     * (e.g. "calle" / "la calle").
     */
    fun cleanOccupation(workplace: String?, workAddress: String?): String? {
        val w = workplace?.trim()?.takeIf { it.isNotBlank() }
        val a = workAddress?.trim()?.takeIf { it.isNotBlank() }
        return when {
            w == null && a == null -> null
            w == null -> a
            a == null -> w
            nearlySamePlace(w, a) -> listOf(w, a).maxByOrNull { it.length } ?: w
            else -> "$w — $a"
        }
    }

    private fun nearlySamePlace(a: String, b: String): Boolean {
        val na = normalizePlace(a)
        val nb = normalizePlace(b)
        if (na.isEmpty() || nb.isEmpty()) return false
        return na == nb || na.contains(nb) || nb.contains(na)
    }

    private fun normalizePlace(s: String): String =
        s.lowercase(localeEs)
            .replace(Regex("^(la|el|los|las|de|del|en|al)\\s+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun Context.findActivity(): android.app.Activity? {
        var ctx: Context? = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
