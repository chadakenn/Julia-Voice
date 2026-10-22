"""Layout selects. A generation of 0 means the TV has not adopted the entity yet."""

from homeassistant.components.select import SelectEntity
from homeassistant.config_entries import ConfigEntry
from homeassistant.core import HomeAssistant
from homeassistant.helpers.entity import DeviceInfo, EntityCategory
from homeassistant.helpers.entity_platform import AddEntitiesCallback
from homeassistant.helpers.restore_state import RestoreEntity

from .const import ALERT_SELECTS, DOMAIN, NOISE_SELECTS, SELECTS


async def async_setup_entry(
    hass: HomeAssistant,
    entry: ConfigEntry,
    async_add_entities: AddEntitiesCallback,
) -> None:
    entities = [
        KellieSelect(entry, setting, name, options, icon)
        for setting, name, options, icon in SELECTS
    ]
    entities.extend(
        KellieSelect(entry, setting, name, options, icon, initial)
        for setting, name, options, icon, initial in ALERT_SELECTS
    )
    entities.extend(
        KellieSelect(entry, setting, name, options, icon, initial)
        for setting, name, options, icon, initial in NOISE_SELECTS
    )
    async_add_entities(entities)


class KellieSelect(SelectEntity, RestoreEntity):
    _attr_should_poll = False
    _attr_has_entity_name = True
    _attr_entity_category = EntityCategory.CONFIG

    def __init__(self, entry: ConfigEntry, setting: str, name: str, options: list, icon: str,
                 initial=None):
        self._setting = setting
        self._device_id = entry.data["device_id"]
        self._generation = 0
        self._attr_name = name
        self._attr_unique_id = f"{self._device_id}_{setting}"
        self._attr_options = list(options)
        self._attr_current_option = initial if initial in self._attr_options else self._attr_options[0]
        self._attr_icon = icon
        self._attr_device_info = DeviceInfo(
            identifiers={(DOMAIN, self._device_id)},
            name=entry.data.get("name", "Kellie's Voice"),
            manufacturer="Kellie's Voice",
            model="Android TV",
        )
        self._attr_extra_state_attributes = self._attributes()

    def _attributes(self) -> dict:
        return {
            "kellie_device_id": self._device_id,
            "kellie_setting": self._setting,
            "kellie_generation": self._generation,
        }

    async def async_added_to_hass(self) -> None:
        await super().async_added_to_hass()
        last = await self.async_get_last_state()
        if last is None:
            return
        if last.state in self._attr_options:
            self._attr_current_option = last.state
        try:
            self._generation = int(last.attributes.get("kellie_generation", 0))
        except (TypeError, ValueError):
            self._generation = 0
        self._attr_extra_state_attributes = self._attributes()

    async def async_select_option(self, option: str) -> None:
        if option not in self._attr_options:
            return
        self._attr_current_option = option
        self._generation += 1
        self._attr_extra_state_attributes = self._attributes()
        self.async_write_ha_state()
