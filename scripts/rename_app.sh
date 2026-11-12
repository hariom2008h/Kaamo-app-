#!/usr/bin/env bash
# Usage: scripts/rename_app.sh NewName   (run from the project root, before building)
# Replaces the visible product name (currently "Kaamo") in code, legal copy, SQL and docs.
set -euo pipefail
NEW="${1:?usage: rename_app.sh NewName}"
OLD="Kaamo"
grep -rIl "$OLD" app supabase README.md | xargs sed -i "s/$OLD/$NEW/g"
echo "Renamed $OLD -> $NEW. The package id (com.kaamo.app) is unchanged; change it separately if needed."
