"""Discovery flow. Home Assistant does not connect to the TV."""

import voluptuous as vol
from homeassistant.config_entries import ConfigFlow
from homeassistant.helpers.service_info.zeroconf import ZeroconfServiceInfo

from .const import DOMAIN
from .validation import clean_tv_device_id


def _text(properties: dict, key: str) -> str:
    value = properties.get(key)
    if isinstance(value, bytes):
        return value.decode("utf-8", "replace").strip()
    if value is None:
        return ""
    return str(value).strip()


class KellieVoiceConfigFlow(ConfigFlow, domain=DOMAIN):
    """Add a TV that is already advertising from its Settings switch."""

    def __init__(self) -> None:
        self._device_id = ""
        self._name = "Kellie's Voice"

    async def async_step_user(self, user_input=None):
        """Manual fallback when the TV cannot advertise across mDNS/VLANs."""
        errors = {}
        if user_input is not None:
            try:
                device_id = clean_tv_device_id(user_input.get("device_id"))
            except ValueError:
                errors["device_id"] = "invalid_device_id"
            else:
                name = str(user_input.get("name") or "Kellie's Voice TV").strip()
                if not name or len(name) > 64:
                    errors["name"] = "invalid_name"
                else:
                    await self.async_set_unique_id(device_id)
                    self._abort_if_unique_id_configured()
                    return self.async_create_entry(
                        title=name,
                        data={"device_id": device_id, "name": name},
                    )
        return self.async_show_form(
            step_id="user",
            data_schema=vol.Schema(
                {
                    vol.Required("device_id"): str,
                    vol.Optional("name", default="Kellie's Voice TV"): str,
                }
            ),
            errors=errors,
        )

    async def async_step_zeroconf(self, discovery_info: ZeroconfServiceInfo):
        device_id = _text(discovery_info.properties, "id")
        if not device_id:
            return self.async_abort(reason="no_id")
        await self.async_set_unique_id(device_id)
        self._abort_if_unique_id_configured()
        name = _text(discovery_info.properties, "name") or "Kellie's Voice"
        self._device_id = device_id
        self._name = name
        self.context["title_placeholders"] = {"name": name}
        return await self.async_step_confirm()

    async def async_step_confirm(self, user_input=None):
        if user_input is not None:
            return self.async_create_entry(
                title=self._name,
                data={"device_id": self._device_id, "name": self._name},
            )
        return self.async_show_form(
            step_id="confirm",
            description_placeholders={"name": self._name},
        )
