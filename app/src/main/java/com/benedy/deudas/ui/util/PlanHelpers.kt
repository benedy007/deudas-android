package com.benedy.deudas.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.benedy.deudas.R
import com.benedy.deudas.data.local.entity.DebtEntity
import com.benedy.deudas.data.local.entity.PlanFrequency

/** Spanish label for a stored plan frequency (or null). */
@Composable
fun planFrequencyLabel(frequency: String?): String = when (frequency) {
    PlanFrequency.WEEKLY -> stringResource(R.string.plan_freq_weekly)
    PlanFrequency.BIWEEKLY -> stringResource(R.string.plan_freq_biweekly)
    PlanFrequency.MONTHLY -> stringResource(R.string.plan_freq_monthly)
    else -> stringResource(R.string.plan_freq_none)
}

/**
 * e.g. "Mensual · 30% · cuota RD$ X"
 * Returns null when the debt has no installment plan.
 */
@Composable
fun debtPlanLabel(debt: DebtEntity): String? {
    val cuota = debt.cuotaAmount() ?: return null
    val freq = debt.planFrequency ?: return null
    val pct = debt.planPercent ?: return null
    val pctText = if (pct == pct.toLong().toDouble()) {
        pct.toLong().toString()
    } else {
        pct.toString()
    }
    return stringResource(
        R.string.debt_plan_label,
        planFrequencyLabel(freq),
        pctText,
        formatMoney(cuota)
    )
}

/** Suggested default percent when picking a frequency. */
fun suggestedPlanPercent(frequency: String?): Double? = when (frequency) {
    PlanFrequency.MONTHLY -> 30.0
    PlanFrequency.BIWEEKLY -> 25.0
    PlanFrequency.WEEKLY -> 10.0
    else -> null
}
