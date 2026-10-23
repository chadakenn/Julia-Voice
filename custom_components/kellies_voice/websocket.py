"""Read-scoped Home Assistant WebSocket alert delivery for non-admin TV users.

The command uses the existing HA-authenticated socket. A user must have read
access to this configured TV's HA layout entity. No permanent message store,
additional TV secret or elevated HA token is needed.
"""

import probatio

from homeassistant.auth.permissions.const import POLICY_READ
from homeassistant.components import websocket_api
from homeassistant.core import HomeAssistant, callback

from .alert_transport import EVENT_FIELDS, sanitize_event
from .alert_presence import receiver_connected, receiver_disconnected
from .const import DOMAIN
from .validation import clean_tv_device_id

_REGISTERED = "alert_ws_registered"
_WS_TYPE = "kellies_voice/subscribe_alerts"


@callback
def async_register_alert_subscription(hass: HomeAssistant) -> None:
    data = hass.data.setdefault(DOMAIN, {})
    if data.get(_REGISTERED):
        return
    websocket_api.async_register_command(hass, websocket_subscribe_alerts)
    data[_REGISTERED] = True


def _tv_can_read(hass: HomeAssistant, user, tv_id: str) -> bool:
    """Require both a paired TV and access to one of its actual HA entities."""
    paired = any(entry.data.get("device_id") == tv_id
                 for entry in hass.config_entries.async_entries(DOMAIN))
    if not paired or user is None:
        return False
    # Do not treat a known pairing UUID as authorization on its own.
    permissions = user.permissions
    for state in hass.states.async_all("select"):
        attrs = state.attributes
        if attrs.get("kellie_device_id") != tv_id or attrs.get("kellie_setting") != "alert_position":
            continue
        if user.is_admin or permissions.access_all_entities(POLICY_READ):
            return True
        if permissions.check_entity(state.entity_id, POLICY_READ):
            return True
    return False


@websocket_api.websocket_command({
    probatio.Required("type"): _WS_TYPE,
    probatio.Required("device_id"): str,
})
@callback
def websocket_subscribe_alerts(hass: HomeAssistant,
                                connection: websocket_api.ActiveConnection,
                                msg: dict) -> None:
    """Subscribe only to alerts for a TV this authenticated user may read."""
    try:
        device_id = clean_tv_device_id(msg["device_id"])
    except ValueError:
        connection.send_error(msg["id"], websocket_api.ERR_INVALID_FORMAT,
                              "Invalid TV pairing ID")
        return
    if not _tv_can_read(hass, connection.user, device_id):
        connection.send_error(msg["id"], websocket_api.ERR_UNAUTHORIZED,
                              "No read permission for this paired TV")
        return

    unsubscribers = []
    connection_key = object()
    @callback
    def forward(event) -> None:
        # Check permissions for every event, including after an HA role change.
        if not _tv_can_read(hass, connection.user, device_id):
            return
        payload = sanitize_event(event.event_type, event.data, device_id)
        if payload is None:
            return
        connection.send_message(websocket_api.event_message(msg["id"], {
            "event_type": event.event_type, "data": payload
        }))

    for event_type in EVENT_FIELDS:
        unsubscribers.append(hass.bus.async_listen(event_type, forward))

    @callback
    def unsubscribe() -> None:
        for remove in unsubscribers:
            remove()
        receiver_disconnected(hass, device_id, connection_key)

    connection.subscriptions[msg["id"]] = unsubscribe
    connection.send_result(msg["id"])
    # Only mark connected after successfully registering and acknowledging.
    receiver_connected(hass, device_id, connection_key)
