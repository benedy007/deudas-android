package com.benedy.deudas.ui.util

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

object WhatsAppHelper {

    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun openChat(context: Context, phone: String, prefilledText: String? = null) {
        val digits = digitsOnly(phone)
        val uri = if (!prefilledText.isNullOrBlank()) {
            Uri.parse("https://wa.me/$digits?text=${Uri.encode(prefilledText)}")
        } else {
            Uri.parse("https://wa.me/$digits")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startSafely(context, intent)
    }

    fun shareTextToWhatsApp(context: Context, phone: String, text: String) {
        try {
            openChat(context, phone, text)
        } catch (_: Exception) {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            for (pkg in WHATSAPP_PACKAGES) {
                try {
                    startSafely(context, Intent(send).setPackage(pkg))
                    return
                } catch (_: Exception) {
                    // try next
                }
            }
            startSafely(context, Intent.createChooser(send, null))
        }
    }

    /**
     * Shares a receipt image via the system share sheet (same path that works when
     * the user picks WhatsApp). Does not rely on setPackage as the only path;
     * direct package targeting fails on many devices.
     *
     * Intentionally does not set EXTRA_TEXT: WhatsApp often drops EXTRA_STREAM
     * when a caption is also present.
     */
    @Suppress("UNUSED_PARAMETER")
    fun shareImageToWhatsApp(
        context: Context,
        phone: String,
        imageUri: Uri,
        caption: String? = null
    ) {
        // phone kept for API compatibility; image share opens system chooser
        shareImageViaChooser(context, imageUri)
    }

    /**
     * System share sheet for the receipt image (no caption).
     * Same implementation as shareImageToWhatsApp; both use the working chooser path.
     */
    fun shareImageSystem(context: Context, imageUri: Uri) {
        shareImageViaChooser(context, imageUri)
    }

    /**
     * ACTION_SEND with MIME image slash-star, MediaStore/FileProvider URI,
     * ClipData and FLAG_GRANT_READ_URI_PERMISSION, opened with Intent.createChooser.
     * Matches the path confirmed working on device.
     */
    private fun shareImageViaChooser(context: Context, imageUri: Uri) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            // Do NOT put EXTRA_TEXT: WhatsApp drops the stream when both are set
            clipData = ClipData.newUri(context.contentResolver, "recibo", imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        for (pkg in WHATSAPP_PACKAGES) {
            try {
                context.grantUriPermission(
                    pkg,
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // package may not be installed
            }
        }

        val chooser = Intent.createChooser(send, null).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = send.clipData
        }
        startSafely(context, chooser)
    }

    /**
     * Starts an intent using Activity context when possible.
     * Only adds FLAG_ACTIVITY_NEW_TASK when the context is not an Activity.
     */
    private fun startSafely(context: Context, intent: Intent) {
        val activity = context.findActivity()
        if (activity != null) {
            activity.startActivity(intent)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun Context.findActivity(): Activity? {
        var ctx: Context? = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun buildReceiptText(
        clientName: String,
        amount: Double,
        debtDescription: String,
        dateMs: Long,
        remaining: Double,
        companyName: String = "ContaFácil",
        companyPhone: String? = null,
        footerNote: String = "Gracias por su pago"
    ): String = buildString {
        val brand = companyName.ifBlank { "ContaFácil" }
        val note = footerNote.ifBlank { "Gracias por su pago" }
        appendLine("🧾 *Comprobante de pago*")
        appendLine("*$brand*")
        companyPhone?.trim()?.takeIf { it.isNotEmpty() }?.let { phone ->
            appendLine("Tel: $phone")
        }
        appendLine()
        appendLine("Cliente: $clientName")
        appendLine("Concepto: $debtDescription")
        appendLine("Monto pagado: ${formatMoney(amount)}")
        appendLine("Saldo restante: ${formatMoney(remaining)}")
        appendLine("Fecha: ${formatDate(dateMs)}")
        appendLine()
        append(note)
        if (!note.trim().endsWith(".")) {
            append(".")
        }
        appendLine()
        appendLine()
        append("— $brand")
    }
}
