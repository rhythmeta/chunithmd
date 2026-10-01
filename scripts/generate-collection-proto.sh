#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
protoc --java_out=lite:android/app/src/main/java shared/song_collection.proto
