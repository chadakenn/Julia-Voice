"""Checks for announcement and camera event inputs. No Home Assistant install required."""

import importlib.util
import ast
import json
import pathlib
import uuid
import unittest
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock

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


class ForecastDeliveryTests(unittest.IsolatedAsyncioTestCase):
    """Exercise the real service handler with fake HA I/O, without installing HA."""

    async def asyncSetUp(self):
        import logging
        import time

        source = ast.parse((_COMPONENT / "services.py").read_text(encoding="utf-8"))
        # Keep production functions; replace only HA imports and external I/O.
        functions = [node for node in source.body
                     if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef))
                     and node.name in ("configured_device_ids", "_saved_weather_entity",
                                       "_async_show_forecast")]
        namespace = dict(vars(validation))
        namespace.update(HomeAssistant=object, ServiceCall=object,
                         ServiceValidationError=ValueError, DOMAIN=const.DOMAIN,
                         EVENT_FORECAST=const.EVENT_FORECAST, time=time,
                         _LOGGER=logging.getLogger(__name__))
        exec(compile(ast.Module(body=functions, type_ignores=[]), "services.py", "exec"), namespace)
        self.handler = namespace["_async_show_forecast"]
        self.hass = SimpleNamespace(
            config_entries=SimpleNamespace(async_entries=lambda domain: [
                SimpleNamespace(data={"device_id": "living-room"})]),
            config=SimpleNamespace(time_zone="UTC"),
            states=SimpleNamespace(get=lambda entity: SimpleNamespace(
                attributes={"supported_features": 1, "temperature_unit": "°F",
                            "friendly_name": "Home"})),
            services=SimpleNamespace(async_call=AsyncMock(return_value={
                "weather.home": {"forecast": [{"datetime": "2026-10-24T12:00:00Z",
                                               "temperature": 70, "templow": 50}]}})),
            bus=SimpleNamespace(async_fire=Mock()))

    async def test_omitted_duration_preserves_tv_preference(self):
        await self.handler(SimpleNamespace(hass=self.hass, data={"entity_id": "weather.home"}))
        event, payload = self.hass.bus.async_fire.call_args.args
        self.assertEqual(const.EVENT_FORECAST, event)
        self.assertEqual("living-room", payload["device_id"])
        self.assertNotIn("duration_seconds", payload)
        self.assertEqual(1, len(payload["days"]))

    async def test_explicit_duration_overrides_tv_preference(self):
        await self.handler(SimpleNamespace(hass=self.hass, data={
            "entity_id": "weather.home", "duration_seconds": 30}))
        self.assertEqual(30, self.hass.bus.async_fire.call_args.args[1]["duration_seconds"])


class ValidationTests(unittest.TestCase):
    def test_non_finite_forecast_values_do_not_abort_valid_days(self):
        for bad in (float("nan"), float("inf"), -float("inf"), 10 ** 1000):
            with self.subTest(value=str(bad)):
                days = validation.clean_forecast_days([{
                    "datetime": "2026-10-24T12:00:00+00:00",
                    "temperature": bad,
                    "templow": 50,
                    "precipitation_probability": bad,
                }], "UTC")
                self.assertIsNone(days[0]["high"])
                self.assertEqual(50, days[0]["low"])
                self.assertNotIn("precipitation", days[0])

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

    def test_microphone_sensitivity_defaults_to_normal(self):
        setting, name, options, icon, initial = const.NOISE_SELECTS[0]
        self.assertEqual(setting, "noise")
        self.assertEqual(name, "Microphone sensitivity")
        self.assertEqual(options, ["Quiet room", "Normal", "TV on"])
        self.assertEqual(initial, "Normal")
        self.assertNotIn("noise", [item[0] for item in const.SELECTS])
        self.assertNotIn("on", options)

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

    def test_forecast_keeps_seven_days_and_drops_bad_fields(self):
        rows = []
        for day in range(1, 10):
            rows.append({
                "datetime": f"2026-10-{day:02d}T12:00:00+00:00",
                "condition": "sunny" if day != 2 else "http://nope.example/icon",
                "temperature": 70 if day != 3 else None,
                "templow": 48 if day != 3 else None,
                "precipitation_probability": 10 if day != 4 else 250,
            })
        cleaned = validation.clean_forecast_days(rows, "America/New_York")
        self.assertEqual(7, len(cleaned))
        self.assertEqual("unknown", cleaned[1]["condition"])
        self.assertNotIn("precipitation", cleaned[2])
        self.assertNotIn("://", str(cleaned))
        self.assertEqual("°F", validation.clean_forecast_unit("F"))
        self.assertEqual("Forecast", validation.clean_forecast_place("https://home.example/secret"))
        with self.assertRaises(ValueError):
            validation.clean_forecast_days([], "UTC")
        with self.assertRaises(ValueError):
            validation.clean_weather_entity("camera.door")
        self.assertEqual([], validation.forecast_rows({"weather.home": {}}, "weather.home"))
        self.assertEqual(20, validation.clean_forecast_duration(None))
        with self.assertRaises(ValueError):
            validation.clean_forecast_duration(90)

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
