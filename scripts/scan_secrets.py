"""Scan tracked + nonignored source; report rule/location only, never matching values.
This supplements the pinned Gitleaks scan and covers literal Android signing passwords.
"""
from pathlib import Path
import re
import subprocess
import sys

RULES = {
    "private-key": re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----"),
    "aws-access-key": re.compile(r"\b(?:AKIA|ASIA)[A-Z0-9]{16}\b"),
    "github-token": re.compile(r"\b(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{50,})\b"),
    "google-api-key": re.compile(r"\bAIza[A-Za-z0-9_-]{30,}\b"),
    "slack-token": re.compile(r"\bxox[baprs]-[A-Za-z0-9-]{20,}\b"),
    "literal-secret": re.compile(r"(?i)(?:api[_-]?key|client[_-]?secret|access[_-]?token|storePassword|keyPassword|password)\s*[:=]\s*[\"']([^\"'\n]{8,})[\"']"),
}
PLACEHOLDER = re.compile(r"^(?:REPLACE(?:_LOCALLY)?|YOUR_[A-Z_]+|example|placeholder|\$\{.*\})$", re.I)

def findings(text):
    for number, line in enumerate(text.splitlines(), 1):
        for name, pattern in RULES.items():
            match = pattern.search(line)
            if match and not (name == "literal-secret" and PLACEHOLDER.fullmatch(match.group(1))):
                yield number, name

def source_files(root):
    raw = subprocess.check_output(["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"], cwd=root)
    return sorted(set(raw.decode().split("\0")) - {""})

def main():
    root = Path(__file__).resolve().parents[1]
    errors = []
    for relative in source_files(root):
        path = root / relative
        if not path.is_file():
            continue
        if path.suffix.lower() in (".jks", ".keystore", ".p12", ".pfx", ".pem") or path.name == ".env":
            errors.append(f"{relative}: forbidden credential-file type")
        try:
            content = path.read_text(encoding="utf-8")
        except (UnicodeError, OSError):
            continue
        errors.extend(f"{relative}:{line}: {rule} (value redacted)" for line, rule in findings(content))
    if errors:
        print("\n".join(errors))
        return 1
    print("Source secret checks passed (no secret values printed)")
    return 0

if __name__ == "__main__":
    sys.exit(main())
