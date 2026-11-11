"""Fail closed if checksum/lock/pinned-action safeguards are removed."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
wrapper = (root / "gradle/wrapper/gradle-wrapper.properties").read_text()
assert re.search(r"^distributionSha256Sum=[a-f0-9]{64}$", wrapper, re.M), "Wrapper checksum missing"
metadata = ET.parse(root / "gradle/verification-metadata.xml").getroot()
ns = {"v": "https://schema.gradle.org/dependency-verification"}
assert metadata.find("v:configuration/v:verify-metadata", ns).text == "true"
assert not metadata.findall(".//v:trusted-artifacts", ns), "Do not bypass artifact verification"
artifacts = metadata.findall(".//v:artifact", ns)
assert artifacts and all(a.find("v:sha256", ns) is not None for a in artifacts)
assert any(a.get("name", "").endswith("-linux.jar") and a.get("name", "").startswith("aapt2-") for a in artifacts), "CI aapt2 is not pinned"
assert (root / "app/gradle.lockfile").is_file(), "Dependency lock missing"
for workflow in (root / ".github/workflows").glob("*.yml"):
    for action in re.findall(r"uses:\s+([^\s#]+)", workflow.read_text()):
        assert re.fullmatch(r"[A-Za-z0-9_./-]+@[0-9a-f]{40}", action), f"Unpinned action in {workflow.name}"
print(f"Build security passed: {len(artifacts)} checksum-pinned artifacts, locks and immutable CI actions")
