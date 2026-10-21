package com.mywifiadata.app.core

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Per-app language preference and localized context support for Android versions before 13. */
object AppLanguage {
    const val SYSTEM = "system"
    const val ARABIC = "ar"
    const val ENGLISH = "en"

    const val PREFERENCES_NAME = "mywifi_settings"
    const val PREFERENCE_KEY = "app_language"

    fun normalize(languageTag: String?): String = when (languageTag?.lowercase(Locale.ROOT)) {
        ARABIC -> ARABIC
        ENGLISH -> ENGLISH
        else -> SYSTEM
    }

    /**
     * Reads the persisted choice early enough for Context resources to use the selected strings.
     *
     * التهيئة المُجاوَزة تُبنى **فارغة** ثم تُضبط اللغة واتجاهها فيها فقط — لا تُنسخ
     * تهيئة الجهاز كاملةً. السبب: عند كل تغيّر تهيئة يبني `ResourcesManager` التهيئة
     * الجديدة هكذا: `tmpConfig.setTo(globalConfig)` ثم
     * `tmpConfig.updateFrom(mOverrideConfiguration)` — أي أن التهيئة المُجاوَزة تعلو
     * على التهيئة الحيّة. نسخ التهيئة كاملةً يُثبّت معها `windowConfiguration`
     * (حدود النافذة والدوران) على قيمتها لحظة إنشاء المكوّن، فتصير الخدمة عمياء عن
     * تدوير الشاشة: تبقى النافذة العائمة عند إحداثيات الوضع القديم فتظهر في منتصف
     * الشاشة الأفقية بدل الحافة. الحقول غير المحدّدة في تهيئة فارغة يتجاهلها
     * `updateFrom`، فتُترك للتحديث الحيّ.
     */
    fun localizedContext(base: Context): Context {
        val language = normalize(
            base.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(PREFERENCE_KEY, SYSTEM),
        )
        if (language == SYSTEM) {
            Locale.setDefault(base.resources.configuration.locales[0])
            return base
        }

        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        // تهيئة فارغة = «تجاوز اللغة فقط»؛ بقية الحقول تأتي من التهيئة الحيّة.
        val configuration = Configuration().apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(configuration)
    }
}
