package com.example.tvvoiceprobe;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AnswerStreamTest {
    @Test public void chunksAccumulateIntoTheFullText() {
        AnswerStream stream = new AnswerStream();
        assertEquals("The ", stream.append(true, "The "));
        assertEquals("The kitchen ", stream.append(false, "kitchen "));
        assertEquals("The kitchen light is off.", stream.append(false, "light is off."));
    }

    @Test public void emptyOrMissingChunksChangeNothing() {
        AnswerStream stream = new AnswerStream();
        assertNull(stream.append(false, null));
        assertNull(stream.append(false, ""));
        stream.append(false, "Hi");
        assertNull(stream.append(false, ""));
    }

    @Test public void aNewMessageReplacesTheOldOne() {
        AnswerStream stream = new AnswerStream();
        stream.append(true, "Let me check.");
        // The reply after a tool call starts fresh instead of being appended.
        assertEquals("", stream.append(true, null));
        assertEquals("It is 72 degrees.", stream.append(false, "It is 72 degrees."));
    }

    @Test public void aNewMessageWithNothingBeforeItIsNotAChange() {
        assertNull(new AnswerStream().append(true, null));
    }

    @Test public void resetStartsOver() {
        AnswerStream stream = new AnswerStream();
        stream.append(false, "old");
        stream.reset();
        assertEquals("new", stream.append(false, "new"));
    }

    @Test public void growthIsBounded() {
        AnswerStream stream = new AnswerStream();
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < AnswerStream.MAX_CHARS + 500; i++) big.append('x');
        assertEquals(AnswerStream.MAX_CHARS, stream.append(false, big.toString()).length());
        assertNull(stream.append(false, "more"));
    }
}
