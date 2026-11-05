"""Check tracked build inputs without printing potential secret values."""
import re
import subprocess
from pathlib import Path
root = Path(__file__).resolve().parents[1]
files = subprocess.check_output(["git", "ls-files"], cwd=root, text=True).splitlines()
for file in files:
    if file.startswith((".gradle/", "app/build/", "build/")) or file.endswith((".jks", ".keystore")) or file == "keystore.properties":
        raise SystemExit(f"Generated output or signing material tracked: {file}")
for file in (root / "app/build.gradle.kts", root / "build.gradle.kts"):
    if re.search(r'(?:storePassword|keyPassword)\s*=\s*"[^"\n]+"', file.read_text()):
        raise SystemExit(f"Literal signing credential in {file.name}")
print("Tracked source hygiene passed")
