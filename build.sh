#!/usr/bin/env bash
# 手动构建 com.wuzhi APK（不用 Gradle）
set -e

ROOT="/c/dsh/wuzhi"
APP="$ROOT/app"
BUILD="$ROOT/build"
SDK="/c/Users/12452/AppData/Local/Android/Sdk"
BT="$SDK/build-tools/36.0.0"
ANDROID_JAR="$SDK/platforms/android-35/android.jar"
JAVAC="javac"
KEYSTORE="$ROOT/wuzhi.keystore"
KS_PASS="wuzhi123"
KS_ALIAS="wuzhi"

echo "=== 0. clean ==="
rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex"

echo "=== 1. aapt2 compile ==="
"$BT/aapt2.exe" compile --dir "$APP/res" -o "$BUILD/res.zip"

echo "=== 2. aapt2 link ==="
"$BT/aapt2.exe" link \
  -o "$BUILD/app.unsigned.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$APP/AndroidManifest.xml" \
  --java "$BUILD/gen" \
  -R "$BUILD/res.zip" \
  --auto-add-overlay \
  --min-sdk-version 24 \
  --target-sdk-version 34

echo "=== 3. javac ==="
SRCS=$(find "$APP/src" "$BUILD/gen" -name "*.java")
BC="C:/Users/12452/AppData/Local/Android/Sdk/build-tools/36.0.0/core-lambda-stubs.jar;C:/Users/12452/AppData/Local/Android/Sdk/platforms/android-35/android.jar"
"$JAVAC" -source 8 -target 8 -encoding UTF-8 \
  -bootclasspath "$BC" \
  -d "$BUILD/classes" \
  $SRCS

echo "=== 4. d8 ==="
CLASSES=$(find "$BUILD/classes" -name "*.class")
"$BT/d8.bat" --min-api 24 --lib "$ANDROID_JAR" \
  --classpath "$BT/core-lambda-stubs.jar" \
  --output "$BUILD/dex" \
  $CLASSES

echo "=== 5. package dex into apk ==="
"C:/Users/12452/.dsh/dsh-runtimes/dsh-primary-runtime/dependencies/python/python.exe" - "$BUILD/app.unsigned.apk" "$BUILD/dex/classes.dex" <<'PY'
import sys, zipfile, shutil, os
apk, dex = sys.argv[1], sys.argv[2]
tmp = apk + ".tmp"
with zipfile.ZipFile(apk, 'r') as zin, zipfile.ZipFile(tmp, 'w', zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        if item.filename == 'classes.dex':
            continue
        data = zin.read(item.filename)
        zout.writestr(item, data)
    zout.write(dex, 'classes.dex')
shutil.move(tmp, apk)
print("dex packed ->", apk)
PY

echo "=== 6. zipalign ==="
"$BT/zipalign.exe" -f -p 4 "$BUILD/app.unsigned.apk" "$BUILD/app.aligned.apk"

echo "=== 7. keystore ==="
if [ ! -f "$KEYSTORE" ]; then
  keytool -genkeypair -keystore "$KEYSTORE" -storepass "$KS_PASS" -keypass "$KS_PASS" \
    -alias "$KS_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Wuzhi, OU=Personal, O=Wuzhi, L=Jinan, S=Shandong, C=CN"
fi

echo "=== 8. sign ==="
"$BT/apksigner.bat" sign \
  --ks "$KEYSTORE" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
  --ks-key-alias "$KS_ALIAS" \
  --v1-signing-enabled true --v2-signing-enabled true \
  --out "$ROOT/wuzhi.apk" "$BUILD/app.aligned.apk"

echo "=== 9. verify ==="
"$BT/apksigner.bat" verify --verbose "$ROOT/wuzhi.apk"
echo "--- badging ---"
"$BT/aapt2.exe" dump badging "$ROOT/wuzhi.apk" || true
echo "=== DONE: $ROOT/wuzhi.apk ==="
ls -l "$ROOT/wuzhi.apk"
