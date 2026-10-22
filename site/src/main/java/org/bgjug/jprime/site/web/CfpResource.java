package org.bgjug.jprime.site.web;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.SessionLevel;
import org.bgjug.jprime.core.model.SessionType;
import org.bgjug.jprime.core.model.Submission;
import org.bgjug.jprime.core.service.BranchService;
import org.bgjug.jprime.core.service.CfpService;
import org.bgjug.jprime.core.service.DateFormats;
import org.bgjug.jprime.core.service.ValidationFailedException;
import org.jboss.logging.Logger;

@Path("/cfp")
@Produces(MediaType.TEXT_HTML)
public class CfpResource {

    private static final Logger LOG = Logger.getLogger(CfpResource.class);

    @Inject
    @Location("cfp.html")
    Template cfp;

    @Inject
    @Location("cfp-closed.html")
    Template cfpClosed;

    @Inject
    @Location("cfp-thank-you.html")
    Template cfpThankYou;

    @Inject
    @Location("cfp-problem.html")
    Template cfpProblem;

    @Inject
    BranchService branchService;

    @Inject
    CfpService cfpService;

    @Inject
    CaptchaService captchaService;

    @GET
    public String form() {
        return render(new CfpForm(), Map.of());
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response submit(@BeanParam CfpForm form, @CookieParam(CaptchaService.COOKIE) String captchaCookie) {
        Branch branch = branchService.getCurrentBranch();
        if (!branch.isCfpOpen()) {
            return Response.ok(render(form, Map.of())).build();
        }
        if (!captchaService.verify(captchaCookie, form.captcha)) {
            return Response.ok(render(form, Map.of("captcha", "Invalid captcha text, please enter the sum."))).build();
        }
        Submission submission;
        try {
            submission = cfpService.submit(form.toInput(), branch);
        } catch (ValidationFailedException e) {
            return Response.ok(render(form, e.getErrors())).build();
        } catch (RuntimeException e) {
            LOG.error("Can't save the submission", e);
            return Response.seeOther(URI.create("/cfp/problem")).build();
        }
        try {
            cfpService.sendNotificationEmails(submission.getId());
        } catch (RuntimeException e) {
            LOG.error("Could not send confirmation email", e);
        }
        return Response.seeOther(URI.create("/cfp/thank-you")).build();
    }

    private String render(CfpForm form, Map<String, String> errors) {
        Branch branch = branchService.getCurrentBranch();
        if (!branch.isCfpOpen()) {
            return cfpClosed.data("agendaPublished", branch.isAgendaPublished()).render();
        }
        return cfp.data("form", form)
            .data("errors", new HashMap<>(errors))
            .data("speakerErrors", errorsOf(errors, "speaker."))
            .data("coSpeakerErrors", errorsOf(errors, "coSpeaker."))
            .data("levels", SessionLevel.values())
            .data("types", SessionType.values())
            .data("conferenceDates", DateFormats.conferenceDates(branch.getStartDate()))
            .data("cfpCloseDate", DateFormats.withMonth(branch.getCfpCloseDate()))
            .render();
    }

    /** "speaker.email" -> "email" for the given role prefix. */
    static Map<String, String> errorsOf(Map<String, String> errors, String prefix) {
        Map<String, String> result = new HashMap<>();
        errors.forEach((k, v) -> {
            if (k.startsWith(prefix)) {
                result.put(k.substring(prefix.length()), v);
            }
        });
        return result;
    }

    @GET
    @Path("thank-you")
    public String thankYou() {
        return cfpThankYou.data("cfpCloseDate", DateFormats.withMonth(branchService.getCurrentBranch().getCfpCloseDate()))
            .render();
    }

    @GET
    @Path("problem")
    public String problem() {
        return cfpProblem.render();
    }
}
