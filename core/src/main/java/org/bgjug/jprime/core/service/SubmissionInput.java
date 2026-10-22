package org.bgjug.jprime.core.service;

import org.bgjug.jprime.core.model.SessionLevel;
import org.bgjug.jprime.core.model.SessionType;

/**
 * A talk proposal as entered in the CFP form.
 *
 * @param coSpeaker may be null or {@link SpeakerInput#isEmpty() empty}
 */
public record SubmissionInput(String title, String description, SessionLevel level, SessionType type,
                              SpeakerInput speaker, SpeakerInput coSpeaker) {

    public boolean hasCoSpeaker() {
        return coSpeaker != null && !coSpeaker.isEmpty();
    }
}
