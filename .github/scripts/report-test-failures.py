#!/usr/bin/env python3
"""ينشر اختبارات الوحدة الفاشلة كتنبيهات في صفحة GitHub Actions.

تقرير Gradle بصيغة HTML فقط، ولا يمكن تنزيله من السجل مباشرة، لذلك نقرأ ملفات
JUnit XML في مجلد نتائج الاختبارات ونطبع اسم الصنف والدالة وأول سطر من رسالة
الفشل كتنبيهات (`::error`) تظهر في صفحة التشغيل، مع قائمة كاملة في الملخّص.
"""

import glob
import os
import xml.etree.ElementTree as ET

MAX_ANNOTATIONS = 10
RESULTS_GLOB = "app/build/test-results/*/*.xml"


def first_line(text: str) -> str:
    for line in (text or "").splitlines():
        line = line.strip()
        if line:
            return line
    return "failed"


def main() -> int:
    failures = []
    total = 0
    for path in sorted(glob.glob(RESULTS_GLOB)):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        for case in root.iter("testcase"):
            total += 1
            problem = case.find("failure")
            if problem is None:
                problem = case.find("error")
            if problem is None:
                continue
            classname = case.get("classname") or "?"
            name = case.get("name") or "?"
            detail = first_line(problem.get("message") or problem.text)
            failures.append(f"{classname}.{name}: {detail}")

    if failures:
        summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
        if summary_path:
            with open(summary_path, "a", encoding="utf-8") as summary:
                summary.write(f"\n### اختبارات فاشلة ({len(failures)})\n\n")
                for item in failures[:50]:
                    summary.write(f"- {item}\n")
        for item in failures[:MAX_ANNOTATIONS]:
            escaped = item.replace("%", "%25").replace("\r", "").replace("\n", " ")
            print(f"::error title=Test::{escaped}")

    print(f"unit tests: {total}, failures: {len(failures)}")
    # الخطوة إخبارية فقط؛ الخطوة التي شغّلت الاختبارات هي التي تُسجّل الفشل.
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
