package com.benedy.deudas.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val localeEs = Locale("es", "DO")
private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(localeEs)
private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", localeEs)

fun formatMoney(amount: Double): String = currencyFormat.format(amount)

fun formatDate(epochMs: Long): String = dateFormat.format(Date(epochMs))

fun digitsOnly(phone: String): String = phone.filter { it.isDigit() }
