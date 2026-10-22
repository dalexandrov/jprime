package org.bgjug.jprime.site.web;

import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.core.model.SessionLevel;
import org.bgjug.jprime.core.model.SessionType;
import org.bgjug.jprime.core.service.SpeakerInput;
import org.bgjug.jprime.core.service.SubmissionInput;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

/** The CFP form (multipart). Field names are used in cfp.html. */
public class CfpForm {

    @RestForm
    public String title;
    @RestForm
    public String description;
    @RestForm
    public String level;
    @RestForm
    public String type;

    @RestForm
    public String speakerFirstName;
    @RestForm
    public String speakerLastName;
    @RestForm
    public String speakerEmail;
    @RestForm
    public String speakerHeadline;
    @RestForm
    public String speakerTwitter;
    @RestForm
    public String speakerBsky;
    @RestForm
    public String speakerBio;
    @RestForm
    public FileUpload speakerImage;

    @RestForm
    public boolean withCoSpeaker;
    @RestForm
    public String coSpeakerFirstName;
    @RestForm
    public String coSpeakerLastName;
    @RestForm
    public String coSpeakerEmail;
    @RestForm
    public String coSpeakerHeadline;
    @RestForm
    public String coSpeakerTwitter;
    @RestForm
    public String coSpeakerBsky;
    @RestForm
    public String coSpeakerBio;
    @RestForm
    public FileUpload coSpeakerImage;

    @RestForm
    public String captcha;

    public SubmissionInput toInput() {
        SpeakerInput speaker = new SpeakerInput(speakerFirstName, speakerLastName, speakerEmail, speakerHeadline,
            speakerTwitter, speakerBsky, speakerBio, Uploads.bytes(speakerImage));
        SpeakerInput coSpeaker = withCoSpeaker ?
                                 new SpeakerInput(coSpeakerFirstName, coSpeakerLastName, coSpeakerEmail,
                                     coSpeakerHeadline, coSpeakerTwitter, coSpeakerBsky, coSpeakerBio,
                                     Uploads.bytes(coSpeakerImage)) : null;
        return new SubmissionInput(title, description, parse(SessionLevel.class, level, SessionLevel.BEGINNER),
            parse(SessionType.class, type, SessionType.CONFERENCE_SESSION), speaker, coSpeaker);
    }

    static <E extends Enum<E>> E parse(Class<E> type, String value, E fallback) {
        if (StringUtils.isBlank(value)) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
