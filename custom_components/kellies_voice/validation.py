"""Input checks for TV events. These functions do not log or store secrets."""

import re
from uuid import UUID

try:
    from .const import (
        MAX_DURATION_SECONDS,
        MAX_TEXT_LENGTH,
        MAX_TITLE_LENGTH,
        MIN_DURATION_SECONDS,
    )
except ImportError:  # unit test loads this file without the Home Assistant package
    from const import (  # type: ignore
        MAX_DURATION_SECONDS,
        MAX_TEXT_LENGTH,
        MAX_TITLE_LENGTH,
        MIN_DURATION_SECONDS,
    )

_CAMERA_ENTITY = re.compile(r"^camera\.[a-z0-9_]+$")


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


def _clean_line(value) -> str:
    if value is None:
        return ""
    return " ".join(str(value).split())
