package com.example.tvvoiceprobe;

import org.junit.Test;
import static org.junit.Assert.*;

public class TimerBookTest {
    @Test public void countdownExpiresAndStaysRingingUntilDismissed() {
        TimerBook book = new TimerBook();
        assertTrue(book.start("Pizza", 301000, 1000, 5000, true, "Pizza is ready."));
        assertEquals("5:00", book.entries().get(0).remaining(5000));
        assertFalse(book.tick(304999));
        assertTrue(book.tick(305000));
        assertEquals("Pizza is ready.", book.ringing().phrase);
        book.tick(99999999);
        assertNotNull(book.ringing());
        assertTrue(book.cancel(" pizza "));
        assertNull(book.ringing());
    }

    @Test public void cancelAndDismissDoNotAffectOtherRunningTimers() {
        TimerBook book = new TimerBook();
        book.start("Pizza", 2000, 1000, 1000, false, "");
        book.start("Tea", 6000, 1000, 1000, false, "");
        book.tick(2000);
        book.dismissRinging();
        assertEquals(1, book.entries().size());
        assertEquals("Tea", book.entries().get(0).name);
        assertFalse(book.cancel("missing"));
    }

    @Test public void duplicateDeliveryDoesNotResetButNewStartReplacesSameName() {
        TimerBook book = new TimerBook();
        book.start("Pizza", 2000, 1000, 1000, false, "");
        book.tick(2000);
        book.start("pizza", 2000, 1900, 1900, false, "");
        assertNotNull(book.ringing());
        book.start("PIZZA", 5000, 2000, 2000, false, "");
        assertEquals(1, book.entries().size());
        assertNull(book.ringing());
    }

    @Test public void limitsAndRestoreUseBoundedNamesAndElapsedDeadlines() {
        TimerBook book = new TimerBook();
        assertFalse(book.start("http://bad", 2000, 1000, 0, false, ""));
        assertFalse(book.start("Tea", Long.MAX_VALUE, 1000, 0, false, ""));
        for (int i=0; i<8; i++) assertTrue(book.start("Timer "+i, 6000, 1000, 4000, false, ""));
        assertFalse(book.start("Extra", 6000, 1000, 4000, false, ""));
        assertEquals("0:05", book.entries().get(0).remaining(4000));
        TimerBook restored = new TimerBook();
        restored.start("Expired", 1000, 5000, 200, false, "");
        assertNotNull(restored.ringing());
    }
}
