# Souqi Desktop (سوقي — نسخة الحاسوب)

تطبيق سطح مكتب رسمي لمنصة **سوقي** يعمل على أنظمة **Windows / macOS / Linux**، مبني بـ Electron الآمن (`contextIsolation: true`, `sandbox: true`) مع فتح روابط الطلبات (`wa.me`) تلقائياً في المتصفح أو تطبيق واتساب على الحاسوب.

## المتطلبات
- Node.js 18+ و npm
- وجود مجلد `../web` بجانب مجلد `desktop` (أو نسخ مخرجات `web/dist` إلى `desktop/web-dist`)

## التشغيل المحلي

```bash
cd desktop
npm install
npm run prepare-web
npm start
```

## بناء ملفات التثبيت للحاسوب (EXE / DMG / AppImage)

```bash
# Windows (.exe installer)
npm run dist:win

# macOS (.dmg)
npm run dist:mac

# Linux (.AppImage)
npm run dist:linux
```

ستجد ملفات التثبيت النهائية داخل مجلد `desktop/release/`.

## الرفع إلى مستودع عام مستقل على GitHub (`souqi-desktop`)
بعد تنفيذ `npm run prepare-web` (اختيارياً أو الاعتماد على CI)، يمكنك رفع مجلد `desktop` لمستودع عام باسم `souqi-desktop`:

```bash
cd desktop
git init
git add .
git commit -m "Initial commit: Souqi Desktop app"
gh repo create souqi-desktop --public --source=. --remote=origin --push
```
