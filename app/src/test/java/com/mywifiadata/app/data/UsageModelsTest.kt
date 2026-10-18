package com.mywifiadata.app.data

import com.mywifiadata.app.core.UsageNetwork
import com.mywifiadata.app.data.model.AdditionalPlanDraft
import com.mywifiadata.app.data.model.AppSettings
import com.mywifiadata.app.data.model.DataPlan
import com.mywifiadata.app.data.model.PlanCapacityUnit
import com.mywifiadata.app.data.model.PlanDraft
import com.mywifiadata.app.data.model.PlanDurationUnit
import com.mywifiadata.app.data.model.PlanNetworkType
import com.mywifiadata.app.data.model.TrafficTotals
import com.mywifiadata.app.data.model.WifiStatus
import com.mywifiadata.app.ui.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات النماذج والحسابات المشتقّة في حالة الواجهة.
 */
class UsageModelsTest {

    @Test
    fun `totals add up rx and tx`() {
        val first = TrafficTotals(rxBytes = 1_000, txBytes = 500)
        val second = TrafficTotals(rxBytes = 2_000, txBytes = 250)
        val sum = first + second

        assertEquals(3_000, sum.rxBytes)
        assertEquals(750, sum.txBytes)
        assertEquals(3_750, sum.totalBytes)
        assertEquals(0, TrafficTotals.Empty.totalBytes)
    }

    @Test
    fun `quota progress and remaining bytes are derived from the cycle totals`() {
        val state = UiState(
            now = System.currentTimeMillis(),
            settings = AppSettings(quotaBytes = 10_000_000_000, cycleStartDay = 1),
            cycle = TrafficTotals(rxBytes = 3_000_000_000, txBytes = 1_000_000_000),
        )

        assertEquals(0.4f, state.quotaProgress, 0.0001f)
        assertEquals(6_000_000_000L, state.remainingBytes)
        assertFalse(state.quotaExceeded)
        assertTrue(state.hasQuota)
    }

    @Test
    fun `exceeding the quota clamps the progress and flags it`() {
        val state = UiState(
            settings = AppSettings(quotaBytes = 1_000_000_000),
            cycle = TrafficTotals(rxBytes = 1_500_000_000, txBytes = 0),
        )

        assertEquals(1f, state.quotaProgress, 0.0001f)
        assertEquals(0L, state.remainingBytes)
        assertTrue(state.quotaExceeded)
    }

    @Test
    fun `without a quota the progress stays at zero`() {
        val state = UiState(cycle = TrafficTotals(rxBytes = 500_000, txBytes = 500_000))
        assertFalse(state.hasQuota)
        assertEquals(0f, state.quotaProgress, 0.0001f)
    }

    @Test
    fun `average per day and projection use the elapsed days of the cycle`() {
        val state = UiState(
            now = System.currentTimeMillis(),
            settings = AppSettings(quotaBytes = 50_000_000_000, cycleStartDay = 1),
            cycle = TrafficTotals(rxBytes = 3_000_000_000, txBytes = 0),
        )

        assertEquals(state.cycle.totalBytes / state.elapsedDays, state.averagePerDay)
        assertTrue(state.projectedBytes >= state.cycle.totalBytes)
        assertTrue(state.daysLeft in 0..31)
    }

    @Test
    fun `plan draft converts capacity units and pending or active plan days`() {
        val gbDraft = PlanDraft(capacityValue = 80.0, capacityUnit = PlanCapacityUnit.GB)
        val mbDraft = PlanDraft(capacityValue = 500.0, capacityUnit = PlanCapacityUnit.MB)
        val tbDraft = PlanDraft(capacityValue = 1.5, capacityUnit = PlanCapacityUnit.TB)

        assertEquals(80_000_000_000L, gbDraft.quotaBytes)
        assertEquals(500_000_000L, mbDraft.quotaBytes)
        assertEquals(1_500_000_000_000L, tbDraft.quotaBytes)

        val now = 1_700_000_000_000L
        val pendingState = UiState(
            now = now,
            settings = AppSettings(
                quotaBytes = 80_000_000_000L,
                planDurationValue = 28,
                planDurationUnit = PlanDurationUnit.DAY,
                planPendingActivation = true,
            ),
        )
        assertEquals(28, pendingState.cycleDays)
        assertEquals(28, pendingState.daysLeft)

        val activatedTwelveDaysAgo = now - 12L * 86_400_000L
        val activeState = UiState(
            now = now,
            settings = AppSettings(
                quotaBytes = 80_000_000_000L,
                planDurationValue = 28,
                planDurationUnit = PlanDurationUnit.DAY,
                planPendingActivation = false,
                planActivatedAtMillis = activatedTwelveDaysAgo,
            ),
        )
        assertEquals(28, activeState.cycleDays)
        assertEquals(16, activeState.daysLeft)
    }

    @Test
    fun `wifi and mobile plans track remaining bytes and days independently`() {
        val now = 1_700_000_000_000L
        val state = UiState(
            now = now,
            settings = AppSettings(
                planName = "فودافون كاش",
                quotaBytes = 80_000_000_000L,
                planDurationValue = 28,
                planDurationUnit = PlanDurationUnit.DAY,
                planPendingActivation = false,
                planActivatedAtMillis = now,
                mobilePlan = DataPlan(
                    name = "باقة الموبايل",
                    quotaBytes = 20_000_000_000L,
                    durationValue = 14,
                    durationUnit = PlanDurationUnit.DAY,
                    pendingActivation = false,
                    activatedAtMillis = now - 4L * 86_400_000L,
                ),
            ),
            cycle = TrafficTotals(rxBytes = 250_000_000L, txBytes = 0L),
            mobileCycle = TrafficTotals(rxBytes = 5_000_000_000L, txBytes = 0L),
        )

        assertEquals(79_750_000_000L, state.planRemainingBytes(UsageNetwork.WIFI))
        assertEquals(28, state.planDaysLeft(UsageNetwork.WIFI))
        assertEquals(28, state.planTotalDays(UsageNetwork.WIFI))

        assertEquals(15_000_000_000L, state.planRemainingBytes(UsageNetwork.MOBILE))
        assertEquals(10, state.planDaysLeft(UsageNetwork.MOBILE))
        assertEquals(14, state.planTotalDays(UsageNetwork.MOBILE))

        val wifiActiveState = state.copy(wifi = WifiStatus(isWifiConnected = true, isMobileConnected = false))
        assertEquals(UsageNetwork.WIFI, wifiActiveState.activePlanNetwork)
        assertEquals(80_000_000_000L, wifiActiveState.quotaBytes)
        assertEquals(79_750_000_000L, wifiActiveState.remainingBytes)
        assertEquals(28, wifiActiveState.daysLeft)

        val mobileActiveState = state.copy(wifi = WifiStatus(isWifiConnected = false, isMobileConnected = true))
        assertEquals(UsageNetwork.MOBILE, mobileActiveState.activePlanNetwork)
        assertEquals(20_000_000_000L, mobileActiveState.quotaBytes)
        assertEquals(15_000_000_000L, mobileActiveState.remainingBytes)
        assertEquals(10, mobileActiveState.daysLeft)

        val pendingWifiPlanState = state.copy(
            settings = state.settings.copy(planPendingActivation = true),
            cycle = TrafficTotals(rxBytes = 40_000_000_000L, txBytes = 0L),
            wifiPlanUsage = TrafficTotals.Empty,
        )
        assertEquals(80_000_000_000L, pendingWifiPlanState.remainingBytes)
        assertEquals(
            pendingWifiPlanState.cycle.totalBytes / pendingWifiPlanState.elapsedDays,
            pendingWifiPlanState.averagePerDay,
        )
    }

    @Test
    fun `additional plans support max 2 items reorder update delete and handover`() {
        val now = 1_700_000_000_000L
        var settings = AppSettings(
            planId = "main-wifi",
            planName = "فودافون كاش",
            quotaBytes = 80_000_000_000L,
            planCapacityUnit = PlanCapacityUnit.GB,
            planDurationValue = 28,
            planDurationUnit = PlanDurationUnit.DAY,
            planPendingActivation = false,
            planActivatedAtMillis = now - 5L * 86_400_000L,
        )

        settings = settings.addAdditionalPlan(
            network = UsageNetwork.WIFI,
            draft = AdditionalPlanDraft(
                name = "إضافية 1",
                capacityValue = 500.0,
                capacityUnit = PlanCapacityUnit.MB,
                durationValue = 7,
                durationUnit = PlanDurationUnit.DAY,
            ),
        )
        settings = settings.addAdditionalPlan(
            network = UsageNetwork.WIFI,
            draft = AdditionalPlanDraft(
                name = "إضافية 2",
                capacityValue = 1.0,
                capacityUnit = PlanCapacityUnit.GB,
                durationValue = 14,
                durationUnit = PlanDurationUnit.DAY,
                preventEdit = true,
            ),
        )
        // محاولة إضافة باقة ثالثة يجب تجاهلها (الحد الأقصى 2)
        settings = settings.addAdditionalPlan(
            network = UsageNetwork.WIFI,
            draft = AdditionalPlanDraft(
                name = "إضافية 3",
                capacityValue = 2.0,
                capacityUnit = PlanCapacityUnit.GB,
            ),
        )
        assertEquals(2, settings.planFor(UsageNetwork.WIFI).additionalPlans.size)
        assertEquals("إضافية 1", settings.planFor(UsageNetwork.WIFI).additionalPlans[0].name)
        assertEquals("إضافية 2", settings.planFor(UsageNetwork.WIFI).additionalPlans[1].name)

        // إعادة الترتيب بين الباقتين الإضافيتين
        settings = settings.reorderAdditionalPlans(UsageNetwork.WIFI, 0, 1)
        assertEquals("إضافية 2", settings.planFor(UsageNetwork.WIFI).additionalPlans[0].name)
        assertEquals("إضافية 1", settings.planFor(UsageNetwork.WIFI).additionalPlans[1].name)

        // عند تفعيل منع التعديل على الباقة الإضافية، يُسمح بتعديل الاسم فقط دون السعة أو المدة
        val lockedId = settings.planFor(UsageNetwork.WIFI).additionalPlans[0].id
        settings = settings.updateAdditionalPlan(
            network = UsageNetwork.WIFI,
            additionalPlanId = lockedId,
            draft = AdditionalPlanDraft(
                name = "إضافية 2 محدثة",
                capacityValue = 99.0,
                capacityUnit = PlanCapacityUnit.GB,
                durationValue = 30,
                durationUnit = PlanDurationUnit.DAY,
            ),
        )
        val updatedFirst = settings.planFor(UsageNetwork.WIFI).additionalPlans[0]
        assertEquals("إضافية 2 محدثة", updatedFirst.name)
        assertEquals(1_000_000_000L, updatedFirst.quotaBytes)
        assertEquals(14, updatedFirst.durationValue)

        // عند نفاد سعة الباقة الرئيسية تحل الباقة الإضافية الأولى مكانها بانتظار التفعيل
        // وتُحفظ الباقة السابقة في قائمة الباقات المحفوظة
        val promoted = settings.promoteExpiredPlansIfNeeded(
            wifiUsedBytes = 80_000_000_000L,
            mobileUsedBytes = 0L,
            now = now,
        )
        val newActive = promoted.planFor(UsageNetwork.WIFI)
        assertEquals("إضافية 2 محدثة", newActive.name)
        assertEquals(1_000_000_000L, newActive.quotaBytes)
        assertTrue(newActive.pendingActivation)
        assertEquals(1, newActive.additionalPlans.size)
        assertEquals("إضافية 1", newActive.additionalPlans[0].name)
        assertEquals(1, promoted.savedPlansFor(UsageNetwork.WIFI).size)
        assertEquals("فودافون كاش", promoted.savedPlansFor(UsageNetwork.WIFI)[0].name)
    }

    @Test
    fun `saved plans list supports switching with reset and storage codec roundtrip`() {
        val now = 1_700_000_000_000L
        var settings = AppSettings(
            planId = "wifi-1",
            planName = "باقة أساسية",
            quotaBytes = 100_000_000_000L,
            planPendingActivation = false,
            planActivatedAtMillis = now - 10L * 86_400_000L,
        )

        settings = settings.addOrSavePlanFromList(
            PlanDraft(
                name = "باقة احتياطية",
                capacityValue = 50.0,
                capacityUnit = PlanCapacityUnit.GB,
                durationValue = 30,
                durationUnit = PlanDurationUnit.DAY,
                networkType = PlanNetworkType.WIFI,
            ),
        )
        assertEquals(1, settings.savedPlansFor(UsageNetwork.WIFI).size)
        val savedPlanId = settings.savedPlansFor(UsageNetwork.WIFI)[0].id

        // التبديل إلى الباقة المحفوظة يفعّلها ويُعيد تعيين عداد الباقة السابقة ويحفظها في القائمة
        val switched = settings.switchToSavedPlan(UsageNetwork.WIFI, savedPlanId, now)
        assertEquals("باقة احتياطية", switched.planFor(UsageNetwork.WIFI).name)
        assertFalse(switched.planFor(UsageNetwork.WIFI).pendingActivation)
        assertEquals(now, switched.planFor(UsageNetwork.WIFI).activatedAtMillis)

        val previousInSaved = switched.savedPlansFor(UsageNetwork.WIFI).single()
        assertEquals("باقة أساسية", previousInSaved.name)
        assertTrue(previousInSaved.pendingActivation)
        assertEquals(0L, previousInSaved.activatedAtMillis)

        // اختبار ترميز وفك ترميز الباقات المحفوظة والباقات الإضافية
        val encodedSaved = PlanStorageCodec.encodeSavedPlans(switched.savedPlansFor(UsageNetwork.WIFI))
        val decodedSaved = PlanStorageCodec.decodeSavedPlans(encodedSaved)
        assertEquals(switched.savedPlansFor(UsageNetwork.WIFI), decodedSaved)
    }
}
