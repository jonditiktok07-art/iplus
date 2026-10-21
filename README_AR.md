# لانشر i-plus (أندرويد TV)

## البناء
1. افتح المجلد في Android Studio (Hedgehog أو أحدث) واضغط Sync.
2. Build > Build APK(s) تطلع لك الملف في app/build/outputs/apk/debug/
3. ثبّته على الشاشة (USB أو adb install app-debug.apk)

## التعيين كلانشر افتراضي
من داخل التطبيق: زر "تعيين كلانشر افتراضي"، أو من إعدادات الجهاز.

## تعطيل اللانشر القديم (بعد ما تعيّن الجديد افتراضي!)
زر "تعطيل اللانشر القديم" يعرض لك اللانشرات الموجودة ويعطيك الأمر.
بالـ ADB:
    adb shell pm disable-user --user 0 <package-name>
للتراجع:
    adb shell pm enable <package-name>
