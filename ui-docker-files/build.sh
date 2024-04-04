#!/bin/bash
set -e
mkdir dist
manifest_files=()
echo [] > dist/manifest.json
for d in open-xchange-appsuite-* ; do
    (cd $d && pnpm i && pnpm build)
    if [ -f $d/dist/manifest.json ]; then
        cp -R $d/dist/* dist/
        manifest_files+=($d/dist/manifest.json)
    fi
done
if [ -n "$manifest_files" ]; then
    jq -s 'add' "${manifest_files[@]}" | tr -d '\n' | tr -d ' ' > dist/manifest.json
fi
# For debugging
cat dist/manifest.json
