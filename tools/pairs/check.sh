#!/usr/bin/env bash
# Checks one pair file on its own, safe to run alongside other writers:
#   bash tools/pairs/check.sh src/main/java/dev/wildercord/pairs/b001/Pairs001.java
# 1. its @Pair entries are valid and clash with nothing already written;
# 2. it compiles against the mod as last built (build/pair-classpath.txt).
set -e
cd "$(dirname "$0")/../.."
file="$1"
[ -f "$file" ] || { echo "no such file: $file"; exit 1; }
python tools/pairs/index.py --file "$file"
[ -f build/pair-classpath.txt ] || { echo "build/pair-classpath.txt is missing: ask the lead to export it"; exit 1; }
out="build/pair-check/$(basename "$file" .java)"
rm -rf "$out" && mkdir -p "$out"
javac -proc:none -nowarn -encoding UTF-8 -d "$out" -cp "$(cat build/pair-classpath.txt)" "$file"
echo "compiles: $file"
