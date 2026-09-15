package com.benedy.deudas.ui.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

object WhatsAppHelper {

    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

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
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            for (pkg in WHATSAPP_PACKAGES) {
                try {
                    context.startActivity(Intent(send).setPackage(pkg))
                    return
                } catch (_: Exception) {
                    // try next
                }
            }
            val chooser = Intent.createChooser(send, null)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    /**
     * Shares a receipt PNG via FileProvider.
     * Uses ClipData + FLAG_GRANT_READ_URI_PERMISSION (required on Android 7+),
     * grants URI to WhatsApp packages, and falls back to the system share sheet.
     */
    fun shareImageToWhatsApp(context: Context, phone: String, imageUri: Uri, caption: String? = null) {
        val base = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            clipData = ClipData.newUri(context.contentResolver, "recibo", imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (!caption.isNullOrBlank()) {
                putExtra(Intent.EXTRA_TEXT, caption)
            }
        }

        // Explicitly grant read permission to known WhatsApp packages
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

        // Prefer installed WhatsApp / Business if resolvable
        for (pkg in WHATSAPP_PACKAGES) {
            if (!isPackageInstalled(context, pkg)) continue
            val targeted = Intent(base).setPackage(pkg)
            try {
                if (canResolve(context, targeted)) {
                    context.startActivity(targeted)
                    return
                }
            } catch (_: ActivityNotFoundException) {
                // try next / fallback
            } catch (_: SecurityException) {
                // try next / fallback
            } catch (_: Exception) {
                // try next / fallback
            }
        }

        // System share sheet (ClipData propagates URI grants to chosen app)
        val chooser = Intent.createChooser(Intent(base).setPackage(null), null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Propagate ClipData for permission grants through the chooser
            clipData = base.clipData
            // Also grant to any EXTRA_INITIAL_INTENTS targets if present
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Last resort: image/* mime
            val loose = Intent(base).apply {
                type = "image/*"
                setPackage(null)
            }
            context.startActivity(
                Intent.createChooser(loose, null)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .also { it.clipData = ClipData.newUri(context.contentResolver, "recibo", imageUri) }
            )
        }
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun canResolve(context: Context, intent: Intent): Boolean {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= 33) {
            pm.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            ) != null
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
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
