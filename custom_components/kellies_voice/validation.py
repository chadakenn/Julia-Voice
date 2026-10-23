"""Input checks for TV events. These functions do not log or store secrets."""

import math
import re
from uuid import UUID

try:
    from .const import (
        MAX_ACTION_LABEL,
        MAX_ACTIONS,
        MAX_DURATION_SECONDS,
        MAX_SPEAK_LENGTH,
        MAX_TEXT_LENGTH,
        MAX_TITLE_LENGTH,
        MIN_DURATION_SECONDS,
    )
except ImportError:  # unit test loads this file without the Home Assistant package
    from const import (  # type: ignore
        MAX_ACTION_LABEL,
        MAX_ACTIONS,
        MAX_DURATION_SECONDS,
        MAX_SPEAK_LENGTH,
        MAX_TEXT_LENGTH,
        MAX_TITLE_LENGTH,
        MIN_DURATION_SECONDS,
    )

_CAMERA_ENTITY = re.compile(r"^camera\.[a-z0-9_]+$")
_ACTION_ID = re.compile(r"^[a-z][a-z0-9_]{0,31}$")


def clean_tv_device_id(value) -> str:
    """Normalise the UUID shown on the TV; reject malformed/missing IDs."""
    if not isinstance(value, str):
        raise ValueError("Enter the device ID shown on your TV.")
    try:
        parsed = UUID(value.strip())
    except (ValueError, AttributeError):
        raise ValueError("Enter a valid TV device ID.") from None
    if parsed.version != 4 or parsed.variant != "specified in RFC 4122":
        raise ValueError("Enter a valid TV device ID.")
    return str(parsed)



def clean_title(value) -> str:
    """Return a short heading, or raise ValueError."""
    title = _clean_line(value)
    if not title or len(title) > MAX_TITLE_LENGTH:
        raise ValueError(f"Title must be 1 to {MAX_TITLE_LENGTH} characters.")
    return title


def clean_text(value) -> str:
    """Return announcement body text, or raise ValueError."""
    if value is None:
        raise ValueError(f"Text must be 1 to {MAX_TEXT_LENGTH} characters.")
    text = str(value).strip()
    if not text or len(text) > MAX_TEXT_LENGTH:
        raise ValueError(f"Text must be 1 to {MAX_TEXT_LENGTH} characters.")
    return text


def clean_duration(value, default: int) -> int:
    """Return a duration inside the allowed cap."""
    if value is None or value == "":
        seconds = default
    else:
        try:
            seconds = int(value)
        except (TypeError, ValueError):
            raise ValueError(
                f"Duration must be a whole number of seconds from {MIN_DURATION_SECONDS} to {MAX_DURATION_SECONDS}."
            ) from None
    if seconds < MIN_DURATION_SECONDS or seconds > MAX_DURATION_SECONDS:
        raise ValueError(
            f"Duration must be from {MIN_DURATION_SECONDS} to {MAX_DURATION_SECONDS} seconds."
        )
    return seconds


def clean_camera_entity(value) -> str:
    """Accept only a Home Assistant camera entity id. Never a URL or token."""
    if value is None:
        raise ValueError("Choose a camera entity, such as camera.front_door.")
    entity_id = str(value).strip().lower()
    if not _CAMERA_ENTITY.fullmatch(entity_id):
        raise ValueError("Choose a camera entity, such as camera.front_door.")
    return entity_id


def clean_speak(value) -> str:
    """Optional spoken line. Empty means stay quiet. Never a web address."""
    if value is None:
        return ""
    text = " ".join(str(value).split())
    if not text:
        return ""
    if len(text) > MAX_SPEAK_LENGTH:
        raise ValueError(f"Spoken text must be at most {MAX_SPEAK_LENGTH} characters.")
    lowered = text.lower()
    if "://" in text or lowered.startswith("http") or "www." in lowered:
        raise ValueError("Spoken text cannot be a web address.")
    return text


def clean_actions(value) -> list:
    """At most three buttons. Each id is a short word such as unlock or talk."""
    if value is None or value == "":
        return []
    if not isinstance(value, list):
        raise ValueError("Actions must be a list of id and label.")
    if len(value) > MAX_ACTIONS:
        raise ValueError(f"Use at most {MAX_ACTIONS} actions.")
    cleaned = []
    seen = set()
    for item in value:
        if not isinstance(item, dict):
            raise ValueError("Each action needs an id and a label.")
        action_id = str(item.get("id", "")).strip().lower()
        label = _clean_line(item.get("label"))
        if not _ACTION_ID.fullmatch(action_id):
            raise ValueError("Action id must be a short lowercase word, such as unlock.")
        if not label or len(label) > MAX_ACTION_LABEL:
            raise ValueError(f"Action label must be 1 to {MAX_ACTION_LABEL} characters.")
        if action_id in seen:
            raise ValueError("Each action id can be used once.")
        seen.add(action_id)
        cleaned.append({"id": action_id, "label": label})
    return cleaned


def target_device_ids(requested, configured) -> list:
    """One configured TV, or every configured TV when no id is given."""
    known = []
    seen = set()
    for item in configured or []:
        if not isinstance(item, str):
            continue
        device_id = item.strip()
        if device_id and device_id not in seen:
            seen.add(device_id)
            known.append(device_id)
    if not known:
        raise ValueError("No Kellie's Voice TVs are configured.")
    if requested is None:
        return known
    chosen = str(requested).strip()
    if not chosen:
        return known
    if chosen not in seen:
        raise ValueError("That TV is not a configured Kellie's Voice device.")
    return [chosen]


_WEATHER_ENTITY = re.compile(r"^weather\.[a-z0-9_]+$")


def clean_timer_name(value):
    """Named timer keys match the TV's ASCII, case-insensitive rules."""
    if not isinstance(value, str):
        raise ValueError("Use a timer name from 1 to 40 letters, digits, spaces, hyphens or underscores.")
    name = " ".join(value.split())
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9 _-]{0,39}", name):
        raise ValueError("Use a timer name from 1 to 40 letters, digits, spaces, hyphens or underscores.")
    return name


def clean_timer_seconds(value):
    if isinstance(value, bool):
        raise ValueError("Timer duration must be 1 to 86400 whole seconds.")
    try:
        seconds = int(value)
        if float(value) != seconds or not 1 <= seconds <= 86400:
            raise ValueError
    except (ValueError, TypeError, OverflowError):
        raise ValueError("Timer duration must be 1 to 86400 whole seconds.") from None
    return seconds


def clean_timer_phrase(value):
    if not isinstance(value, str):
        raise ValueError("Alert phrase must be text.")
    text = " ".join(value.split())
    if len(text) > 140 or "://" in text:
        raise ValueError("Alert phrase must be at most 140 characters, without a URL.")
    return text

_CONDITIONS = {
    "clear-night", "cloudy", "exceptional", "fog", "hail", "lightning",
    "lightning-rainy", "partlycloudy", "pouring", "rainy", "snowy",
    "snowy-rainy", "sunny", "windy", "windy-variant",
}


def clean_weather_entity(value) -> str:
    """Accept only a Home Assistant weather entity id."""
    if value is None:
        raise ValueError("Choose a weather entity, such as weather.forecast_home.")
    entity_id = str(value).strip().lower()
    if not _WEATHER_ENTITY.fullmatch(entity_id):
        raise ValueError("Choose a weather entity, such as weather.forecast_home.")
    return entity_id


def clean_forecast_duration(value) -> int:
    """Forecast cards stay up long enough to read, and no longer than 45 seconds."""
    if value is None or value == "":
        return 20
    try:
        seconds = int(value)
    except (TypeError, ValueError):
        raise ValueError("Forecast duration must be from 8 to 45 seconds.") from None
    if seconds < 8 or seconds > 45:
        raise ValueError("Forecast duration must be from 8 to 45 seconds.")
    return seconds


def clean_forecast_place(value) -> str:
    """A short label from the weather entity name. Never a URL."""
    text = _clean_line(value)
    if "://" in text or text.lower().startswith("http"):
        return "Forecast"
    if not text:
        return "Forecast"
    return text[:40]


def clean_forecast_unit(value) -> str:
    if value in ("°C", "°F", "C", "F"):
        return "°C" if value in ("°C", "C") else "°F"
    return "°"


def clean_forecast_days(raw_days, tz_name="UTC") -> list:
    """Keep at most seven usable days. Drop a day that has no temperature."""
    if not isinstance(raw_days, list):
        raise ValueError("That weather entity returned no daily forecast.")
    days = []
    for raw in raw_days:
        if len(days) >= 7:
            break
        day = _forecast_day(raw, tz_name)
        if day is not None:
            days.append(day)
    if not days:
        raise ValueError("That weather entity returned no usable daily forecast.")
    return days


def forecast_rows(response, entity_id) -> list:
    """Read the weather.get_forecasts response without assuming extra wrappers."""
    if not isinstance(response, dict):
        return []
    block = response.get(entity_id)
    if isinstance(block, dict):
        rows = block.get("forecast")
        return rows if isinstance(rows, list) else []
    return []


def _forecast_day(raw, tz_name) -> dict:
    if not isinstance(raw, dict):
        return None
    label = _forecast_label(raw.get("datetime"), tz_name)
    if label is None:
        return None
    high = _forecast_temp(raw.get("temperature"))
    low = _forecast_temp(raw.get("templow"))
    if high is None and low is None:
        return None
    condition = raw.get("condition")
    if not isinstance(condition, str) or condition not in _CONDITIONS:
        condition = "unknown"
    day = {"label": label, "condition": condition, "high": high, "low": low}
    precip = _forecast_precip(raw.get("precipitation_probability"))
    if precip is not None:
        day["precipitation"] = precip
    return day


def _forecast_label(value, tz_name) -> str:
    if not isinstance(value, str) or "://" in value:
        return None
    text = value.strip()
    if not text or len(text) > 40:
        return None
    try:
        from datetime import datetime
        from zoneinfo import ZoneInfo
        parsed = datetime.fromisoformat(text.replace("Z", "+00:00"))
        if parsed.tzinfo is None:
            parsed = parsed.replace(tzinfo=ZoneInfo("UTC"))
        zone = ZoneInfo(tz_name or "UTC")
        local = parsed.astimezone(zone)
    except Exception:
        return None
    return f"{local.strftime('%a')} {local.day}"


def _forecast_temp(value):
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        return None
    if not -80 <= value <= 140 or not math.isfinite(value):
        return None
    rounded = int(round(value))
    if rounded < -80 or rounded > 140:
        return None
    return rounded


def _forecast_precip(value):
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        return None
    if not 0 <= value <= 100 or not math.isfinite(value):
        return None
    rounded = int(round(value))
    if rounded < 0 or rounded > 100:
        return None
    return rounded


def _clean_line(value) -> str:
    if value is None:
        return ""
    return " ".join(str(value).split())
