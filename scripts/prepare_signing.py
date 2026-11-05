"""CI-only signing setup. Secrets are never printed."""
import base64
import os
from pathlib import Path
names = ("KEYSTORE_BASE64", "STORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
if any(not os.environ.get(name) for name in names):
    raise SystemExit("All four release environment signing secrets are required")
def escape(value):
    return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r").replace(" ", "\\ ")
Path("upload-key.jks").write_bytes(base64.b64decode(os.environ["KEYSTORE_BASE64"], validate=True))
Path("keystore.properties").write_text("storeFile=upload-key.jks\n" + "\n".join(
    f"{key}={escape(os.environ[env])}" for key, env in
    (("storePassword", "STORE_PASSWORD"), ("keyAlias", "KEY_ALIAS"), ("keyPassword", "KEY_PASSWORD"))) + "\n")
