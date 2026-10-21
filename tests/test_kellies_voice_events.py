"""Checks for announcement and camera event inputs. No Home Assistant install required."""

import importlib.util
import json
import pathlib
import uuid
import unittest

import sys

_COMPONENT = pathlib.Path(__file__).resolve().parents[1] / "custom_components" / "kellies_voice"
sys.path.insert(0, str(_COMPONENT))
_PATH = _COMPONENT / "validation.py"
_SPEC = importlib.util.spec_from_file_location("kellies_voice_validation", _PATH)
validation = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(validation)


class ValidationTests(unittest.TestCase):
    def test_custom_integration_english_translations(self):
        # Custom HA integrations load translations/en.json, not strings.json.
        source = json.loads((_COMPONENT / "strings.json").read_text(encoding="utf-8"))
        shipped = json.loads((_COMPONENT / "translations" / "en.json").read_text(encoding="utf-8"))
        self.assertEqual(source, shipped)
        self.assertIn("use_discovery", shipped["config"]["abort"])
        self.assertIn("confirm", shipped["config"]["step"])
        self.assertIn("announce", shipped["services"])
        self.assertIn("show_camera", shipped["services"])

    def test_manual_tv_pairing_identifier(self):
        stable = str(uuid.uuid4())
        self.assertEqual(validation.clean_tv_device_id(stable.upper()), stable)
        self.assertEqual(validation.clean_tv_device_id(stable.replace("-", "")), stable)
        for invalid in ("", "my-tv", "123456", str(uuid.uuid1()), None):
            with self.assertRaises(ValueError):
                validation.clean_tv_device_id(invalid)

    def test_manual_pairing_translations(self):
        import json
        messages = json.loads((_COMPONENT / "translations" / "en.json").read_text(encoding="utf-8"))
        self.assertIn("user", messages["config"]["step"])
        self.assertIn("device_id", messages["config"]["step"]["user"]["data"])

    def test_title_and_text_limits(self):
        self.assertEqual(validation.clean_title("  Washer   done "), "Washer done")
        with self.assertRaises(ValueError):
            validation.clean_title("   ")
        with self.assertRaises(ValueError):
            validation.clean_title("x" * 66)
        self.assertEqual(len(validation.clean_text("x" * 500)), 500)
        with self.assertRaises(ValueError):
            validation.clean_text("x" * 501)

    def test_duration_cap(self):
        self.assertEqual(validation.clean_duration(None, 10), 10)
        self.assertEqual(validation.clean_duration("20", 10), 20)
        with self.assertRaises(ValueError):
            validation.clean_duration(0, 10)
        with self.assertRaises(ValueError):
            validation.clean_duration(46, 10)
        with self.assertRaises(ValueError):
            validation.clean_duration("soon", 10)

    def test_camera_entity_only(self):
        self.assertEqual(validation.clean_camera_entity(" Camera.Front_Door "), "camera.front_door")
        for bad in ("rtsp://user:pass@camera/live", "http://home.local/token", "light.front", "camera.front/door", ""):
            with self.assertRaises(ValueError):
                validation.clean_camera_entity(bad)

    def test_one_tv_or_all_configured(self):
        tvs = ["living-room", "bedroom"]
        self.assertEqual(validation.target_device_ids(None, tvs), tvs)
        self.assertEqual(validation.target_device_ids("  ", tvs), tvs)
        self.assertEqual(validation.target_device_ids("bedroom", tvs), ["bedroom"])
        with self.assertRaises(ValueError):
            validation.target_device_ids("unknown", tvs)
        with self.assertRaises(ValueError):
            validation.target_device_ids(None, [])


if __name__ == "__main__":
    unittest.main()
