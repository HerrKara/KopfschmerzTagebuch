#!/usr/bin/env bash
# Baut die APK ohne Gradle.
# Benoetigt: JDK 11+, aapt, dx (dalvik-exchange), zipalign, apksigner, android.jar (API 23)
#   Ubuntu/Debian: apt install aapt dalvik-exchange zipalign apksigner android-sdk-platform-23
set -euo pipefail
cd "$(dirname "$0")"

ANDROID_JAR="${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}"
OUT=build
APK_NAME=Kopfschmerztagebuch.apk
VERSION_CODE=1
VERSION_NAME=1.0

rm -rf "$OUT" && mkdir -p "$OUT/gen" "$OUT/classes"

aapt package -f -m -J "$OUT/gen" -M app/src/main/AndroidManifest.xml -S app/src/main/res \
     -A app/src/main/assets -I "$ANDROID_JAR" -F "$OUT/unsigned.apk" \
     --min-sdk-version 23 --target-sdk-version 34 \
     --version-code "$VERSION_CODE" --version-name "$VERSION_NAME"

javac --release 8 -Xlint:-options -cp "$ANDROID_JAR" -d "$OUT/classes" \
      $(find "$OUT/gen" app/src/main/java -name '*.java')

DX="${DX:-/usr/lib/android-sdk/build-tools/debian/dx}"
"$DX" --dex --output="$OUT/classes.dex" "$OUT/classes"
( cd "$OUT" && aapt add -f unsigned.apk classes.dex >/dev/null )

zipalign -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

# Fester Schluessel, damit spaetere Versionen als Update ueber die alte installiert werden koennen.
KS=keystore/kopfschmerztagebuch.jks
if [ ! -f "$KS" ]; then
  mkdir -p keystore
  keytool -genkeypair -keystore "$KS" -storepass kopfschmerz -keypass kopfschmerz \
          -alias key -keyalg RSA -keysize 2048 -validity 36500 \
          -dname "CN=Kopfschmerztagebuch" 2>/dev/null
fi
apksigner sign --ks "$KS" --ks-pass pass:kopfschmerz --key-pass pass:kopfschmerz \
          --out "$APK_NAME" "$OUT/aligned.apk"
apksigner verify --verbose "$APK_NAME" | head -5
ls -l "$APK_NAME"
