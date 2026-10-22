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
import org.bgjug.jprime.admin.dto.Dtos.PartnerDto;
import org.bgjug.jprime.core.model.Partner;
import org.bgjug.jprime.core.model.PartnerPackage;
import org.bgjug.jprime.core.repository.PartnerRepository;
import org.bgjug.jprime.core.service.ThumbnailService;
import org.bgjug.jprime.core.service.ValidationFailedException;

@Path("/api/admin/partners")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class PartnerAdminResource {

    @Inject
    PartnerRepository partnerRepository;

    @Inject
    ThumbnailService thumbnailService;

    @GET
    public List<PartnerDto> list() {
        return partnerRepository.listAll(Sort.ascending("companyName")).stream().map(PartnerDto::of).toList();
    }

    @GET
    @Path("{id}")
    public PartnerDto get(@PathParam("id") long id) {
        return PartnerDto.of(AdminSupport.found(partnerRepository.findByIdOptional(id), "Partner"));
    }

    @GET
    @Path("{id}/logo")
    @Produces(MediaType.WILDCARD)
    public Response logo(@PathParam("id") long id) {
        return AdminSupport.image(AdminSupport.found(partnerRepository.findByIdOptional(id), "Partner").getLogo());
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public PartnerDto create(@BeanParam CompanyForm form) {
        Partner partner = new Partner();
        apply(partner, form);
        partnerRepository.persist(partner);
        return PartnerDto.of(partner);
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public PartnerDto update(@PathParam("id") long id, @BeanParam CompanyForm form) {
        Partner partner = AdminSupport.found(partnerRepository.findByIdOptional(id), "Partner");
        apply(partner, form);
        return PartnerDto.of(partner);
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") long id) {
        partnerRepository.deleteById(id);
    }

    private void apply(Partner partner, CompanyForm form) {
        if (StringUtils.isBlank(form.companyName)) {
            throw ValidationFailedException.of("companyName", "The company name is required");
        }
        partner.setCompanyName(form.companyName.trim());
        partner.setCompanyWebsite(form.companyWebsite);
        partner.setDescription(form.description);
        partner.setActive(form.active);
        partner.setPartnerPackage(StringUtils.isBlank(form.partnerPackage) ? PartnerPackage.MEDIA :
                                  PartnerPackage.valueOf(form.partnerPackage));
        byte[] logo = AdminSupport.bytes(form.logo);
        if (logo != null) {
            partner.setLogo(form.resizeImage ? thumbnailService.logo(logo) : logo);
        }
    }
}
