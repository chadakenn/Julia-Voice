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
_CONST_SPEC = importlib.util.spec_from_file_location("kellies_voice_const", _COMPONENT / "const.py")
const = importlib.util.module_from_spec(_CONST_SPEC)
_CONST_SPEC.loader.exec_module(const)


class ValidationTests(unittest.TestCase):
    def test_alert_layout_is_separate_from_the_voice_overlay(self):
        voice_settings = [item[0] for item in const.SELECTS]
        self.assertEqual(voice_settings, ["position", "size", "style", "auto_hide"])
        self.assertEqual(
            [(item[0], item[1], item[4]) for item in const.ALERT_SELECTS],
            [
                ("alert_position", "TV alert position", "Top right"),
                ("alert_size", "TV alert size", "Medium"),
            ],
        )
        self.assertEqual(
            const.ALERT_POSITIONS,
            ["Top left", "Top right", "Bottom left", "Bottom right", "Center"],
        )
        self.assertEqual(const.ALERT_SIZES, ["Small", "Medium", "Large"])
        self.assertNotIn("Original location", const.ALERT_POSITIONS)
        self.assertNotIn("Bottom center", const.ALERT_POSITIONS)

    def test_custom_integration_english_translations(self):
        # Custom HA integrations load translations/en.json, not strings.json.
        source = json.loads((_COMPONENT / "strings.json").read_text(encoding="utf-8"))
        shipped = json.loads((_COMPONENT / "translations" / "en.json").read_text(encoding="utf-8"))
        self.assertEqual(source, shipped)
        self.assertIn("use_discovery", shipped["config"]["abort"])
        self.assertIn("confirm", shipped["config"]["step"])
        self.assertIn("announce", shipped["services"])
        self.assertIn("show_camera", shipped["services"])
        self.assertIn("hide_camera", shipped["services"])

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

    def test_live_camera_service_schema_contract(self):
        source = (_COMPONENT / "services.py").read_text(encoding="utf-8")
        const = (_COMPONENT / "const.py").read_text(encoding="utf-8")
        self.assertIn("SHOW_LIVE_SCHEMA", source)
        self.assertIn("async_show_live_camera", source)
        self.assertIn("kellies_voice_live_camera", const)
        self.assertIn("MIN_LIVE_SECONDS = 15", const)
        self.assertIn("MAX_LIVE_SECONDS = 180", const)

    def test_hide_camera_event_registration_and_targeting(self):
        services = (_COMPONENT / "services.py").read_text(encoding="utf-8")
        constants = (_COMPONENT / "const.py").read_text(encoding="utf-8")
        android = (pathlib.Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "java" / "com" / "example" / "tvvoiceprobe" / "TvEventsService.java").read_text(encoding="utf-8")
        self.assertIn('EVENT_HIDE_CAMERA = "kellies_voice_hide_camera"', constants)
        self.assertIn('SERVICE_HIDE_CAMERA = "hide_camera"', constants)
        self.assertIn("HIDE_CAMERA_SCHEMA", services)
        self.assertIn("_async_hide_camera", services)
        self.assertIn("configured_device_ids(call.hass)", services)
        self.assertIn("if (cameraVisible) hideCard()", android)
        self.assertIn("HIDE_CAMERA_EVENT_ID = 6", android)

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
