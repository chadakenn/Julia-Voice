"""Home Assistant services that fire TV events. They do not talk to the TV directly."""

import logging

import voluptuous as vol
from homeassistant.core import HomeAssistant, ServiceCall
from homeassistant.exceptions import ServiceValidationError
from homeassistant.helpers import config_validation as cv

from .const import (
    DEFAULT_ANNOUNCE_SECONDS,
    DEFAULT_CAMERA_SECONDS,
    DEFAULT_LIVE_SECONDS,
    DOMAIN,
    EVENT_ANNOUNCEMENT,
    EVENT_CAMERA_POPUP,
    EVENT_LIVE_CAMERA,
    EVENT_HIDE_CAMERA,
    MAX_DURATION_SECONDS,
    MAX_LIVE_SECONDS,
    MIN_LIVE_SECONDS,
    MIN_DURATION_SECONDS,
    SERVICE_ANNOUNCE,
    SERVICE_SHOW_CAMERA,
    SERVICE_SHOW_LIVE_CAMERA,
    SERVICE_HIDE_CAMERA,
)
from .validation import (
    clean_camera_entity,
    clean_duration,
    clean_text,
    clean_title,
    target_device_ids,
)

_LOGGER = logging.getLogger(__name__)
_REGISTERED = "services_registered"

ANNOUNCE_SCHEMA = vol.Schema(
    {
        vol.Required("title"): cv.string,
        vol.Required("text"): cv.string,
        vol.Optional("duration_seconds", default=DEFAULT_ANNOUNCE_SECONDS): vol.All(
            vol.Coerce(int),
            vol.Range(min=MIN_DURATION_SECONDS, max=MAX_DURATION_SECONDS),
        ),
        vol.Optional("device_id"): cv.string,
    }
)

SHOW_CAMERA_SCHEMA = vol.Schema(
    {
        vol.Required("entity_id"): cv.string,
        vol.Required("title"): cv.string,
        vol.Optional("duration_seconds", default=DEFAULT_CAMERA_SECONDS): vol.All(
            vol.Coerce(int),
            vol.Range(min=MIN_DURATION_SECONDS, max=MAX_DURATION_SECONDS),
        ),
        vol.Optional("device_id"): cv.string,
    }
)


SHOW_LIVE_SCHEMA = vol.Schema(
    {
        vol.Required("entity_id"): cv.string,
        vol.Optional("title", default="Live camera"): cv.string,
        vol.Optional("duration_seconds", default=DEFAULT_LIVE_SECONDS): vol.All(
            vol.Coerce(int), vol.Range(min=MIN_LIVE_SECONDS, max=MAX_LIVE_SECONDS)
        ),
        vol.Optional("device_id"): cv.string,
    }
)


HIDE_CAMERA_SCHEMA = vol.Schema(
    {
        vol.Optional("device_id"): cv.string,
    }
)


def async_register_services(hass: HomeAssistant) -> None:
    """Register four TV-event services once; HA layout entities remain untouched."""
    data = hass.data.setdefault(DOMAIN, {})
    if data.get(_REGISTERED):
        return
    data[_REGISTERED] = True
    hass.services.async_register(
        DOMAIN, SERVICE_ANNOUNCE, _async_announce, schema=ANNOUNCE_SCHEMA
    )
    hass.services.async_register(
        DOMAIN, SERVICE_SHOW_CAMERA, _async_show_camera, schema=SHOW_CAMERA_SCHEMA
    )
    hass.services.async_register(
        DOMAIN, SERVICE_SHOW_LIVE_CAMERA, _async_show_live_camera, schema=SHOW_LIVE_SCHEMA
    )
    hass.services.async_register(
        DOMAIN, SERVICE_HIDE_CAMERA, _async_hide_camera, schema=HIDE_CAMERA_SCHEMA
    )


def configured_device_ids(hass: HomeAssistant) -> list:
    """App device ids from Kellie's Voice config entries. Not Home Assistant tokens."""
    found = []
    for entry in hass.config_entries.async_entries(DOMAIN):
        device_id = entry.data.get("device_id")
        if isinstance(device_id, str):
            found.append(device_id)
    return found


async def _async_announce(call: ServiceCall) -> None:
    try:
        title = clean_title(call.data.get("title"))
        text = clean_text(call.data.get("text"))
        duration = clean_duration(
            call.data.get("duration_seconds", DEFAULT_ANNOUNCE_SECONDS),
            DEFAULT_ANNOUNCE_SECONDS,
        )
        targets = target_device_ids(call.data.get("device_id"), configured_device_ids(call.hass))
    except ValueError as err:
        raise ServiceValidationError(str(err)) from None
    for device_id in targets:
        call.hass.bus.async_fire(
            EVENT_ANNOUNCEMENT,
            {
                "title": title,
                "text": text,
                "duration_seconds": duration,
                "device_id": device_id,
            },
        )
    _LOGGER.debug("Fired %s for %s configured TV(s)", EVENT_ANNOUNCEMENT, len(targets))


async def _async_show_camera(call: ServiceCall) -> None:
    try:
        entity_id = clean_camera_entity(call.data.get("entity_id"))
        title = clean_title(call.data.get("title"))
        duration = clean_duration(
            call.data.get("duration_seconds", DEFAULT_CAMERA_SECONDS),
            DEFAULT_CAMERA_SECONDS,
        )
        targets = target_device_ids(call.data.get("device_id"), configured_device_ids(call.hass))
    except ValueError as err:
        raise ServiceValidationError(str(err)) from None
    if call.hass.states.get(entity_id) is None:
        raise ServiceValidationError("That camera is not in Home Assistant.")
    for device_id in targets:
        call.hass.bus.async_fire(
            EVENT_CAMERA_POPUP,
            {
                "entity_id": entity_id,
                "title": title,
                "duration_seconds": duration,
                "device_id": device_id,
            },
        )
    _LOGGER.debug("Fired %s for %s configured TV(s)", EVENT_CAMERA_POPUP, len(targets))



async def _async_show_live_camera(call: ServiceCall) -> None:
    """Request a live HLS session on a configured TV without sharing stream URLs."""
    try:
        entity_id = clean_camera_entity(call.data.get("entity_id"))
        title = clean_title(call.data.get("title", "Live camera"))
        duration = int(call.data.get("duration_seconds", DEFAULT_LIVE_SECONDS))
        if not MIN_LIVE_SECONDS <= duration <= MAX_LIVE_SECONDS:
            raise ValueError("Live duration must be from 15 to 180 seconds.")
        targets = target_device_ids(call.data.get("device_id"), configured_device_ids(call.hass))
    except (ValueError, TypeError) as err:
        raise ServiceValidationError(str(err)) from None
    camera = call.hass.states.get(entity_id)
    if camera is None:
        raise ServiceValidationError("That camera is not in Home Assistant.")
    # CameraEntityFeature.STREAM == 2; reject still-only camera entities.
    if not int(camera.attributes.get("supported_features", 0)) & 2:
        raise ServiceValidationError("This camera does not support live streams in Home Assistant.")
    for device_id in targets:
        call.hass.bus.async_fire(
            EVENT_LIVE_CAMERA,
            {
                "entity_id": entity_id,
                "title": title,
                "duration_seconds": duration,
                "device_id": device_id,
            },
        )
    _LOGGER.debug("Fired %s for %s configured TV(s)", EVENT_LIVE_CAMERA, len(targets))


async def _async_hide_camera(call: ServiceCall) -> None:
    """Close the current camera (snapshot or live) on a configured TV.

    No camera URL, credentials or arbitrary entity ids are accepted here.
    Announcements and the separate voice overlay are unaffected.
    """
    try:
        targets = target_device_ids(
            call.data.get("device_id"), configured_device_ids(call.hass)
        )
    except ValueError as err:
        raise ServiceValidationError(str(err)) from None
    for device_id in targets:
        call.hass.bus.async_fire(EVENT_HIDE_CAMERA, {"device_id": device_id})
    _LOGGER.debug("Fired %s for %s configured TV(s)", EVENT_HIDE_CAMERA, len(targets))
