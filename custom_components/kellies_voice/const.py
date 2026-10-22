"""Option text shared with the Android TV app. Do not rename values."""

DOMAIN = "kellies_voice"
PLATFORMS = ["select", "switch"]

POSITIONS = [
    "Original location",
    "Top left",
    "Top right",
    "Bottom left",
    "Bottom right",
    "Center",
    "Bottom center",
]
SIZES = ["Small", "Medium", "Large"]
STYLES = ["Copper Glass", "Compact Charcoal", "Night Signal"]
HIDE_TIMES = ["Never", "5 seconds", "7 seconds", "10 seconds", "20 seconds"]

SELECTS = (
    ("position", "Overlay position", POSITIONS, "mdi:overscan"),
    ("size", "Overlay size", SIZES, "mdi:relative-scale"),
    ("style", "Overlay style", STYLES, "mdi:palette"),
    ("auto_hide", "Overlay auto-hide", HIDE_TIMES, "mdi:timer-outline"),
)

EVENT_ANNOUNCEMENT = "kellies_voice_announcement"
EVENT_CAMERA_POPUP = "kellies_voice_camera_popup"
EVENT_LIVE_CAMERA = "kellies_voice_live_camera"
SERVICE_ANNOUNCE = "announce"
SERVICE_SHOW_CAMERA = "show_camera"
SERVICE_SHOW_LIVE_CAMERA = "show_live_camera"

MAX_TITLE_LENGTH = 65
MAX_TEXT_LENGTH = 500
MIN_DURATION_SECONDS = 4
MAX_DURATION_SECONDS = 45
DEFAULT_ANNOUNCE_SECONDS = 10
DEFAULT_CAMERA_SECONDS = 20

DEFAULT_LIVE_SECONDS = 90
MIN_LIVE_SECONDS = 15
MAX_LIVE_SECONDS = 180
