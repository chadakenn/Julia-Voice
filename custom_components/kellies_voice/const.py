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
