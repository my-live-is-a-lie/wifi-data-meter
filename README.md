# مقياس الواي فاي — Wi‑Fi Data Meter

تطبيق أندرويد لقياس وحساب استهلاك الإنترنت على شبكة الواي فاي، مكتوب بـ **Kotlin** و **Jetpack Compose**
بواجهة مبنية على **Material 3 Expressive** (الوضع التعبيري من Material 3).

> الاسم الظاهر للتطبيق: «مقياس الواي فاي» (العربية افتراضيًا) / «Wi‑Fi Data Meter» (English).

---

## المزايا

| الشاشة | ما تقدّمه |
|---|---|
| **لوحة المعلومات** | ثلاثة أنماط لبطاقة الاستهلاك (حلقة كاملة، حلقة منقّطة، وساعة حقيقية داخل ميناء منقّط) تُبدَّل بالسحب الأفقي، وبطاقة «اليوم» بالتخطيط المرجعي: بلاطتان كبيرتان للتنزيل (أزرق) والرفع (بنفسجي)، صف الإجمالي مع إجمالي الدورة، ولوحة اختبار السرعة عبر fast.com. إضافةً إلى تفاصيل شبكة الواي فاي (الاسم، قوة الإشارة، سرعة الرابط، النطاق 2.4/5/6 جيجاهرتز، معيار الواي فاي، عنوان IP والبوابة)، إجماليات الجلسة، وأزرار سريعة (تحديث / تصفير الجلسة / ضبط الباقة). |
| **التطبيقات** | قائمة فلاتر قابلة للإظهار والإخفاء لاختيار Wi-Fi أو بيانات الهاتف، وكل الوقت أو الباقة الحالية للواي فاي، وترتيب التطبيقات من الأعلى أو الأقل استهلاكًا، وتحديد العدد: الكل أو 5 / 10 / 15 / 20 / 25 تطبيقًا. مع تفصيل زمني (اليوم / الأسبوع / الشهر) وفلتر تطبيقات النظام. |
| **السجل** | مخطط أعمدة لاستهلاك آخر 7 أو 30 يومًا، مع تفاصيل اليوم عند اختياره وأكثر التطبيقات استهلاكًا فيه. |
| **الإعدادات** | حجم الباقة ويوم بدء الدورة، التنبيهات ونسبة التحذير، مُعدّل القياس، تشغيل/إيقاف المراقبة، الألوان الديناميكية، حالة صلاحية «الوصول إلى الاستخدام» في قسم «التخزين والصلاحيات»، ومدة الاحتفاظ بالسجل (الافتراضي شهر، حتى ثلاث سنوات أو لا محدود)، ومسح البيانات. نافذة واحدة موحّدة لإضافة/تحديث الباقة الأساسية أو الإضافية (الاسم والشعار والسعة والمدة وخيار منع التعديل مع شرحه)، مع إمكانية إعادة تعيين الباقة. |

### أنماط بطاقة الاستهلاك

- ثلاثة أنماط تُبدَّل بالسحب الأفقي داخل البطاقة ويُحفظ النمط المختار: حلقة كاملة،
  حلقة منقّطة، ونمط **ساعة الاستهلاك**. (أُزيل النمط الجانبي القديم من قائمة
  الاختيار، ويعود من كان عليه إلى الحلقة الكاملة تلقائيًا.)
- **ساعة الاستهلاك**: ميناء من ستّين نقطة (نقطة كبيرة لكل ساعة ونقاط دقيقة بينها)،
  وخارجها قوس يعرض نسبة الاستهلاك من الباقة يبدأ من **أسفل الدائرة** ويتقدم باتجاه
  عقارب الساعة حول الجانب الأيسر، ونص الساعة الحالية أعلى النسبة المئوية، وتحتهما
  «المستهلك» وقيمته، وعدد الأيام المتبقية أعلى الدائرة داخل الميناء مع كلمة «يوم»
  ملاصقة للرقم.
- **العقارب**: الثواني (حمراء) أقصرها، ثم الساعات (بلون التمييز)، ثم الدقائق (رمادية)
  أطولها؛ تُرسم القصيرة فوق الطويلة، وتتحرك مع وقت الجهاز الحقيقي بحركة انسيابية
  بإطارات الشاشة (بأجزاء الثانية) كساعة حائط ميكانيكية بلا قفزات.
- عندما يتجه عقرب نحو نص (النسبة المئوية أو عدد الأيام المتبقية) يتقلّص طوله من جهة
  النص فلا يغطيه ولا يلامسه، بتقلّص تدريجي مطاطي (نابض مستقل لكل عقرب) ثم يعود
  لطوله كاملًا بعد تجاوز النص، مع مسافة راحة بينه وبين كتلة النص الوسطى.
- **نص الساعة** أعلى النسبة بصيغة ١٢ ساعة بلغة التطبيق: «٥:٠٤ م» بالعربية
  (بأرقام عربية-هندية) و«5:04 PM» بالإنجليزية، وحجمه صغير لا يزاحم النسبة.
- عند عدم تحديد سعة للباقة يعرض النمط استهلاك اليوم مع رسالة «لم تُحدَّد باقة»
  وزر الانتقال إلى الإعدادات، مثل بقية الأنماط.

### أداة الشاشة الرئيسية

- أداة واحدة فقط باسم **«بطاقة الاستهلاك»**، تعرض النسبة (أو استهلاك اليوم عند
  عدم ضبط باقة)، والمتبقّي من الباقة، وأيام الدورة المتبقية، وتنزيل/رفع اليوم.
- **تتبع شكل البطاقة المختار داخل التطبيق**: حلقة كاملة، أو حلقة منقّطة، أو
  ساعة. تغيير النمط من البطاقة يعيد رسم كل الأدوات المضافة فورًا،
  ولا توجد للأداة إعدادات شكل مستقلة عن التطبيق.
- زر **«إضافة أداة»** يضيف الشكل المعروض نفسه، وصورة الأداة في قائمة الأدوات
  صورة البطاقة نفسها بدل أيقونة التطبيق، فلا تظهر أداة مختلفة عمّا اختاره المستخدم.
- تُحدَّث القيم بعد كل قياس في الخلفية (بحد أقصى مرة كل دقيقة تقريبًا)، وتظهر
  القراءة المحفوظة فور إضافة الأداة حتى قبل فتح التطبيق.
- **مقاس مرن**: القرص يكبر مع ارتفاع الأداة على الشاشة الرئيسية بدل أن يبقى
  صغيرًا وسط فراغ، ويُرسم النمط كله (النقاط والأرقام والعقارب) في فضاء تصميم
  ثابت ثم يُكبَّر بنسبة واحدة، فتبقى نسبه مطابقة لبطاقة التطبيق عند أي مقاس.

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
- **تحذير بيانات الهاتف:** إن كانت الشبكة النشطة بيانات الهاتف يظهر قبل البدء تحذير
  بكمية الاستهلاك التقديرية. يُحسب التقدير من الاستهلاك الفعلي لأحدث خمسة اختبارات
  (مع وزن أعلى للأحدث)، ويُقدَّر استهلاك السجلات القديمة من سرعتيها. الخيارات:
  «متابعة الاختبار»، «إلغاء»، و«تجاهل لمدة يوم» يخفي التحذير 24 ساعة كاملة ثم
  يعود تلقائيًا في الاختبار التالي.
- يُحفظ استهلاك كل اختبار فعليًا (معايرة + تنزيل + رفع) ضمن سجل النتائج، فالتقدير
  يتغيّر مع كل اختبار جديد بدل أن يبقى رقمًا ثابتًا.

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
│                                 # و SpeedTestDataUsage (تقدير استهلاك البيانات والتحذير)
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
  stall window, 45 s overall cap). Before a test over mobile data, a warning shows an adaptive estimate of the
  data it will use — computed from the last five tests’ measured consumption — with continue, cancel, and
  “ignore for a day” options that suppress the warning for 24 hours before it returns.

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

### السحب للتنقل بين الصفحات
- شريط التنقل السفلي يدعم النقر المعتاد والسحب الأفقي المستمر عبر الأيقونات أو تسمياتها. بعد تجاوز عتبة السحب، يتبدل التبويب عند دخول الإصبع منطقته دون الحاجة لرفعه، مع عكس ترتيب المناطق تلقائيًا في العربية.
- التظليل كبسولة مشتركة تتحرك خلف الأيقونات وتتبع موضع الإصبع، مع امتداد محدود (حتى 18dp) في الجانب المعاكس للحركة. يعود الامتداد تدريجيًا للصفر بمجرد توقف الحركة حتى إذا بقي الإصبع على الشاشة، ثم تستقر الكبسولة على أيقونة التبويب المحدد عند الإفلات أو إلغاء السحب.
- تبقى ألوان وأيقونات وتسميات وأهداف لمس Material وإتاحة TalkBack والنقر بلوحة المفاتيح كما هي. منطقة إيماءات النظام أسفل الشريط مستثناة. الخروج عموديًا من الشريط يوقف تغيير الصفحات حتى العودة إليه، والنقر العادي لا يتحول إلى سحب بسبب اهتزاز بسيط للإصبع.
- حالة الإيماءة لا تعتمد على الصفحة المحددة، ولذلك لا تنقطع عند أول تبديل. تغيير اتجاه الواجهة أو حجم الشريط يلغي السحب بأمان ويعيد وضع التظليل.

Validation: `NavigationDragTest` has 16 JVM checks for continuous RTL/LTR sweeps, boundary crossing, edge clamping, resizing, direction reversals and bounded one-sided indicator geometry. These passed locally with Kotlin 2.4.10 / Java 25 and JUnit 4.12. The Compose integration and animation still require an Android build/device check; pure geometry tests do not validate gestures or rendered frames.

Manual checks: in Arabic, drag from Home through Apps, History and Settings without lifting; reverse in the same gesture; pause with the finger down and check the tail retracts; release between icons and check settling; start dragging on a label; leave vertically and return; test ordinary taps, TalkBack, RTL/LTR, large fonts, rotation, gesture/three-button system navigation, and leaving dashboard edit mode. Check that the final page is not changed by an extra click on release.

### السحبة القصيرة من التظليل
- بدء السحب من داخل الكبسولة المظللة ثم إفلات سحبة أفقية قصيرة (16dp على الأقل، أو عتبة لمس النظام إذا كانت أكبر) يختار الأيقونة المجاورة في اتجاه حركة الإصبع ويحرّك الكبسولة إليها بالأنيميشن الموجود، دون الحاجة للوصول إلى منطقة الأيقونة التالية.
- هذه إضافة للسحب المستمر: إذا مر الإصبع بتبويب آخر بالفعل، لا تحدث خطوة إضافية عند الإفلات، حتى عند الرجوع إلى التبويب الأصلي في نفس السحبة. السحب من خارج الكبسولة يحتفظ بسلوك التنقل حسب موضع الإصبع فقط.
- لا التفاف بين طرفي الشريط، ولا انتقال إضافي عند إلغاء الإيماءة أو اهتزاز الإصبع البسيط. حساب الاتجاه فعلي على الشاشة في العربية والإنجليزية، واختبار بداية اللمس يطابق حدود الكبسولة المستديرة بما فيها ذيلها المرئي.

Validation: the navigation JVM suite now passes 28 tests (12 new checks for short swipes and capsule hit-testing). The new release gesture path has not yet been built or exercised on an Android device. Manual checks: short left/right swipes starting on the selected capsule in RTL/LTR; a tap and sub-threshold jitter; an outward swipe at either end; long scrubs and out-and-back scrubs with no extra page on release; starts on a label/outside the capsule; cancellation and multi-touch.

### تكبير الأيقونة حسب قرب التظليل
- أثناء لمس شريط التنقل، تتدرج الأيقونة من حجمها العادي إلى 120% كلما اقترب مركز التظليل من مركزها. التكبير يعتمد على المسافة الفعلية بينهما، وليس مجرد اختيار الصفحة أو طول ذيل التظليل، ويعمل في RTL وLTR.
- يبقى الحجم مكبّرًا عند التوقف فوق الأيقونة مع إبقاء الإصبع مضغوطًا. الابتعاد عنها يصغّرها تدريجيًا ويكبّر الأيقونة التالية؛ الإفلات أو إلغاء الإيماءة يعيد الحجم الطبيعي بسلاسة، بما في ذلك اللمس دون سحب.
- التكبير تحوّل رسومي داخل غلاف ثابت بحجم 24dp، فلا يتغيّر ارتفاع الشريط أو مكان النصوص أو مناطق اللمس أو مركز الكبسولة. السحب المستمر والسحبة القصيرة إلى الأيقونة المجاورة محفوظان.

Validation: all 36 navigation JVM tests passed locally (8 new proximity-scale tests: gradual approach/departure, stationary hold, release, neighbor handoff, symmetry, RTL/LTR, bounds/resizing and invalid measurements). These validate target-scale math, not Compose rendering or physical gestures; the latest short-swipe and scaling integration still needs an Android build/device check. Manual checks: approach each icon slowly, hold for several seconds without moving, reverse direction, lift between icons, tap/hold without dragging, cancel or rotate during a gesture, and verify icons shrink smoothly without moving labels or the capsule. Also check that release settling and the elastic tail do not leave another icon enlarged.

### تثبيت نوع التطبيقات مع أدوات القائمة
- يظهر شريط «الكل / المثبتة / النظام» وحده دون نص «نوع التطبيقات» أو المسافة التي كانت أسفله؛ يبقى عنوان «التطبيقات» والعدد تحت الشريط كما في التصميم المطلوب.
- يجتمع فلتر «نوع التطبيقات» (الكل / المثبتة / النظام)، ثم عنوان القائمة وعدد التطبيقات، ثم البحث والترتيب داخل رأس ثابت واحد في قسم التطبيقات. يحتفظ بالترتيب والمسافات السابقة ويظل ظاهرًا عند تمرير صفوف الاستهلاك، دون أن يدفع رأس ثابت آخر الفلتر خارج الشاشة.
- خلفية الرأس معتمة حتى لا تظهر صفوف التطبيقات خلف أزرار الفلتر أو الفراغات أثناء السحب. السلوك المدمج أثناء البحث لم يتغير: يُخفى النوع والعنوان مؤقتًا ويبقى حقل البحث وحده، مع الاحتفاظ بفلتر النوع المختار وموضع التمرير المستقل قبل البحث.

Manual checks: scroll past the period summary/charts into app rows and verify the type selector remains above search/sort; switch All/Installed/System while pinned and check the count and rows; open, type, clear and close search and verify scroll restoration; repeat with empty results, long lists and large fonts. This header-only change passed whitespace/diff checks; Android rendering/build validation is still pending with the other local navigation changes.

### مقبض التمرير السريع لقائمة التطبيقات
- عند تثبيت شريط نوع التطبيقات، يظهر في أقصى اليمين الفعلي (حتى في العربية) مقبض رأسي صغير بحجم 2×16dp وعتامة 40%، يوضح موضع التمرير بين بداية تطبيقات القائمة ونهايتها. لا يحتسب المخططات والملخصات والفلاتر ضمن تقدير الموضع.
- هذه الميزة في **قسم التطبيقات وقائمة تطبيقات اليوم المحدد في السجل**، باستخدام نفس المكوّن مع حالة تمرير مستقلة لكل قائمة. السحب الرأسي من أي موضع ضمن آخر 24dp من الحافة اليمنى لقائمة التطبيقات يكبّر المقبض إلى 5×32dp وينقله إلى ارتفاع الإصبع ويحرك القائمة معه؛ لا يلزم لمس المقبض الصغير أولًا. اللمسة وحدها أو الاهتزاز البسيط لا يقفزان بالقائمة.
- عند رفع الإصبع أو تحريكه خارج الحافة اليمنى يصغر المقبض ويعود خافتًا فور بدء أنيميشن الرجوع، ويتوقف التمرير السريع. الرجوع إلى الحافة بعد إلغاء السحبة يتطلب بدء لمسة جديدة. يبقى التمرير العادي خارج الشريط الضيق كما هو، وتظل الشرطة صغيرة وخافتة أثناءه.
- مساره يبدأ أسفل الرأس المثبت وينتهي قبل حافة القائمة السفلية، ولا يغطي الفلاتر أو شريط التنقل. يُخفى قبل تثبيت الرأس، وفي البحث أو غياب الصلاحية أو خلو القائمة أو عندما تتسع الشاشة لكل التطبيقات.
- مؤشر الموضع تقديري بحسب الصفوف المرئية لأن بعض الصفوف أطول (مثل القراءات المحفوظة). الوصول للنهاية يستخدم آخر صف فعلي؛ الرجوع للبداية يبقي التطبيق الأول تحت الرأس المثبت. تغيير النوع أو الترتيب أو نطاق البيانات يلغي الإمساك القديم، ويُتاح ضبط الموضع أيضًا عبر إجراء إمكانية الوصول.

Validation: 58 local JVM checks passed across `NavigationDragTest` and `AppsFastScrollTest`, including edge-wide activation, absolute finger mapping, horizontal exit, rail boundaries, pinned-only visibility, empty/fitting lists, top/middle/end mapping, padding, partial rows, variable heights and filtered counts. XML parsing and diff checks passed. Compose build and device interaction checks remain pending. Manual checks: scroll until the type bar pins, start a vertical drag anywhere on the right edge away from the small thumb, hold and drag to both ends, exit sideways without lifting, release/cancel, reverse sort and change type/count, reopen search, test saved-usage rows/large fonts, and verify ordinary list dragging away from the thumb still works and neither the header nor bottom navigation is covered.

### مقبض الحافة في تطبيقات السجل
- بعد الضغط على عمود يوم في مخطط السجل، يظهر نفس المقبض على يمين تطبيقات ذلك اليوم عندما يصل شريط البحث والترتيب إلى موضعه المثبت. السحب من أي موضع على الحافة اليمنى يحرّك القائمة والمقبض معًا؛ الإفلات أو الابتعاد عن الحافة يعيده صغيرًا وخافتًا.
- لا يغطي المخطط أو بطاقة اليوم أو الملخص، ولا يظهر قبل تحديد يوم، أو أثناء التحميل/البحث، أو عندما تكفي الشاشة لعرض كل الصفوف. الانتقال ليوم/فترة أخرى أو تغيير ترتيب التطبيقات يلغي سحبة اليوم السابق.
- يُحسب المدى من التطبيقات المعروضة فعلًا (أعلى/أقل عشرة في العرض العادي كما كان)، لا من إجمالي التطبيقات غير المعروضة. لم يتغير نطاق البحث أو عدد الصفوف أو حالة تمرير قسم التطبيقات.

Validation: 62 tests in the local navigation/fast-scroll JVM suite pass, including four History-specific checks for the shorter pinned header, rendered-row bounds, pinned-only visibility and independent list targets. Device/build verification is still pending. Manual checks: select a chart column, scroll to pin the app controls, drag from the right edge to both ends and leave sideways; change day/period/order, close the day card, try a short/empty day, open/close search, and switch between Apps and History to verify independent positions.

### انتقال الصفحات عند التنقل
- الانتقال بين التبويبات يمر بانزلاق أفقي متزامن لخروج الصفحة السابقة ودخول التالية، بمدة أساسية 240ms وتباطؤ سلس. يعمل مع السحب المستمر والسحبة القصيرة فقط؛ تدخل الصفحة من جهة أيقونتها الفعلية في RTL وLTR. النقر العادي على أيقونة يبدّل الصفحة مباشرةً دون انزلاق أو تلاشي، كما في السلوك القديم.
- اتجاه كل انتقال يُحسب من آخر تبويب مطلوب، لا من آخر أنيميشن اكتمل، لتصحيح الاتجاه عند عكس السحب سريعًا. مفتاح الأنيميشن هو التبويب فقط، لذلك لا تعيد تغييرات الاستهلاك أو كتابة البحث تشغيل حركة الصفحة.
- شريطا التنقل والعنوان خارج حركة المحتوى، ويظل شريط التنقل يستقبل السحب خلال الأنيميشن. محتوى الصفحات مقصوص ضمن مساحة ثابتة دون أنيميشن تغيير الارتفاع، ولا تُمرَّر نقرات إلى صفوف تغادر الشاشة أثناء الانتقال.
- تُحفظ الحالات القابلة للحفظ لكل تبويب مع مواضع القوائم المستقلة الموجودة. الشاشة المغادرة لا تتحكم في تركيز البحث/لوحة المفاتيح أو زر الرجوع، والسحب للتحديث متوقف مؤقتًا أثناء الانتقال لمنع ظهور مؤشر التحديث بسبب الحركة.

Validation: all 70 local JVM checks in the navigation, fast-scroll and page-motion suites passed, including eight new direction tests (RTL/LTR, same tab, multi-tab jumps, rapid reversal, and short-swipe integration). Static wiring/diff checks passed. This does not validate Compose animation frames, timing or gestures on a device; Android build/device verification is still pending. Manual checks: continuous sweeps across all four tabs, fast reversal before a slide ends, short swipes, tapping the active tab, going to Settings from the dashboard shortcut, restoring scrolled Apps/History, entering/leaving expanded search with the keyboard, Back during a transition, RTL/LTR and rotation.

### فصل حركة السحب عن النقر على شريط التنقل
- مسارا السحب المستمر والسحبة القصيرة يبلّغان عن اختيار إيمائي، بينما النقر العادي (وكذلك تفعيل الأيقونة بإمكانية الوصول/لوحة المفاتيح) يستخدم مسارًا مباشرًا دون حركة انتقال صفحة.
- النقر أثناء انتقال جارٍ يلغيه ويعرض الصفحة المطلوبة مكتملة فورًا، حتى إذا نقر المستخدم على أيقونة الوجهة المختارة بالفعل. النقر على الصفحة الحالية بعد استقرارها لا يعيد تركيبها أو يغيّر تركيز البحث. تحريك ظل الشريط وتكبير الأيقونات لا يتغيران.
- تستمر الحركة عند تغيير الوجهة بإيماءة أخرى، ولا ينتقل اختيار نوع الحركة من إيماءة سابقة إلى نقرة لاحقة. مواضع القوائم وحالات الصفحات المحفوظة تبقى مستقلة.

Validation: 76 JVM navigation/scroll checks passed locally, including six new tests separating tap/drag behavior, cancelling a slide by tapping its current target, idle same-tab taps and alternating input types. Static callback wiring/diff checks passed. Compose/device validation is pending. Manual checks: tap every tab (instant), swipe continuously and with a short swipe (slide), interrupt a running slide by tapping another tab or its selected destination, then drag again; verify no remaining outgoing content, scroll loss or keyboard-focus changes on a same-tab idle tap.
