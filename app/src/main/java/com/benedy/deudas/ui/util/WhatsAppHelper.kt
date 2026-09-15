package com.benedy.deudas.ui.util

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
            context.startActivity(send)
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
    }
}
