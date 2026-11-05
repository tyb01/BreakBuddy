"""Fail closed on unexplained permissions/components in both merged variants."""
import json
from pathlib import Path
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
allowlist = json.loads((root / "scripts/permissions.json").read_text())
a = "{http://schemas.android.com/apk/res/android}"
for variant, package in (("debug", "com.pawsup.debug"), ("release", "com.pawsup")):
    path = root / f"app/build/intermediates/merged_manifest/{variant}/process{variant.title()}MainManifest/AndroidManifest.xml"
    tree = ET.parse(path).getroot()
    actual = {node.get(a + "name") for node in tree if node.tag.startswith("uses-permission")}
    expected = {name.replace("${applicationId}", package) for name in allowlist}
    if actual != expected:
        raise SystemExit(f"{variant}: unexpected={actual - expected}; missing={expected - actual}")
    app = tree.find("application")
    assert app.get(a + "allowBackup") == "false", "Backup exclusions must remain enabled"
    service = next(n for n in app.findall("service") if n.get(a + "name") == "com.pawsup.monitoring.MonitoringService")
    assert service.get(a + "foregroundServiceType") == "specialUse"
    assert service.get(a + "exported") == "false"
    assert not any(n.get(a + "name") == "androidx.core.content.FileProvider" for n in app.findall("provider"))
    print(f"{variant}: {len(actual)} explained permissions; service and backup gates passed")
