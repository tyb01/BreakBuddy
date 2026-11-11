"""jarsigner can return success for an unsigned archive; require signature entries too."""
import sys
import zipfile
with zipfile.ZipFile(sys.argv[1]) as archive:
    entries = archive.namelist()
    if not any(name.startswith("META-INF/") and name.endswith(".SF") for name in entries):
        raise SystemExit("Release bundle is unsigned")
    if not any(name.startswith("META-INF/") and name.endswith((".RSA", ".EC", ".DSA")) for name in entries):
        raise SystemExit("Release bundle has no signature block")
print("Bundle signature entries present; jarsigner must also verify their validity")
