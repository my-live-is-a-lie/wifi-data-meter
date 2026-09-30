# مقياس الواي فاي — Wi‑Fi Data Meter

تطبيق أندرويد لقياس وحساب استهلاك الإنترنت على شبكة الواي فاي، مكتوب بـ **Kotlin** و **Jetpack Compose**
بواجهة مبنية على **Material 3 Expressive** (الوضع التعبيري من Material 3).

> الاسم الظاهر للتطبيق: «مقياس الواي فاي» (العربية افتراضيًا) / «Wi‑Fi Data Meter» (English).

---

## المزايا

| الشاشة | ما تقدّمه |
|---|---|
| **لوحة المعلومات** | حلقة استهلاك متحرّكة تُظهر نسبة ما استُهلك من الباقة، المتبقّي، السرعة اللحظية، تفاصيل شبكة الواي فاي (الاسم، قوة الإشارة، سرعة الرابط، النطاق 2.4/5/6 جيجاهرتز، معيار الواي فاي، عنوان IP والبوابة)، إجماليات اليوم والجلسة، وأزرار سريعة (تحديث / تصفير الجلسة / ضبط الباقة). |
| **التطبيقات** | ترتيب التطبيقات حسب استهلاكها على الواي فاي (اليوم / الأسبوع / الشهر) مع أيقونة كل تطبيق ونسبة مساهمته، وإمكانية إظهار تطبيقات النظام. |
| **السجل** | مخطط أعمدة لاستهلاك آخر 7 أو 30 يومًا، مع تفاصيل اليوم عند اختياره وأكثر التطبيقات استهلاكًا فيه. |
| **الإعدادات** | حجم الباقة ويوم بدء الدورة، التنبيهات ونسبة التحذير، مُعدّل القياس، تشغيل/إيقاف المراقبة، الألوان الديناميكية، حالة صلاحية «الوصول إلى الاستخدام»، ومسح البيانات. |

بالإضافة إلى:

- **مراقبة في الخلفية** عبر خدمة أمامية (Foreground Service) بإشعار دائم يعرض استهلاك اليوم والسرعة والمتبقّي، مع زر إيقاف سريع من الإشعار.
- **تنبيهات ذكية** عند بلوغ نسبة التحذير (افتراضيًا 80%) وعند تجاوز الباقة، تُعاد ضبطها تلقائيًا مع بداية دورة فاتورة جديدة.
- **ألوان ديناميكية** (Android 12+) قابلة للإيقاف، وثيم فاتح/داكن، وواجهة عربية من اليمين إلى اليسار افتراضيًا.

---

## لقطات من التصميم التعبيري

يستخدم التطبيق مكوّنات Material 3 Expressive الحديثة:

- `MaterialExpressiveTheme` مع `MotionScheme.expressive()` و `expressiveLightColorScheme()`.
- `ButtonGroup` بمجموعات أزرار متّصلة لتغيير الفترات ومُعدّل القياس.
- `ToggleFloatingActionButton` + `FloatingActionButtonMenu` للإجراءات السريعة.
- `ShortNavigationBar` لتبويبات سفلية قصيرة، و `TopAppBar` بعنوان فرعي (subtitle).
- أنماط كتابة «مُشدَّدة» (`displaySmallEmphasized`، `titleMediumEmphasized` …) وأشكال `largeIncreased` / `extraLargeIncreased`.
- `LoadingIndicator` و `PullToRefreshBox` و `Slider` الحديث القائم على الحالة.

---

## التقنيات وإصداراتها

| المكوّن | الإصدار |
|---|---|
| Kotlin | 2.4.10 |
| Android Gradle Plugin | 9.3.1 |
| Gradle | 9.5.0 (Wrapper مُضمَّن) |
| Compose BOM | 2026.09.00 |
| Material 3 | 1.5.0-alpha29 (أول خط يحتوي Material 3 Expressive) |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |

> **ملاحظة عن Material 3 Expressive:** هذه الميزات متوفرة حاليًا في الإصدارات التجريبية من `androidx.compose.material3`
> (خط 1.5.0)، وليست بعد في نسخة مستقرة. لذلك قد تظهر تحذيرات `@ExperimentalMaterial3ExpressiveApi` عند البناء — وهي مقصودة.
> عند صدور النسخة المستقرة يكفي تغيير `material3` في `gradle/libs.versions.toml`.

---

## البناء والتشغيل

1. افتح مجلد المشروع في **Android Studio** (إصدار يدعم AGP 9.x) أو استخدم سطر الأوامر.
2. تأكد من تثبيت **Android SDK 37** و **JDK 17** أو أحدث (JDK المُضمَّن مع Android Studio كافٍ).
3. نفّذ:

```bash
./gradlew assembleDebug        # بناء نسخة التصحيح
./gradlew installDebug         # تثبيتها على جهاز/محاكي متصل
./gradlew test                 # تشغيل اختبارات الوحدة
```

4. عند أول تشغيل، امنح التطبيق صلاحية **«الوصول إلى الاستخدام»** من الزر الظاهر في التطبيق
   (أو من: الإعدادات ← التطبيقات ← وصول خاص ← الوصول إلى الاستخدام).
   بدونها تظهر تفاصيل الشبكة فقط، أما إجماليات الاستهلاك فتبقى صفرًا.

---

## الصلاحيات وسبب كل واحدة

| الصلاحية | السبب |
|---|---|
| `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE` | قراءة حالة الشبكة وحقول `WifiInfo` (سرعة الرابط، القناة، النطاق). |
| `ACCESS_FINE_LOCATION` (حتى API 32 فقط) | اسم الشبكة SSID على أندرويد 12 وما قبله (اشتراط نظام). |
| `NEARBY_WIFI_DEVICES` (API 33+) | اسم الشبكة SSID على أندرويد 13+ دون طلب الموقع. |
| `POST_NOTIFICATIONS` (API 33+) | إشعار المراقبة الدائم وتنبيهات الباقة. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | تشغيل المراقبة في الخلفية. |
| `PACKAGE_USAGE_STATS` | إحصاءات الاستهلاك من `NetworkStatsManager` (تُمنح يدويًا من إعدادات النظام). |
| `INTERNET` | تُعلَن لأن التطبيق يقرأ حالة الاتصال؛ **لا يُرسل التطبيق أي بيانات إلى أي جهة**. |

---

## الخصوصية

- كل العمليات تجري على الجهاز؛ لا حسابات ولا خدمات سحابية ولا تتبّع.
- تُحفظ الإعدادات فقط في `SharedPreferences` باسم `mywifi_settings`، وتُستثنى من النسخ الاحتياطي
  حسب قواعد `backup_rules.xml` / `data_extraction_rules.xml`.
- لا يُخزَّن أي سجل تصفّح أو محتوى؛ الأرقام تأتي مجمّعة من `NetworkStatsManager`.

---

## بنية المشروع

```
app/src/main/java/com/mywifiadata/app/
├── MyWifiApplication.kt          # تهيئة المستودع وقنوات الإشعارات
├── MainActivity.kt               # نقطة الدخول (Compose + edge-to-edge + RTL)
├── core/                         # Format (وحدات/تواريخ) و TimeRanges و SystemActions
├── data/                         # SettingsStore، NetworkStatsReader، WifiNetworkReader، UsageRepository
├── data/model/                   # نماذج البيانات (TrafficTotals، WifiStatus، AppUsage …)
├── monitor/                      # MonitorService (خدمة أمامية) و Notifier
└── ui/
    ├── AppViewModel.kt           # حالة الواجهة والمنطق العرضي
    ├── theme/                    # AppTheme، Shapes، Typography
    ├── components/               # UsageRing، DailyBarChart، StatTile، AppUsageRow …
    ├── screens/                  # Main / Dashboard / Apps / History / Settings
    └── PreviewData.kt            # معاينات Android Studio
app/src/test/java/…               # اختبارات الوحدة (Format، TimeRanges، النماذج)
```

---

## حدود القياس (مهم)

- الأرقام مأخوذة من `NetworkStatsManager` الخاص بالنظام، وهي نفس الأرقام التي يعرضها
  «استخدام البيانات» في إعدادات أندرويد، لذا قد تختلف قليلًا عن لوحة تحكّم الراوتر.
- استهلاك الأجهزة الأخرى على نفس الشبكة **لا يمكن قياسه** من الهاتف؛ التطبيق يقيس هذا الجهاز فقط.
- القياس لكل تطبيق يعتمد على تجميع حسب `uid` الذي يوفّره النظام، وبعض التطبيقات تتشارك `uid` واحدًا
  فتظهر مجمّعة.
- على أندرويد 15+ قد يوقف النظام خدمة `dataSync` بعد مدة؛ يوقف التطبيق المراقبة عندها ويُعلم المستخدم
  لإعادة تشغيلها من الإعدادات.

---

## English summary

**Wi‑Fi Data Meter** is an Android app that measures Wi‑Fi data usage per day, per billing cycle and per app,
built with **Kotlin + Jetpack Compose + Material 3 Expressive**.

- Dashboard: animated usage ring vs. your plan quota, live speed, full Wi‑Fi link details (SSID, RSSI, link
  speed, band, standard, IP/gateway/DNS), today and session totals.
- Apps: Wi‑Fi usage ranked per app for today / week / month.
- History: 7‑ or 30‑day bar chart with per‑day details and that day's top apps.
- Settings: quota + cycle start day, alert threshold, sampling interval, background tracking, dynamic color.
- Background monitoring through a `dataSync` foreground service with an ongoing notification and threshold alerts.

Build with `./gradlew assembleDebug` (AGP 9.3.1 · Kotlin 2.4.10 · Gradle 9.5.0 · Compose BOM 2026.09.00 ·
Material 3 1.5.0‑alpha29 · compileSdk 37 · minSdk 26). Grant **Usage Access** on first run to enable per‑app stats.
Everything stays on-device — no analytics, no network calls.
