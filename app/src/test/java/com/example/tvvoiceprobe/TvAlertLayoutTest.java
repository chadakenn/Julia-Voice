package com.example.tvvoiceprobe;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TvAlertLayoutTest {
    @Test public void unknownValuesStayAtTheCurrentUpperRightPopup() {
        assertEquals(TvAlertLayout.TOP_RIGHT, TvAlertLayout.position(null));
        assertEquals(TvAlertLayout.TOP_RIGHT, TvAlertLayout.position("default"));
        assertEquals(TvAlertLayout.TOP_RIGHT, TvAlertLayout.position("bottom_center"));
        assertEquals(TvAlertLayout.MEDIUM, TvAlertLayout.size(" "));
        assertEquals(TvAlertLayout.MEDIUM, TvAlertLayout.size("huge"));
        assertNull(TvAlertLayout.positionOrNull("Original location"));
        assertNull(TvAlertLayout.sizeOrNull("Medium (original) extra"));
    }

    @Test public void homeAssistantLabelsAndStoredKeysMatch() {
        assertEquals(TvAlertLayout.TOP_LEFT, TvAlertLayout.positionOrNull("Top left"));
        assertEquals(TvAlertLayout.TOP_RIGHT, TvAlertLayout.positionOrNull("top_right"));
        assertEquals(TvAlertLayout.BOTTOM_LEFT, TvAlertLayout.positionOrNull("Bottom left"));
        assertEquals(TvAlertLayout.BOTTOM_RIGHT, TvAlertLayout.positionOrNull("Bottom right"));
        assertEquals(TvAlertLayout.CENTER, TvAlertLayout.positionOrNull("Center"));
        assertEquals("Top right", TvAlertLayout.positionLabel(TvAlertLayout.TOP_RIGHT));
        assertEquals("Small", TvAlertLayout.sizeLabel("small"));
        assertEquals("Medium", TvAlertLayout.sizeLabel("medium"));
        assertEquals("Large", TvAlertLayout.sizeLabel("large"));
    }

    @Test public void mediumKeepsTodaysCardAndPictureSizes() {
        assertEquals(430, TvAlertLayout.widthDp(TvAlertLayout.MEDIUM, false, false));
        assertEquals(505, TvAlertLayout.widthDp(TvAlertLayout.MEDIUM, true, false));
        assertEquals(570, TvAlertLayout.widthDp(TvAlertLayout.MEDIUM, true, true));
        assertEquals(220, TvAlertLayout.mediaHeightDp(TvAlertLayout.MEDIUM, false));
        assertEquals(295, TvAlertLayout.mediaHeightDp(TvAlertLayout.MEDIUM, true));
        assertEquals(28, TvAlertLayout.marginXDp(TvAlertLayout.TOP_RIGHT));
        assertEquals(40, TvAlertLayout.marginYDp(TvAlertLayout.TOP_RIGHT));
        assertEquals(0, TvAlertLayout.marginXDp(TvAlertLayout.CENTER));
        assertEquals(0, TvAlertLayout.marginYDp(TvAlertLayout.CENTER));
    }

    @Test public void smallAndLargeScaleEveryPopupKindTogether() {
        assertEquals(323, TvAlertLayout.widthDp(TvAlertLayout.SMALL, false, false));
        assertEquals(538, TvAlertLayout.widthDp(TvAlertLayout.LARGE, false, false));
        assertEquals(379, TvAlertLayout.widthDp(TvAlertLayout.SMALL, true, false));
        assertEquals(428, TvAlertLayout.widthDp(TvAlertLayout.SMALL, true, true));
        assertEquals(713, TvAlertLayout.widthDp(TvAlertLayout.LARGE, true, true));
        assertEquals(165, TvAlertLayout.mediaHeightDp(TvAlertLayout.SMALL, false));
        assertEquals(369, TvAlertLayout.mediaHeightDp(TvAlertLayout.LARGE, true));
    }
}
