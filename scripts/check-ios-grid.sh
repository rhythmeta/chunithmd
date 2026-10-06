#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p artifacts/ios-checks
swiftc ios/chunithmd/CatalogGridGeometry.swift test/ios/grid-geometry.swift -o artifacts/ios-checks/grid-geometry
artifacts/ios-checks/grid-geometry
