package com.example.tvvoiceprobe;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ForecastCardTest {
    @Test public void sevenDaysRenderAndAnEighthIsDropped() {
        List<ForecastCard.Day> days = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            days.add(ForecastCard.day("Day " + i, i == 3 ? "not-a-url" : "sunny", "70", "50", "10"));
        }
        ForecastCard card = ForecastCard.card("Home", "°F", 20, days);
        assertEquals(7, card.days.size());
        assertEquals("7 day forecast", card.heading());
        assertTrue(card.days.get(0).line("°F").contains("70°F / 50°F"));
        assertTrue(card.days.get(0).line("°F").contains("10%"));
        assertEquals("•", ForecastCard.glyph(card.days.get(3).condition));
    }

    @Test public void missingPrecipitationAndMissingTemperaturesAreSafe() {
        ForecastCard.Day partial = ForecastCard.day("Sat 24", "rainy", "61", null, null);
        assertFalse(partial.line("°C").contains("%"));
        assertTrue(partial.line("°C").contains("--"));
        assertNull(ForecastCard.day("Sat", "sunny", null, null, "10"));
        assertNull(ForecastCard.day("http://weather.example/day", "sunny", "1", "0", "1"));
        assertNull(ForecastCard.card("Home", "°F", 20, new ArrayList<>()));
    }

    @Test public void staleOrOversizedPayloadsDoNotDisplay() {
        assertFalse(ForecastCard.fresh(1_000, 1_200));
        assertTrue(ForecastCard.fresh(1_000, 1_050));
        assertTrue(ForecastCard.fresh(0, 9_999));
        ForecastCard.Day hot = ForecastCard.day("Sun", "sunny", "400", "10", "200");
        assertNull(hot.high);
        assertNull(hot.precipitation);
        ForecastCard card = ForecastCard.card("https://secret.example/home", "K", 99,
                java.util.Collections.singletonList(ForecastCard.day("Mon 1", "sunny", "68", "40", "0")));
        assertEquals("Forecast", card.location);
        assertEquals("°", card.units);
        assertEquals(20, card.seconds);
    }
}
