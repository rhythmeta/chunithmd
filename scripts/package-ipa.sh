#!/bin/sh
# Package an unsigned device app for subsequent signing by the user.
set -eu

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 APP_PATH OUTPUT_IPA" >&2
    exit 1
fi
app_path=$(CDPATH='' cd -- "$1" && pwd)
mkdir -p "$(dirname -- "$2")"
output_ipa="$(CDPATH='' cd -- "$(dirname -- "$2")" && pwd)/$(basename -- "$2")"
info_plist="$app_path/Info.plist"
/usr/libexec/PlistBuddy -c 'Print :CFBundleExecutable' "$info_plist" > /dev/null
platform=$(/usr/libexec/PlistBuddy -c 'Print :DTPlatformName' "$info_plist")
[ "$platform" = iphoneos ] || { echo "Expected an iPhone device app, got $platform" >&2; exit 1; }
expected_build=$("$(dirname -- "$0")/build-number.sh")
actual_build=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleVersion' "$info_plist")
[ "$actual_build" = "$expected_build" ] || { echo "Build number mismatch: $actual_build != $expected_build" >&2; exit 1; }

package_dir=$(mktemp -d "${TMPDIR:-/tmp}/chunithmd-ipa.XXXXXX")
trap 'rm -rf "$package_dir"' EXIT HUP INT TERM
mkdir "$package_dir/Payload"
/usr/bin/ditto "$app_path" "$package_dir/Payload/chunithmd.app"
(cd "$package_dir" && /usr/bin/zip -qry chunithmd.ipa Payload)
mv "$package_dir/chunithmd.ipa" "$output_ipa"
/usr/bin/unzip -tq "$output_ipa"
printf 'IPA: %s (build %s)\n' "$output_ipa" "$actual_build"
