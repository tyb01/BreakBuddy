"""Download a checksum-pinned primary release; scan only versioned/nonignored source.
No credentials are needed. Reports are fully redacted. Optional PR-range scanning
also catches secrets added and later removed inside the proposed change.
"""
import hashlib
import io
import json
import os
import platform
import re
import shutil
import subprocess
import tarfile
import tempfile
import urllib.request
import zipfile
from pathlib import Path
from scan_secrets import source_files

root = Path(__file__).resolve().parents[1]
config = json.loads((root / "scripts/security-tools.json").read_text())["gitleaks"]
windows = platform.system() == "Windows"
artifact = "windows_x64.zip" if windows else "linux_x64.tar.gz"
if platform.machine().lower() not in ("amd64", "x86_64"):
    raise SystemExit("Pinned scanner currently supports x64 Linux and Windows")
name = f"gitleaks_{config['version']}_{artifact}"
url = f"https://github.com/gitleaks/gitleaks/releases/download/v{config['version']}/{name}"
cache = root / "build/security-tools"
cache.mkdir(parents=True, exist_ok=True)
archive = cache / name
if not archive.exists():
    with urllib.request.urlopen(url, timeout=60) as response:
        archive.write_bytes(response.read())
raw = archive.read_bytes()
if hashlib.sha256(raw).hexdigest() != config[artifact]:
    raise SystemExit("Scanner archive checksum mismatch; refusing execution")
executable = cache / ("gitleaks.exe" if windows else "gitleaks")
# Read exactly one known member; never extract arbitrary paths from the archive.
if windows:
    with zipfile.ZipFile(io.BytesIO(raw)) as source:
        executable.write_bytes(source.read("gitleaks.exe"))
else:
    with tarfile.open(fileobj=io.BytesIO(raw), mode="r:gz") as source:
        executable.write_bytes(source.extractfile("gitleaks").read())
    executable.chmod(0o755)
with tempfile.TemporaryDirectory(prefix="pawsup-source-scan-") as temporary:
    snapshot = Path(temporary)
    for relative in source_files(root):
        source = root / relative
        if source.is_file():
            destination = snapshot / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, destination)
    subprocess.run([str(executable), "dir", str(snapshot), "--redact=100", "--no-banner"], check=True)
base = os.environ.get("SECRET_SCAN_BASE", "")
if base and base != "0" * 40:
    if not re.fullmatch(r"[0-9a-f]{40}", base):
        raise SystemExit("Invalid base commit for history scan")
    subprocess.run([str(executable), "git", str(root), "--log-opts=" + base + "..HEAD", "--redact=100", "--no-banner"], check=True)
