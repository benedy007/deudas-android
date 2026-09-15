package com.benedy.deudas.data.settings

/**
 * Company branding used on receipts. Empty fields fall back to app defaults
 * ("Deudas", no phone, default thank-you footer).
 */
data class CompanySettings(
    val companyName: String = "",
    val companyPhone: String = "",
    val receiptFooter: String = ""
) {
    fun displayName(): String =
        companyName.trim().ifBlank { DEFAULT_COMPANY_NAME }

    fun displayPhone(): String? =
        companyPhone.trim().ifBlank { null }

    fun displayFooter(): String =
        receiptFooter.trim().ifBlank { DEFAULT_RECEIPT_FOOTER }

    companion object {
        const val DEFAULT_COMPANY_NAME = "Deudas"
        const val DEFAULT_RECEIPT_FOOTER = "Gracias por su pago"
    }
}
