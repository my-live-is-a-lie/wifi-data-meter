package com.mywifiadata.app.data

import com.mywifiadata.app.data.model.AdditionalPlan
import com.mywifiadata.app.data.model.CompletedPlanRecord
import com.mywifiadata.app.data.model.DataPlan
import com.mywifiadata.app.data.model.MAX_ADDITIONAL_PLANS
import com.mywifiadata.app.data.model.PlanCapacityUnit
import com.mywifiadata.app.data.model.PlanDurationUnit
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * ترميز وفك ترميز الباقات الإضافية وقوائم الباقات المحفوظة دون الاعتماد على مكتبات خارجية.
 */
internal object PlanStorageCodec {

    fun encodeAdditionalPlans(plans: List<AdditionalPlan>): String =
        plans.take(MAX_ADDITIONAL_PLANS).joinToString(";") { plan ->
            listOf(
                encodeText(plan.id),
                encodeText(plan.name),
                encodeText(plan.logoPath),
                plan.quotaBytes.toString(),
                plan.capacityUnit.name,
                plan.durationValue.toString(),
                plan.durationUnit.name,
                if (plan.preventEdit) "1" else "0",
            ).joinToString(",")
        }

    fun decodeAdditionalPlans(encoded: String?): List<AdditionalPlan> {
        if (encoded.isNullOrBlank()) return emptyList()
        return encoded.split(';')
            .mapNotNull(::decodeAdditionalPlan)
            .take(MAX_ADDITIONAL_PLANS)
    }

    private fun decodeAdditionalPlan(record: String): AdditionalPlan? {
        val parts = record.split(',')
        if (parts.size < 8) return null
        val id = decodeText(parts[0]).ifBlank { return null }
        val name = decodeText(parts[1])
        val logoPath = decodeText(parts[2])
        val quotaBytes = parts[3].toLongOrNull() ?: return null
        val capacityUnit = PlanCapacityUnit.entries.firstOrNull { it.name == parts[4] } ?: PlanCapacityUnit.MB
        val durationValue = parts[5].toIntOrNull()?.coerceAtLeast(1) ?: return null
        val durationUnit = PlanDurationUnit.entries.firstOrNull { it.name == parts[6] } ?: PlanDurationUnit.DAY
        val preventEdit = parts[7] == "1"
        return AdditionalPlan(
            id = id,
            name = name,
            logoPath = logoPath,
            quotaBytes = quotaBytes,
            capacityUnit = capacityUnit,
            durationValue = durationValue,
            durationUnit = durationUnit,
            preventEdit = preventEdit,
        )
    }

    fun encodeSavedPlans(plans: List<DataPlan>): String =
        plans.joinToString("\n") { plan ->
            listOf(
                encodeText(plan.id),
                encodeText(plan.name),
                encodeText(plan.logoPath),
                plan.quotaBytes.toString(),
                plan.capacityUnit.name,
                plan.durationValue.toString(),
                plan.durationUnit.name,
                if (plan.preventEdit) "1" else "0",
                if (plan.pendingActivation) "1" else "0",
                plan.activatedAtMillis.toString(),
                encodeText(encodeAdditionalPlans(plan.additionalPlans)),
            ).joinToString("|")
        }

    fun decodeSavedPlans(encoded: String?): List<DataPlan> {
        if (encoded.isNullOrBlank()) return emptyList()
        return encoded.split('\n').mapNotNull(::decodeSavedPlan)
    }

    private fun decodeSavedPlan(line: String): DataPlan? {
        val parts = line.split('|')
        if (parts.size < 11) return null
        val id = decodeText(parts[0]).ifBlank { return null }
        val name = decodeText(parts[1])
        val logoPath = decodeText(parts[2])
        val quotaBytes = parts[3].toLongOrNull() ?: return null
        val capacityUnit = PlanCapacityUnit.entries.firstOrNull { it.name == parts[4] } ?: PlanCapacityUnit.GB
        val durationValue = parts[5].toIntOrNull()?.coerceAtLeast(1) ?: return null
        val durationUnit = PlanDurationUnit.entries.firstOrNull { it.name == parts[6] } ?: PlanDurationUnit.DAY
        val preventEdit = parts[7] == "1"
        val pendingActivation = parts[8] == "1"
        val activatedAtMillis = parts[9].toLongOrNull() ?: 0L
        val additionalPlans = decodeAdditionalPlans(decodeText(parts[10]))
        return DataPlan(
            id = id,
            name = name,
            logoPath = logoPath,
            quotaBytes = quotaBytes,
            capacityUnit = capacityUnit,
            durationValue = durationValue,
            durationUnit = durationUnit,
            preventEdit = preventEdit,
            pendingActivation = pendingActivation,
            activatedAtMillis = activatedAtMillis,
            additionalPlans = additionalPlans,
        )
    }

    fun encodeCompletedPlans(records: List<CompletedPlanRecord>): String =
        records.joinToString("\n") { record ->
            listOf(
                encodeText(record.id),
                encodeText(record.planId),
                encodeText(record.name),
                encodeText(record.logoPath),
                record.quotaBytes.toString(),
                record.capacityUnit.name,
                record.durationValue.toString(),
                record.durationUnit.name,
                record.startMillis.toString(),
                record.endMillis.toString(),
                record.usedBytes.toString(),
            ).joinToString("|")
        }

    fun decodeCompletedPlans(encoded: String?): List<CompletedPlanRecord> {
        if (encoded.isNullOrBlank()) return emptyList()
        return encoded.split('\n').mapNotNull(::decodeCompletedPlan)
    }

    private fun decodeCompletedPlan(line: String): CompletedPlanRecord? {
        val parts = line.split('|')
        if (parts.size < 11) return null
        val id = decodeText(parts[0]).ifBlank { return null }
        val planId = decodeText(parts[1])
        val name = decodeText(parts[2])
        val logoPath = decodeText(parts[3])
        val quotaBytes = parts[4].toLongOrNull() ?: return null
        val capacityUnit = PlanCapacityUnit.entries.firstOrNull { it.name == parts[5] } ?: PlanCapacityUnit.GB
        val durationValue = parts[6].toIntOrNull()?.coerceAtLeast(1) ?: return null
        val durationUnit = PlanDurationUnit.entries.firstOrNull { it.name == parts[7] } ?: PlanDurationUnit.DAY
        val startMillis = parts[8].toLongOrNull() ?: return null
        val endMillis = parts[9].toLongOrNull() ?: return null
        val usedBytes = parts[10].toLongOrNull() ?: 0L
        return CompletedPlanRecord(
            id = id,
            planId = planId,
            name = name,
            logoPath = logoPath,
            quotaBytes = quotaBytes,
            capacityUnit = capacityUnit,
            durationValue = durationValue,
            durationUnit = durationUnit,
            startMillis = startMillis,
            endMillis = endMillis,
            usedBytes = usedBytes,
        )
    }

    private fun encodeText(text: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(encoded: String): String = runCatching {
        String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
    }.getOrDefault("")
}
