package org.bgjug.jprime.admin.api;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.admin.dto.Dtos.Page;
import org.bgjug.jprime.admin.dto.Dtos.SpeakerDto;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Speaker;
import org.bgjug.jprime.core.repository.SpeakerRepository;
import org.bgjug.jprime.core.service.BranchService;
import org.bgjug.jprime.core.service.SpeakerService;
import org.bgjug.jprime.core.service.ThumbnailService;
import org.bgjug.jprime.core.service.ValidationFailedException;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/api/admin/speakers")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class SpeakerAdminResource {

    @Inject
    SpeakerRepository speakerRepository;

    @Inject
    SpeakerService speakerService;

    @Inject
    BranchService branchService;

    @Inject
    ThumbnailService thumbnailService;

    public static class SpeakerForm {
        @RestForm
        public String firstName;
        @RestForm
        public String lastName;
        @RestForm
        public String email;
        @RestForm
        public String phone;
        @RestForm
        public String headline;
        @RestForm
        public String twitter;
        @RestForm
        public String bsky;
        @RestForm
        public String bio;
        /** Resize to 280x326 (default true). */
        @RestForm
        @DefaultValue("true")
        public boolean resizeImage;
        @RestForm
        public FileUpload picture;
    }

    /**
     * @param year when given, only speakers with submissions in that year (and the flags are computed for it)
     */
    @GET
    @Transactional
    public Page<SpeakerDto> list(@QueryParam("year") Integer year, @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("20") int size) {
        Branch branch = year != null ? branchService.findByYear(year) : null;
        return Page.of(speakerService.page(branch, page, size).map(SpeakerDto::of));
    }

    @GET
    @Path("{id}")
    @Transactional
    public SpeakerDto get(@PathParam("id") long id) {
        Speaker speaker = AdminSupport.found(speakerRepository.findByIdOptional(id), "Speaker");
        return SpeakerDto.of(speaker.updateFlags(branchService.getCurrentBranch()));
    }

    @GET
    @Path("{id}/picture")
    @Produces(MediaType.WILDCARD)
    @Transactional
    public Response picture(@PathParam("id") long id) {
        return AdminSupport.image(AdminSupport.found(speakerRepository.findByIdOptional(id), "Speaker").getPicture());
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public SpeakerDto create(@BeanParam SpeakerForm form) {
        if (StringUtils.isNotBlank(form.email) && speakerRepository.findByEmail(form.email.trim()).isPresent()) {
            throw ValidationFailedException.of("email", "A speaker with this e-mail already exists");
        }
        Speaker speaker = new Speaker();
        apply(speaker, form);
        speakerRepository.persist(speaker);
        return SpeakerDto.of(speaker.updateFlags(branchService.getCurrentBranch()));
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public SpeakerDto update(@PathParam("id") long id, @BeanParam SpeakerForm form) {
        Speaker speaker = AdminSupport.found(speakerRepository.findByIdOptional(id), "Speaker");
        apply(speaker, form);
        return SpeakerDto.of(speaker.updateFlags(branchService.getCurrentBranch()));
    }

    /** Deletes the speaker together with his/her submissions (as before). */
    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") long id) {
        speakerRepository.findByIdOptional(id).ifPresent(speakerRepository::delete);
    }

    private void apply(Speaker speaker, SpeakerForm form) {
        if (StringUtils.isBlank(form.email) || !form.email.contains("@")) {
            throw ValidationFailedException.of("email", "A valid e-mail is required");
        }
        speaker.setFirstName(form.firstName);
        speaker.setLastName(form.lastName);
        speaker.setEmail(form.email.trim());
        speaker.setPhone(form.phone);
        speaker.setHeadline(form.headline);
        speaker.setTwitter(StringUtils.removeStart(StringUtils.trimToNull(form.twitter), "@"));
        speaker.setBsky(StringUtils.trimToNull(form.bsky));
        speaker.setBio(form.bio);
        byte[] picture = AdminSupport.bytes(form.picture);
        if (picture != null) {
            speaker.setPicture(form.resizeImage ? thumbnailService.speakerPicture(picture) : picture);
        }
    }
}
