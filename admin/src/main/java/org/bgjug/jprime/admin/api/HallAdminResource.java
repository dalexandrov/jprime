package org.bgjug.jprime.admin.api;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.admin.dto.Dtos.HallDto;
import org.bgjug.jprime.core.model.VenueHall;
import org.bgjug.jprime.core.repository.VenueHallRepository;
import org.bgjug.jprime.core.service.ValidationFailedException;

@Path("/api/admin/halls")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class HallAdminResource {

    @Inject
    VenueHallRepository hallRepository;

    @GET
    public List<HallDto> list() {
        return hallRepository.listAll().stream().map(HallDto::of).toList();
    }

    @POST
    @Transactional
    public HallDto create(HallDto input) {
        VenueHall hall = new VenueHall();
        apply(hall, input);
        hallRepository.persist(hall);
        return HallDto.of(hall);
    }

    @PUT
    @Path("{id}")
    @Transactional
    public HallDto update(@PathParam("id") long id, HallDto input) {
        VenueHall hall = AdminSupport.found(hallRepository.findByIdOptional(id), "Hall");
        apply(hall, input);
        return HallDto.of(hall);
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") long id) {
        hallRepository.deleteById(id);
    }

    private static void apply(VenueHall hall, HallDto input) {
        if (input == null || StringUtils.isBlank(input.name())) {
            throw ValidationFailedException.of("name", "The name is required");
        }
        hall.setName(input.name().trim());
        hall.setDescription(input.description());
    }
}
