"""Kellie's Voice layout controls and TV event services."""

from homeassistant.config_entries import ConfigEntry
from homeassistant.core import HomeAssistant

from .const import PLATFORMS
from .services import async_register_services


async def async_setup(hass: HomeAssistant, config: dict) -> bool:
    async_register_services(hass)
    return True


async def async_setup_entry(hass: HomeAssistant, entry: ConfigEntry) -> bool:
    async_register_services(hass)
    await hass.config_entries.async_forward_entry_setups(entry, PLATFORMS)
    return True


async def async_unload_entry(hass: HomeAssistant, entry: ConfigEntry) -> bool:
    return await hass.config_entries.async_unload_platforms(entry, PLATFORMS)
