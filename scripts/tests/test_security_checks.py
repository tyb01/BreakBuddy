import sys
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from scan_secrets import findings

class SecretChecksTest(unittest.TestCase):
    def test_detects_literal_signing_password_without_returning_it(self):
        value = "not-a-real-" + "credential"
        result = list(findings('storePassword = "' + value + '"'))
        self.assertEqual([(1, "literal-secret")], result)
        self.assertNotIn(value, repr(result))
    def test_detects_private_key_and_service_token(self):
        self.assertTrue(list(findings("-----BEGIN " + "PRIVATE KEY-----")))
        self.assertTrue(list(findings("ghp_" + "a" * 36)))
    def test_ignores_placeholder_and_environment_reference(self):
        self.assertFalse(list(findings('keyPassword = "REPLACE_LOCALLY"')))
        self.assertFalse(list(findings('keyPassword = signing.getProperty("keyPassword")')))
    def test_reports_location_only(self):
        self.assertEqual(2, list(findings("safe\n" + "AKIA" + "A" * 16))[0][0])

if __name__ == "__main__": unittest.main()
