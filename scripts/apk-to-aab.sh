#!/usr/bin/env bash
# Converts a built cargo-apk APK into a Google Play Store Android App Bundle (.aab)
# using Android SDK's `aapt2 convert --output-format proto` and Google's `bundletool`.
#
# Usage:
#   scripts/apk-to-aab.sh <input.apk> <output.aab>
set -euo pipefail

APK="${1:?usage: $0 <input.apk> <output.aab>}"
OUT_AAB="${2:?usage: $0 <input.apk> <output.aab>}"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ]; then
  echo "error: ANDROID_HOME or ANDROID_SDK_ROOT must be set" >&2
  exit 1
fi

BUILD_TOOLS="$(find "$SDK/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
AAPT2="$BUILD_TOOLS/aapt2"
if [ ! -x "$AAPT2" ]; then
  echo "error: aapt2 not found in $BUILD_TOOLS" >&2
  exit 1
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

BUNDLETOOL_VERSION="1.17.2"
BUNDLETOOL_JAR="$WORK/bundletool.jar"
echo "Downloading Google bundletool $BUNDLETOOL_VERSION..."
curl -fsSL --retry 5 --retry-all-errors \
  "https://github.com/google/bundletool/releases/download/${BUNDLETOOL_VERSION}/bundletool-all-${BUNDLETOOL_VERSION}.jar" \
  -o "$BUNDLETOOL_JAR"

echo "Converting APK resources and manifest to protobuf format..."
"$AAPT2" convert --output-format proto -o "$WORK/proto.apk" "$APK"

echo "Assembling AAB base module..."
mkdir -p "$WORK/unpack" "$WORK/base/manifest"
unzip -q "$WORK/proto.apk" -d "$WORK/unpack"

mv "$WORK/unpack/AndroidManifest.xml" "$WORK/base/manifest/AndroidManifest.xml"
mv "$WORK/unpack/resources.pb" "$WORK/base/resources.pb"
[ -d "$WORK/unpack/res" ] && mv "$WORK/unpack/res" "$WORK/base/res"
[ -d "$WORK/unpack/lib" ] && mv "$WORK/unpack/lib" "$WORK/base/lib"
[ -d "$WORK/unpack/assets" ] && mv "$WORK/unpack/assets" "$WORK/base/assets"

if ls "$WORK/unpack"/classes*.dex >/dev/null 2>&1; then
  mkdir -p "$WORK/base/dex"
  mv "$WORK/unpack"/classes*.dex "$WORK/base/dex/"
fi

# Bundle base module into base.zip (no META-INF)
(cd "$WORK/base" && zip -qr "$WORK/base.zip" .)

OUT_ABS="$(cd "$(dirname "$OUT_AAB")" && pwd)/$(basename "$OUT_AAB")"
rm -f "$OUT_ABS"

echo "Building Android App Bundle ($OUT_ABS)..."
java -jar "$BUNDLETOOL_JAR" build-bundle \
  --modules="$WORK/base.zip" \
  --output="$OUT_ABS"

echo "Validating generated AAB..."
java -jar "$BUNDLETOOL_JAR" validate --bundle="$OUT_ABS"
echo "Successfully built Play Store bundle: $OUT_ABS"
