<div align="center" dir="rtl">

# 📶 مقياس الواي فاي

### اعرف استهلاكك، وتابع باقتك، قبل أن تنفد البيانات.

تطبيق أندرويد لمراقبة استهلاك الإنترنت عبر Wi‑Fi، مع إحصاءات التطبيقات وسجلّ مرئي وأدوات متابعة مباشرة.

[![الإصدار](https://img.shields.io/github/v/release/my-live-is-a-lie/mywifiaData?include_prereleases=true&style=for-the-badge&color=7c83fd)](https://github.com/my-live-is-a-lie/mywifiaData/releases)
[![التنزيلات](https://img.shields.io/github/downloads/my-live-is-a-lie/mywifiaData/total?style=for-the-badge&color=8b6de9)](https://github.com/my-live-is-a-lie/mywifiaData/releases)
![Android 8+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Preview](https://img.shields.io/badge/status-preview%20%7C%20in%20development-e7b7ff?style=for-the-badge)

<a href="https://github.com/my-live-is-a-lie/mywifiaData/releases/download/v1.0.1-preview.1/app-universal-release.apk">
  <img src="https://img.shields.io/badge/Download-Universal%20APK-5264d8?style=for-the-badge&logo=android&logoColor=white" alt="تحميل النسخة الشاملة لجميع المعماريات" />
</a>
&nbsp;
<a href="https://github.com/my-live-is-a-lie/mywifiaData/releases/tag/v1.0.1-preview.1">
  <img src="https://img.shields.io/badge/All%20architectures-Release%20files-454a5c?style=for-the-badge&logo=github&logoColor=white" alt="عرض ملفات جميع المعماريات" />
</a>

**نسخة أولية قيد التطوير — Android 8.0 فأحدث**

</div>

> ⚠️ قد تحتوي النسخة التجريبية على أخطاء أو تغييرات. المستودع خاص حاليًا، لذا يتطلب تنزيل الإصدار تسجيل الدخول بحساب يملك صلاحية الوصول.

## ✨ الميزات الأساسية

- **لوحة رئيسية واضحة:** نسبة استهلاك الباقة، المتبقي، الأيام، وإجماليات التنزيل والرفع.
- **إحصاءات لكل تطبيق:** استهلاك التطبيقات مع فلاتر للفترة ونوع التطبيقات وفرز النتائج.
- **سجلّ بصري:** مخططات وتقارير للاستهلاك اليومي والأسبوعي والشهري.
- **متابعة مباشرة:** نافذة عائمة قابلة للتحريك تعرض الاستهلاك أثناء استخدام تطبيقات أخرى.
- **اختبار سرعة الإنترنت:** تشغيل الاختبار والرجوع إلى نتائجه السابقة.
- **تنبيهات ومراقبة:** متابعة الاستهلاك في الخلفية والتنبيه عند الاقتراب من حدّ الباقة.
- **خطة تناسبك:** ضبط حجم الباقة ودورة الفوترة، مع عناصر واجهة للشاشة الرئيسية.
- **واجهة مرنة:** العربية والإنجليزية، ومظهر داكن أو فاتح.

## 📱 لقطات من التطبيق

<div align="center">
<table>
  <tr>
    <td align="center"><img src="docs/screenshots/home.jpg" width="210" alt="الشاشة الرئيسية" /></td>
    <td align="center"><img src="docs/screenshots/apps.jpg" width="210" alt="إحصاءات التطبيقات" /></td>
    <td align="center"><img src="docs/screenshots/history.jpg" width="210" alt="سجل الاستخدام" /></td>
    <td align="center"><img src="docs/screenshots/settings.jpg" width="210" alt="الإعدادات" /></td>
  </tr>
  <tr>
    <td align="center">الرئيسية</td>
    <td align="center">التطبيقات</td>
    <td align="center">السجل</td>
    <td align="center">الإعدادات</td>
  </tr>
</table>
</div>

## ⬇️ التنزيل والتوافق

الإصدار يوفّر APK شاملًا موصى به، إضافةً إلى ملفات مخصّصة لمعالجات **ARM64** و**ARMv7** و**x86_64** و**x86**. اختر الملف الشامل إن لم تكن متأكدًا من معمارية جهازك.

- [تحميل APK الشامل](https://github.com/my-live-is-a-lie/mywifiaData/releases/download/v1.0.1-preview.1/app-universal-release.apk)
- [عرض الإصدار وجميع ملفات APK](https://github.com/my-live-is-a-lie/mywifiaData/releases/tag/v1.0.1-preview.1)

يتطلب عرض استهلاك كل تطبيق تفعيل صلاحية **الوصول إلى الاستخدام** من إعدادات أندرويد. وتحتاج النافذة العائمة إلى السماح بالظهور فوق التطبيقات الأخرى.

## 🛠️ نبذة تقنية

مبنيّ بـ **Kotlin** و**Jetpack Compose** و**Material 3 Expressive**. يُنتج GitHub Actions إصدارات Release موقّعة، ويشغّل اختبارات الوحدة، ثم يرفق ملفات APK بإصدار GitHub عند تشغيله يدويًا أو عند دفع وسم يبدأ بـ `v`.
