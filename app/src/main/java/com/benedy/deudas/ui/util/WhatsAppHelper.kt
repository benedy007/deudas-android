package com.benedy.deudas.ui.util

import android.app.Activity
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
     * Shares a receipt image. Intentionally does NOT set EXTRA_TEXT —
     * WhatsApp often drops EXTRA_STREAM when a caption is also present.
     * Prefers Activity context (no NEW_TASK) when available.
     */
    fun shareImageToWhatsApp(
        context: Context,
        phone: String,
        imageUri: Uri,
        @Suppress("UNUSED_PARAMETER") caption: String? = null
    ) {
        // phone kept for API compatibility; image share opens WhatsApp picker
        shareImageOnly(context, imageUri, preferWhatsApp = true)
    }

    /**
     * System share sheet for the receipt image (no caption).
     */
    fun shareImageSystem(context: Context, imageUri: Uri) {
        shareImageOnly(context, imageUri, preferWhatsApp = false)
    }

    private fun shareImageOnly(
        context: Context,
        imageUri: Uri,
        preferWhatsApp: Boolean
    ) {
        val base = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            // Do NOT put EXTRA_TEXT — WhatsApp drops the stream when both are set
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

        if (preferWhatsApp) {
            for (pkg in WHATSAPP_PACKAGES) {
                if (!isPackageInstalled(context, pkg)) continue
                val targeted = Intent(base).setPackage(pkg)
                try {
                    if (canResolve(context, targeted)) {
                        startSafely(context, targeted)
                        return
                    }
                } catch (_: ActivityNotFoundException) {
                } catch (_: SecurityException) {
                } catch (_: Exception) {
                }
            }
        }

        val chooser = Intent.createChooser(Intent(base).setPackage(null), null).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = base.clipData
        }
        try {
            startSafely(context, chooser)
        } catch (e: Exception) {
            val loose = Intent(base).apply {
                type = "image/*"
                setPackage(null)
            }
            val looseChooser = Intent.createChooser(loose, null).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newUri(context.contentResolver, "recibo", imageUri)
            }
            startSafely(context, looseChooser)
        }
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
