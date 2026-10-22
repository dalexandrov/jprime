package org.bgjug.jprime.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpeakerTest {

    @Test
    void normalisesTheXHandle() {
        Speaker speaker = new Speaker();
        for (String value : new String[] {"@duke", "duke", "https://x.com/duke", "https://twitter.com/duke", "x.com/duke"}) {
            speaker.setTwitter(value);
            assertEquals("duke", speaker.getTwitterHandle(), value);
        }
        speaker.setTwitter("https://example.com/some/path");
        assertNull(speaker.getTwitterHandle());
        speaker.setTwitter(" ");
        assertNull(speaker.getTwitterHandle());
    }

    @Test
    void flagsAreComputedPerBranch() {
        Branch current = new Branch(2026);
        Branch previous = new Branch(2025);
        Speaker speaker = new Speaker("Duke", "Java", "duke@example.com", null, null);
        speaker.setId(1L);
        Submission old = submission(speaker, previous, SubmissionStatus.CONFIRMED, true);
        Submission now = submission(speaker, current, SubmissionStatus.ACCEPTED, false);
        speaker.getSubmissions().add(old);
        speaker.getSubmissions().add(now);

        speaker.updateFlags(current);

        assertFalse(speaker.isFeatured());
        assertEquals(SubmissionStatus.ACCEPTED, speaker.getStatus());
        assertEquals(1, speaker.getNumberOfSubmissions());
        assertEquals("Speaker", speaker.getSpeakerType());
        assertEquals(2026, speaker.getLastBranch().getYear());

        speaker.updateFlags(previous);
        assertTrue(speaker.isFeatured());
        assertEquals(SubmissionStatus.CONFIRMED, speaker.getStatus());
    }

    private static Submission submission(Speaker speaker, Branch branch, SubmissionStatus status, boolean featured) {
        Submission s = new Submission(branch);
        s.setTitle("Talk " + branch.getYear());
        s.setSpeaker(speaker);
        s.setStatus(status);
        s.setFeatured(featured);
        return s;
    }
}
