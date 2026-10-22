package org.bgjug.jprime.admin.api;

import java.util.List;

import io.quarkus.panache.common.Sort;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.admin.dto.Dtos.SponsorDto;
import org.bgjug.jprime.core.model.Sponsor;
import org.bgjug.jprime.core.model.SponsorPackage;
import org.bgjug.jprime.core.repository.SponsorRepository;
import org.bgjug.jprime.core.service.ThumbnailService;
import org.bgjug.jprime.core.service.ValidationFailedException;

@Path("/api/admin/sponsors")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class SponsorAdminResource {

    @Inject
    SponsorRepository sponsorRepository;

    @Inject
    ThumbnailService thumbnailService;

    @GET
    public List<SponsorDto> list() {
        return sponsorRepository.listAll(Sort.ascending("companyName")).stream().map(SponsorDto::of).toList();
    }

    @GET
    @Path("{id}")
    public SponsorDto get(@PathParam("id") long id) {
        return SponsorDto.of(AdminSupport.found(sponsorRepository.findByIdOptional(id), "Sponsor"));
    }

    @GET
    @Path("{id}/logo")
    @Produces(MediaType.WILDCARD)
    public Response logo(@PathParam("id") long id) {
        return AdminSupport.image(AdminSupport.found(sponsorRepository.findByIdOptional(id), "Sponsor").getLogo());
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public SponsorDto create(@BeanParam CompanyForm form) {
        Sponsor sponsor = new Sponsor();
        apply(sponsor, form);
        sponsorRepository.persist(sponsor);
        return SponsorDto.of(sponsor);
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public SponsorDto update(@PathParam("id") long id, @BeanParam CompanyForm form) {
        Sponsor sponsor = AdminSupport.found(sponsorRepository.findByIdOptional(id), "Sponsor");
        apply(sponsor, form);
        return SponsorDto.of(sponsor);
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") long id) {
        sponsorRepository.deleteById(id);
    }

    private void apply(Sponsor sponsor, CompanyForm form) {
        if (StringUtils.isBlank(form.companyName)) {
            throw ValidationFailedException.of("companyName", "The company name is required");
        }
        if (StringUtils.isBlank(form.email) || !form.email.contains("@")) {
            throw ValidationFailedException.of("email", "A valid e-mail is required");
        }
        sponsor.setCompanyName(form.companyName.trim());
        sponsor.setCompanyWebsite(form.companyWebsite);
        sponsor.setEmail(form.email.trim());
        sponsor.setDescription(form.description);
        sponsor.setActive(form.active);
        sponsor.setSponsorPackage(StringUtils.isBlank(form.sponsorPackage) ? null :
                                  SponsorPackage.valueOf(form.sponsorPackage));
        byte[] logo = AdminSupport.bytes(form.logo);
        if (logo != null) {
            sponsor.setLogo(form.resizeImage ? thumbnailService.logo(logo) : logo);
        }
    }
}
