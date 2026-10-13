# مقياس الواي فاي — Wi‑Fi Data Meter

تطبيق أندرويد لقياس وحساب استهلاك الإنترنت على شبكة الواي فاي، مكتوب بـ **Kotlin** و **Jetpack Compose**
بواجهة مبنية على **Material 3 Expressive** (الوضع التعبيري من Material 3).

> الاسم الظاهر للتطبيق: «مقياس الواي فاي» (العربية افتراضيًا) / «Wi‑Fi Data Meter» (English).

---

## المزايا

| الشاشة | ما تقدّمه |
|---|---|
| **لوحة المعلومات** | حلقة استهلاك متحرّكة تُظهر نسبة ما استُهلك من الباقة، وبطاقة «اليوم» بالتخطيط المرجعي: بلاطتان كبيرتان للتنزيل (أزرق) والرفع (بنفسجي)، صف الإجمالي مع إجمالي الدورة، ولوحة اختبار السرعة عبر fast.com. إضافةً إلى تفاصيل شبكة الواي فاي (الاسم، قوة الإشارة، سرعة الرابط، النطاق 2.4/5/6 جيجاهرتز، معيار الواي فاي، عنوان IP والبوابة)، إجماليات الجلسة، وأزرار سريعة (تحديث / تصفير الجلسة / ضبط الباقة). |
| **التطبيقات** | ترتيب التطبيقات حسب استهلاكها على الواي فاي (اليوم / الأسبوع / الشهر) مع أيقونة كل تطبيق ونسبة مساهمته، وإمكانية إظهار تطبيقات النظام. |
| **السجل** | مخطط أعمدة لاستهلاك آخر 7 أو 30 يومًا، مع تفاصيل اليوم عند اختياره وأكثر التطبيقات استهلاكًا فيه. |
| **الإعدادات** | حجم الباقة ويوم بدء الدورة، التنبيهات ونسبة التحذير، مُعدّل القياس، تشغيل/إيقاف المراقبة، الألوان الديناميكية، حالة صلاحية «الوصول إلى الاستخدام»، ومسح البيانات. |

### بطاقة «اليوم» واختبار السرعة عبر fast.com

تتطابق بطاقة اليوم مع التصميم المرجعي المطلوب:

- **بلاطتان كبيرتان** جنبًا إلى جنب: التنزيل بالأزرق والرفع بالبنفسجي، القيمة بالوحدات
  العشرية كما تكتبها شركات الاتصال، وتحتها التسمية مع سهم الاتجاه.
- **صف الإجمالي**: إجمالي اليوم مع أيقونة دورية، وإلى جانبه «الإجمالي» لإجمالي دورة الفاتورة
  بالجيجابايت.
- **لوحة اختبار السرعة**: زر تشغيل أزرق في البداية (يمينًا)، العنوان «اختبار السرعة»
  وسطر «يستخدم موقع fast.com»، ثم نتائج التنزيل والرفع في الطرف الآخر. على الشاشات
  الضيقة تُنقل النتائج إلى سطر ثانٍ تلقائيًا حتى لا يُقتطع أي نص.

**اختبار السرعة (fast.com):**

- القياس يجري على خوادم fast.com الحقيقية (Netflix Open Connect) عبر عدة تدفقات متوازية،
  ومع حجب `api.fast.com` (رمز 403 في بعض الدول) ينتقل التطبيق تلقائيًا إلى خوادم Cloudflare.
- **تُحفظ النتيجة محليًا** (تنزيل/رفع/الوقت/المصدر) فتبقى ظاهرة في اللوحة حتى بعد إغلاق
  التطبيق وإعادة تشغيله، مع طابع وقت آخر اختبار.
- أثناء القياس تظهر المرحلة الجارية والسرعة اللحظية، وزر التشغيل يتحول إلى إيقاف.

**رسائل الانقطاع والمهلة وإغلاق التحميل:**

| الحالة | ما يحدث |
|---|---|
| لا اتصال قبل البدء | تظهر رسالة فورية ولا يبدأ أي تحميل. |
| انقطاع الاتصال أثناء الاختبار | يرصد المرصد الشبكة كل نصف ثانية، فيُغلق التحميل (تُفصل كل الاتصالات) وتظهر رسالة «انقطع الاتصال أثناء الاختبار». |
| توقّف وصول البيانات من الخادم | إن لم تصل أي بيانات لمدة 12 ثانية يُغلق التحميل برسالة «توقّف وصول البيانات». |
| مرور وقت طويل دون نتيجة | سقف زمني كامل للاختبار (45 ثانية) يُغلق التحميل بعده برسالة «مرّ وقت طويل دون الحصول على أي نتيجة». |
| ضغط زر الإيقاف | يُلغى الـCoroutine وتُفصل الاتصالات فورًا، وتعود آخر نتيجة محفوظة. |

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

## البناء التلقائي على GitHub Actions

يوجد Workflow جاهز في `.github/workflows/android-build.yml` يقوم بـ:

1. تجهيز Java 25 (Temurin الموجود مسبقًا في الـ tool cache للـ Runner، بلا أي تنزيل) + كاش Gradle بنظام
   **Enhanced Caching** (كاش مُجزّأ + إزالة تكرار + restore keys) مع توثيق `gradle-wrapper.jar`.
2. بناء **APK إصدار (Release)**: `:app:assembleRelease` — مُصغَّر بـ R8 ومقلَّص الموارد —
   **بمعمارية `arm64-v8a` فقط** (اسم الملف: `app-arm64-v8a-release.apk`) + اختبارات الوحدة
   `:app:testReleaseUnitTest` في **أمر Gradle واحد**.
3. توقيع الـ APK بمفتاح الإصدار الموجود في `release-keystore.p12` (بياناته في `keystore.properties`)،
   فيكون جاهزًا للتثبيت مباشرة على الجهاز.
4. رفعه كـ Artifact باسم `app-release-apk` من صفحة التشغيل (وملف `build.log` وتقارير الاختبار عند الفشل).
5. مهمة ثانية تُرفق نفس الملف بـ **GitHub Release** عند إنشاء وسم `v*`.

**التشغيل:** يُبنى المشروع تلقائيًا عند الدفع إلى `main` أو إلى أي فرع يبدأ بـ `arena/`، وعند أي Pull Request نحو `main`،
ويمكن تشغيله يدويًا من تبويب **Actions → Android Build → Run workflow** (مع اختيار `release` أو `debug` وتشغيل الاختبارات من عدمه).

**للتسريع** (كل بند مطبَّق فعليًا):

| الإجراء | الأثر |
|---|---|
| `org.gradle.caching` + `org.gradle.parallel` + `org.gradle.configuration-cache` في `gradle.properties` | إعادة استخدام مخرجات المهام، تشغيل متوازٍ، وتخطّي مرحلة الإعداد |
| كاش **configuration cache** عبر `actions/cache` (بدون أي أسرار) | اختصار فحص التكوين وبناء شجرة المهام في كل تشغيل |
| Enhanced Caching لـ Gradle User Home (اعتماديات + transforms + مخزن البناء) | لا إعادة تنزيل للاعتماديات |
| هدف واحد فقط `assembleRelease` بدل `build` | لا `check` ولا `lint` ولا اختبارات إضافية غير مطلوبة |
| `lint { checkReleaseBuilds = false }` | تعطيل `lintVitalRelease` الذي يعمل تلقائيًا مع تجميع الإصدار |
| `testReleaseUnitTest` بدل `testDebugUnitTest` | لا تُصرَّف نسخة debug كاملة إضافية |
| `org.gradle.jvmargs=-Xmx4g` و `kotlin.daemon.jvmargs=-Xmx2g` | ذاكرة كافية للـ Runner (16GB) فتقل عمليات GC البطيئة |
| `org.gradle.vfs.watch=false` | إيقاف مراقبة الملفات بلا فائدة في CI |
| بلا تنزيل JDK وبلا تثبيت Android SDK | الصورة تأتي بهما جاهزين |
| `concurrency` + `paths-ignore` + `compression-level: 0` | إلغاء التشغيلات القديمة، تخطي تغييرات التوثيق، ورفع أسرع للـ APK |

أول تشغيل بعد أي تغيير جذري (أو أول مرة على مرجع جديد) يكون باردًا فيستغرق دقائق لتنزيل الاعتماديات،
أما التشغيلات التالية على نفس المرجع فتكون أسرع بكثير.

**وضع البناء السريع:** إن أردت ملفًا بسرعة (بدون R8 وتقليص الموارد) شغّل الـ Workflow يدويًا مع خيار
`fast_build` ثم اضغط Run workflow، أو محليًا:

```bash
./gradlew :app:assembleRelease -Pmywifi.minify=false
```

المقايضة: زمن البناء ينخفض كثيرًا (R8 وحده يستغرق نحو ١٤٥ ثانية)، لكن حجم الـ APK يكبر.

**عن التوقيع:** الملفان `release-keystore.p12` و `keystore.properties` مُضمَّنان في المستودع (مستودع خاص) ليعمل
البناء والتحديث بنفس المفتاح في كل مرة. لترقيتهما لاحقًا إلى أسرار: أضف `RELEASE_STORE_PASSWORD` و
`RELEASE_KEY_ALIAS` و `RELEASE_KEY_PASSWORD` كـ Secrets ثم احذف `keystore.properties` — فملف
`app/build.gradle.kts` يقرأ من متغيرات البيئة أولًا.

> ملاحظة: إن لم تتوفر بيانات التوقيع يُبنى الـ APK **غير موقّع** ويستمر البناء بلا فشل.

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
│                                 # و SpeedTest + SpeedTestWatchdog (قياس fast.com والمهلات)
├── data/                         # SettingsStore، NetworkStatsReader، WifiNetworkReader، UsageRepository
├── data/model/                   # نماذج البيانات (TrafficTotals، WifiStatus، AppUsage …)
├── monitor/                      # MonitorService (خدمة أمامية) و Notifier
│                                 # و MonitoringTileService (بلاطة الإعدادات السريعة)
│                                 # و TileIconFrames / TileIconPainter (تحريك أيقونة البلاطة)
└── ui/
    ├── AppViewModel.kt           # حالة الواجهة والمنطق العرضي
    ├── theme/                    # AppTheme، Shapes، Typography
    ├── components/               # UsageRing، DailyBarChart، StatTile، TrafficTile،
    │                             # TodayTotalRow، SpeedTestPanel، AppUsageRow …
    ├── screens/                  # Main / Dashboard / Apps / History / Settings
    └── PreviewData.kt            # معاينات Android Studio
app/src/test/java/…               # اختبارات الوحدة (Format، TimeRanges، النماذج، SpeedTestWatchdog،
                                  # وإطارات أيقونة البلاطة)
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
- A reference-matching “Today” card (blue download / purple upload tiles, total row, fast.com speed-test panel) and a
  fast.com speed test that saves its last result on-device and aborts the transfer with a clear message when the
  connection drops or no data arrives for too long (12 s stall window, 45 s overall cap).

Build with `./gradlew assembleDebug` (AGP 9.3.1 · Kotlin 2.4.10 · Gradle 9.5.0 · Compose BOM 2026.09.00 ·
Material 3 1.5.0‑alpha29 · compileSdk 37 · minSdk 26). Grant **Usage Access** on first run to enable per‑app stats.
Everything stays on-device — no analytics, no network calls.
