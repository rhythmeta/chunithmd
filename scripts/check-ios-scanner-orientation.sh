#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
check_dir=$(mktemp -d "${TMPDIR:-/tmp}/chunithmd-orientation.XXXXXX")
trap 'rm -rf "$check_dir"' EXIT
swiftc ios/chunithmd/Services/Scanner/ScannerPhysicalOrientation.swift \
    ios/chunithmd/Views/Scanner/ScannerOverlayGeometry.swift test/ios/scanner-orientation.swift -o "$check_dir/check"
"$check_dir/check"
