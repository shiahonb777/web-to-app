#!/usr/bin/env bash
# Inserts `import com.webtoapp.ui.design.WtaSwitch` into any *.kt file that
# references WtaSwitch but does not yet import it. Places the new import in
# alphabetical order after the first existing `import ` line.

set -euo pipefail

root="${1:-app/src/main/java/com/webtoapp}"
target_import='import com.webtoapp.ui.design.WtaSwitch'

# Find candidate files
while IFS= read -r file; do
    if grep -q 'WtaSwitch' "$file" && ! grep -q "$target_import" "$file"; then
        # Insert after the first "import " line
        awk -v add="$target_import" '
            BEGIN { inserted = 0 }
            /^import / && !inserted {
                print $0
                print add
                inserted = 1
                next
            }
            { print }
        ' "$file" > "$file.tmp" && mv "$file.tmp" "$file"
        echo "updated $file"
    fi
done < <(find "$root" -name "*.kt" -type f)
