#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PACKAGE_NAME="${PACKAGE_NAME:-code.name.monkey.retromusic.debug}"
INSTALL_TASK="${INSTALL_TASK:-:app:installNormalDebug}"
INSTALL_APP=1

usage() {
  cat <<'USAGE'
Usage: scripts/launch-normal-debug.sh [--no-install]

Installs and launches the normal debug app on a connected Android device or
emulator. Set ANDROID_SERIAL when multiple devices are connected.

Environment overrides:
  ANDROID_HOME   Android SDK path. Falls back to local.properties sdk.dir.
  JAVA_HOME      Java 21 path. Falls back to local Zulu 21 when present.
  PACKAGE_NAME   App package to launch. Defaults to code.name.monkey.retromusic.debug.
  INSTALL_TASK   Gradle install task. Defaults to :app:installNormalDebug.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-install)
      INSTALL_APP=0
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

cd "$ROOT_DIR"

if [[ -z "${ANDROID_HOME:-}" && -f local.properties ]]; then
  SDK_DIR="$(sed -n 's/^sdk\.dir=//p' local.properties | tail -n 1)"
  if [[ -n "$SDK_DIR" ]]; then
    export ANDROID_HOME="$SDK_DIR"
  fi
fi

if [[ -z "${JAVA_HOME:-}" && -d /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home ]]; then
  export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home"
fi

if [[ -z "${ANDROID_HOME:-}" ]]; then
  echo "ANDROID_HOME is not set and local.properties has no sdk.dir." >&2
  exit 1
fi

ADB="$ANDROID_HOME/platform-tools/adb"
if [[ ! -x "$ADB" ]]; then
  echo "adb not found at $ADB." >&2
  exit 1
fi

DEVICES=()
while IFS= read -r DEVICE; do
  DEVICES+=("$DEVICE")
done < <("$ADB" devices | awk 'NR > 1 && $2 == "device" { print $1 }')

if [[ ${#DEVICES[@]} -eq 0 ]]; then
  echo "No Android device/emulator is connected." >&2
  echo "Start an emulator or plug in a device, then rerun this script." >&2
  exit 1
fi

ADB_ARGS=()
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  ADB_ARGS=(-s "$ANDROID_SERIAL")
elif [[ ${#DEVICES[@]} -gt 1 ]]; then
  echo "Multiple Android devices/emulators are connected:" >&2
  printf '  %s\n' "${DEVICES[@]}" >&2
  echo "Set ANDROID_SERIAL to the device id to use." >&2
  exit 1
fi

if [[ $INSTALL_APP -eq 1 ]]; then
  ./gradlew "$INSTALL_TASK"
fi

if [[ ${#ADB_ARGS[@]} -gt 0 ]]; then
  "$ADB" "${ADB_ARGS[@]}" shell monkey \
    -p "$PACKAGE_NAME" \
    -c android.intent.category.LAUNCHER \
    1
else
  "$ADB" shell monkey \
    -p "$PACKAGE_NAME" \
    -c android.intent.category.LAUNCHER \
    1
fi
