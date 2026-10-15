# مقياس الواي فاي — Wi‑Fi Data Meter

تطبيق أندرويد لقياس وحساب استهلاك الإنترنت على شبكة الواي فاي، مكتوب بـ **Kotlin** و **Jetpack Compose**
بواجهة مبنية على **Material 3 Expressive** (الوضع التعبيري من Material 3).

> الاسم الظاهر للتطبيق: «مقياس الواي فاي» (العربية افتراضيًا) / «Wi‑Fi Data Meter» (English).

---

## المزايا

| الشاشة | ما تقدّمه |
|---|---|
| **لوحة المعلومات** | حلقة استهلاك متحرّكة تُظهر نسبة ما استُهلك من الباقة، وبطاقة «اليوم» بالتخطيط المرجعي: بلاطتان كبيرتان للتنزيل (أزرق) والرفع (بنفسجي)، صف الإجمالي مع إجمالي الدورة، ولوحة اختبار السرعة عبر fast.com. إضافةً إلى تفاصيل شبكة الواي فاي (الاسم، قوة الإشارة، سرعة الرابط، النطاق 2.4/5/6 جيجاهرتز، معيار الواي فاي، عنوان IP والبوابة)، إجماليات الجلسة، وأزرار سريعة (تحديث / تصفير الجلسة / ضبط الباقة). |
| **التطبيقات** | قائمة فلاتر قابلة للإظهار والإخفاء لاختيار Wi-Fi أو بيانات الهاتف، وكل الوقت أو الباقة الحالية للواي فاي، وترتيب التطبيقات من الأعلى أو الأقل استهلاكًا، وتحديد العدد: الكل أو 5 / 10 / 15 / 20 / 25 تطبيقًا. مع تفصيل زمني (اليوم / الأسبوع / الشهر) وفلتر تطبيقات النظام. |
| **السجل** | مخطط أعمدة لاستهلاك آخر 7 أو 30 يومًا، مع تفاصيل اليوم عند اختياره وأكثر التطبيقات استهلاكًا فيه. |
| **الإعدادات** | حجم الباقة ويوم بدء الدورة، التنبيهات ونسبة التحذير، مُعدّل القياس، تشغيل/إيقاف المراقبة، الألوان الديناميكية، حالة صلاحية «الوصول إلى الاستخدام»، ومسح البيانات. |

### بطاقة «اليوم» واختبار السرعة عبر fast.com

تتطابق بطاقة اليوم مع التصميم المرجعي المطلوب:

- **بلاطتان كبيرتان** جنبًا إلى جنب: التنزيل بالأزرق والرفع بالبنفسجي، القيمة بالوحدات
  العشرية كما تكتبها شركات الاتصال، وتحتها التسمية مع سهم الاتجاه.
- **صف الإجمالي**: إجمالي اليوم مع أيقونة دورية، وإلى جانبه «الإجمالي» لإجمالي دورة الفاتورة
  بالجيجابايت.
- **لوحة اختبار السرعة**: زر التشغيل والعنوان في الأعلى، ثم نتائج التنزيل والرفع
  بجانب وقت آخر اختبار كما في التصميم المرجعي. السهم في منتصف أسفل البطاقة يفتح
  سجلًّا يضم أحدث تسع نتائج سابقة، مع تاريخ ووقت كل اختبار والوقت المنقضي وسرعتي
  التنزيل والرفع؛ وتبقى بطاقة الجلسة أسفلها عند توسيع السجل.

**اختبار السرعة (fast.com):**

- القياس يجري على خوادم fast.com الحقيقية (Netflix Open Connect) عبر عدة تدفقات متوازية،
  ومع حجب `api.fast.com` (رمز 403 في بعض الدول) ينتقل التطبيق تلقائيًا إلى خوادم Cloudflare.
- **تُحفظ آخر عشر نتائج محليًا** (تنزيل/رفع/الوقت/المصدر) لتبقى متاحة بعد إغلاق
  التطبيق وإعادة تشغيله؛ تظهر الأحدث في الملخص والتسع الأسبق في السجل القابل للتوسيع.
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

- **مراقبة في الخلفية** عبر خدمة أمامية بإشعار دائم يعرض استهلاك اليوم والمتبقّي؛ وأيقونته تعرض نسبة استهلاك الباقة داخل حلقة تقدم، أو علامة الواي فاي إن لم تُضبط باقة، مع زر إيقاف سريع.
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
- Apps: collapsible dropdown filters for Wi-Fi/mobile data, all-time/current Wi-Fi plan, highest/lowest usage ordering, and a separate all/5/10/15/20/25 app count. Day/week/month charts support time-slice drill-down. The day/week/month buttons control the period summary and app list. The all-time scope adds no billing-cycle restriction to that selected period; the current plan narrows it to the configured cycle start day. The count limits rows after sorting, not summary totals or the highest-usage app in the summary. Pull down to refresh.
- History: 7‑ or 30‑day bar chart with per‑day details and that day's top apps.
- Settings: quota + cycle start day, alert threshold, sampling interval, background tracking, dynamic color.
- Background monitoring through a `dataSync` foreground service with an ongoing notification and threshold alerts.
- A reference-matching “Today” card (blue download / purple upload tiles, total row, fast.com speed-test panel) with
  a collapsible history of the nine preceding tests; the latest ten results are saved on-device. The speed test
  also aborts its transfer with a clear message when the connection drops or no data arrives for too long (12 s
  stall window, 45 s overall cap).

Build with `./gradlew assembleDebug` (AGP 9.3.1 · Kotlin 2.4.10 · Gradle 9.5.0 · Compose BOM 2026.09.00 ·
Material 3 1.5.0‑alpha29 · compileSdk 37 · minSdk 26). Grant **Usage Access** on first run to enable per‑app stats.
Everything stays on-device — no analytics, no network calls.


### البحث في التطبيقات
- زر البحث بجوار زر عكس الترتيب يفتح حقلًا يتمدّد إلى اليمين في الواجهة العربية، ويعرض نتائج البحث في قائمة مستقلة ذات شريط بحث مثبّت أعلى منطقة المحتوى، مع الاحتفاظ بموضع القائمة الأصلية.
- البحث بالاسم أو اسم الحزمة داخل الفترة ونوع الشبكة وفلتر التطبيقات المختارة؛ يُطبّق العدد بعد البحث وليس قبله.
- عندما يكون الحقل فارغًا تظهر التطبيقات المختارة سابقًا من البحث أولًا، حسب عدد مرات الاختيار ثم الأحدث عند التساوي، وتحتها بقية التطبيقات دون تكرار وبترتيب الاستهلاك المختار. إذا لم توجد اختيارات سابقة تظهر القائمة كاملة حسب الفلاتر وحد العدد الحالي. بعد الكتابة يشمل البحث كل التطبيقات المطابقة للفلاتر الحالية. تُحفظ آخر 100 هوية تطبيق مختارة محليًا، ولا يُحفظ نص البحث.
- حقل البحث بارتفاع 48dp وحواف مستديرة، وزره الداخلي يمسح النص إذا وُجد، وإلا يغلق الحقل بحركة انكماش إلى أيقونة البحث. يبقى النص الإرشادي مخفيًا طوال فتح الحقل وحركة إغلاقه.
- إغلاق البحث يعيد ترتيب الاستهلاك المحدد وموضع الصفحة السابق (العنصر وإزاحة التمرير)، دون إعادة التمرير المتحركة عند كل حرف. مسح بيانات التطبيق من الإعدادات يمسح سجل الاختيارات أيضًا.
- تُستهلك حشوات Scaffold قبل تطبيق حشوة لوحة المفاتيح، لتجنب حجز مساحة شريط التنقل مرتين وترك مستطيل فارغ فوق لوحة المفاتيح.

Manual UI checks: open Apps in Arabic, verify period buttons precede the summary, then open search beside the sort icon. Confirm rightward expansion, keyboard focus and scrolling to the pinned toolbar; try a name, a package, and no matches; clear/close search. Open a result, return, repeat selections and restart to check frequency/recency ordering. Repeat with top-5, mobile data, current plan, rotation, large text and English layout.

Search-field regression checks: on a device/emulator with gesture navigation and again with three-button navigation, open the keyboard and verify app rows reach its top without an extra bottom-bar-sized gap. Type a query and tap the in-field X: only the query should clear and the keyboard should stay open. Tap X again: the empty pill should retract toward the search icon, the keyboard should dismiss, and only then should the list hint return. Repeat rapid open/close, system Back, no results, large fonts and RTL/LTR layouts.

Search-position regression checks: note the normal list position with the controls partly visible, open search with zero history, type a query, clear it, and close using both X and system Back. The normal index/offset and filter panel visibility should return. Repeat with a populated history, one/no/many matches, rapid typing/deletion, keyboard hide/show, opening a result and returning, and rotation. No ranking hint or height-based spacer should appear in search mode, and editing should never animate the list down/up.

Pull-to-refresh/search regression checks: partially pull the apps list, then open search. While search is open, no refresh gesture handler or indicator is attached, even if a refresh was already running. Type/delete repeatedly (including no matches), move the cursor, change keyboard language and hide/show the keyboard: no refresh indicator should appear or move. Close search and check that normal pull-to-refresh works from a fresh zero-distance state. Other tabs and app details retain their normal refresh behavior. The search field stays in the same composition when its refresh policy changes.

### أسماء وأيقونات التطبيقات المحذوفة
- يُحفظ اسم الحزمة واسم التطبيق ومعرّف UID وحالته، مع أيقونة PNG بحجم 96×96، في مجلد خاص دائم داخل `noBackupFilesDir/app-identities`؛ لا يُستخدم مجلد الكاش القابل للإخلاء ولا تُرفع هذه الملفات إلى الشبكة أو نسخة احتياطية لجهاز آخر.
- تُلتقط التطبيقات التي يتيح أندرويد رؤيتها عند التشغيل، وتُحدّث عند قراءة الاستهلاك وأحداث تثبيت/تحديث/حذف الحزم أثناء عمل العملية، دون طلب صلاحية الوصول لكل الحزم.
- لا يقتصر الحفظ على الاسم والأيقونة: تُحفظ القراءات المرصودة محليًا لكل شبكة في `app-usage-archive`. تُلتقط الأيام الثلاثون الماضية عند توفر صلاحية الاستخدام، وتُحدّث قراءة اليوم على الأكثر مرة في الدقيقة أثناء عمل الواجهة أو خدمة المراقبة. تُعاد الاستفادة من الأيام المكتملة لتقليل الاستعلامات.
- عند اختفاء UID من إحصاءات النظام بعد الحذف، يُعرض التطبيق باسمه وأيقونته المحفوظين وشارة «غير مثبت»، وتُستعاد قراءاته السابقة المناسبة للفترة المحددة. يظهر وصف «آخر قراءة محفوظة» لتمييزها عن البيانات الحالية.
- بيانات النظام المنفصلة لها الأولوية؛ لا يُضاف نفس UID مرتين. لا تُجمع قراءات اليوم والأسبوع والشهر المتداخلة معًا، ولا تُوزّع قراءة يوم كامل افتراضيًا على ساعة لم تُسجّل. يظل أرشيف Wi-Fi منفصلًا عن بيانات الهاتف.
- التطبيقات المحذوفة تبقى في «الكل» وتُستبعد من «المثبتة». إعادة التثبيت تعيد حالة «مثبت». لا تُنسب بيانات صاحب UID القديم إلى حزمة جديدة تستخدم المعرّف نفسه، ولا يُقسَّم UID المشترك بين الحزم بالتخمين.
- **حدود الحفظ:** لا يمكن استرجاع اسم/أيقونة أو قراءة لم تُحفظ قبل الحذف. إذا جمع النظام الاستهلاك تحت `UID_REMOVED`، نستعيد فقط ما رُصد محليًا قبل الحذف، لا حصة تخمينية من ذلك الإجمالي. قد تنقص الحركة التي حدثت بين آخر رصد والحذف، ويُوضَّح ذلك في الواجهة.
- تُحدَّد قراءات الأرشيف بآخر 45 يومًا وبحد أقصى 256 ملفًا؛ تبقى الأسماء والأيقونات محفوظة مستقلًا. مسح البيانات من الإعدادات يمسح الأرشيفين. تحميل الأيقونات والقراءات يتم خارج خيط الواجهة، مع أيقونة بديلة آمنة عند غياب الملف أو تلفه.

Manual device checks: grant usage access, open Apps and wait for archive capture; generate traffic with a known test app, uninstall it and refresh. Verify cached identity/icon, “Not installed” and the saved-reading timestamp even when NetworkStats drops the individual UID. Restart and switch today/week/month and Wi-Fi/mobile; compare daily versus overlapping period readings to check there is no double counting. Test reinstall, updates, missing icon files, search, Installed filter and Clear data. Unobserved time slices must not receive invented traffic. Check foreground monitoring still records readings when the activity is closed.

Local validation: actual production storage/archive/filter sources and six JUnit test classes were compiled with Kotlin 2.4.10 against Android 37 API and executed on Java 25 using a JUnit 4.12 runner. The 55 tests passed, including deleted-UID restoration, network/range separation, overlap prevention, timestamps, UID reuse, persistence and search. The project uses JUnit 4.13.2 in its normal Gradle build; APK/device validation is separate.

### البحث والترتيب في تطبيقات السجل
- بعد تحديد عمود يوم في مخطط السجل، يظهر شريط البحث وعكس ترتيب الاستهلاك فوق تطبيقات ذلك اليوم، بنفس تصميم قسم التطبيقات.
- العرض العادي يعرض أعلى عشرة تطبيقات، أو أقل عشرة عند عكس الترتيب. البحث بالاسم أو اسم الحزمة يشمل جميع تطبيقات اليوم؛ الحقل الفارغ أثناء البحث يعرضها كلها بالترتيب المختار.
- البحث مستقل عن بحث قسم التطبيقات وفلاتره، ولا يغيّر إجمالي اليوم أو نسب استهلاك التطبيقات. تحديد يوم/فترة جديدة يمسح البحث السابق ويمنع ظهور نتائج يوم قديم أثناء التحميل.
- البحث له موضع تمرير مستقل، مع إخفاء المخطط والملخص مؤقتًا والتركيز على قائمة اليوم. إغلاقه يعيد موضع السجل السابق. زر الرجوع يغلق البحث، وزر الإغلاق داخل الحقل يمسح النص أولًا ثم يغلق البحث إذا كان فارغًا. السحب للتحديث معطل أثناء البحث.

Validation: `HistoryAppsTest` adds eight JVM tests for top/bottom ten, searching beyond the first ten, blank queries, Arabic diacritics, package names, ordering, removed apps and empty results. All 63 tests in the local seven-class JVM suite passed. Device checks: select a chart column, search for a low-usage app, reverse order while typing, clear/close/back, reopen details, change day/period, and verify the date, scroll restoration, keyboard clearance and absence of the refresh indicator.
