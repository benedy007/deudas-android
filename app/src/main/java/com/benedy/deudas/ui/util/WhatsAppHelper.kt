package com.benedy.deudas.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object WhatsAppHelper {

    fun openChat(context: Context, phone: String, prefilledText: String? = null) {
        val digits = digitsOnly(phone)
        val uri = if (!prefilledText.isNullOrBlank()) {
            Uri.parse("https://wa.me/$digits?text=${Uri.encode(prefilledText)}")
        } else {
            Uri.parse("https://wa.me/$digits")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun shareTextToWhatsApp(context: Context, phone: String, text: String) {
        try {
            openChat(context, phone, text)
        } catch (_: Exception) {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(send)
            } catch (_: ActivityNotFoundException) {
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    },
                    null
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        }
    }

    /** Shares a receipt image (PNG via FileProvider). Targets WhatsApp when installed. */
    fun shareImageToWhatsApp(context: Context, phone: String, imageUri: Uri, caption: String? = null) {
        val digits = digitsOnly(phone)

        fun buildSend(packageName: String?): Intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                if (!caption.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_TEXT, caption)
                }
                if (digits.isNotEmpty()) {
                    // Undocumented but widely used: open specific WhatsApp chat
                    putExtra("jid", "$digits@s.whatsapp.net")
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (packageName != null) setPackage(packageName)
            }

        try {
            context.startActivity(buildSend("com.whatsapp"))
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(buildSend("com.whatsapp.w4b"))
            } catch (_: ActivityNotFoundException) {
                val chooser = Intent.createChooser(buildSend(null), null)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(chooser)
            }
        }
    }

    fun buildReceiptText(
        clientName: String,
        amount: Double,
        debtDescription: String,
        dateMs: Long,
        remaining: Double
    ): String = buildString {
        appendLine("🧾 *Comprobante de pago*")
        appendLine()
        appendLine("Cliente: $clientName")
        appendLine("Concepto: $debtDescription")
        appendLine("Monto pagado: ${formatMoney(amount)}")
        appendLine("Saldo restante: ${formatMoney(remaining)}")
        appendLine("Fecha: ${formatDate(dateMs)}")
        appendLine()
        append("Gracias por su pago.")
        appendLine()
        append("— Deudas")
    }
}
