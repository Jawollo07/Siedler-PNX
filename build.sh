#!/usr/bin/env bash

set -Eeuo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
plugin_dir="${SERVER_PLUGIN_DIR:-/home/jannik/Cloud/Dev/test-server-pnx/plugins}"

cd "$script_dir"

if [[ ! -d "$plugin_dir" ]]; then
	printf 'Plugin directory does not exist: %s\n' "$plugin_dir" >&2
	exit 1
fi

printf 'Building Siedler from %s\n' "$script_dir"
mvn --batch-mode clean package

shopt -s nullglob
artifacts=(target/siedler-*.jar)

if (( ${#artifacts[@]} != 1 )); then
	printf 'Expected exactly one plugin JAR, found %d.\n' "${#artifacts[@]}" >&2
	exit 1
fi

rm -f -- "$plugin_dir"/siedler-*.jar
cp -- "${artifacts[0]}" "$plugin_dir/"

printf 'Installed %s into %s\n' "${artifacts[0]}" "$plugin_dir"