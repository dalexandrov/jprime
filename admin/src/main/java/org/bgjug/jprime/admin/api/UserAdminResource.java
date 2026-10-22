package org.bgjug.jprime.admin.api;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.bgjug.jprime.admin.dto.Dtos;
import org.bgjug.jprime.admin.dto.Dtos.UserDto;
import org.bgjug.jprime.admin.dto.Dtos.UserInput;
import org.bgjug.jprime.core.model.User;
import org.bgjug.jprime.core.repository.UserRepository;
import org.bgjug.jprime.core.service.PageResult;
import org.bgjug.jprime.core.service.PasswordService;
import org.bgjug.jprime.core.service.ValidationFailedException;

/** All rows of the User table (plain users, speakers and sponsors). */
@Path("/api/admin/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class UserAdminResource {

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordService passwordService;

    @GET
    public Dtos.Page<UserDto> list(@QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("50") int size) {
        return Dtos.Page.of(PageResult.of(userRepository.findAll(Sort.descending("id")), Page.of(page, size))
            .map(UserDto::of));
    }

    @POST
    @Transactional
    public UserDto create(UserInput input) {
        if (input == null || StringUtils.isBlank(input.email()) || !input.email().contains("@")) {
            throw ValidationFailedException.of("email", "A valid e-mail is required");
        }
        if (userRepository.findByEmail(input.email().trim()).isPresent()) {
            throw ValidationFailedException.of("email", "This e-mail already exists");
        }
        User user = new User();
        user.setFirstName(input.firstName());
        user.setLastName(input.lastName());
        user.setEmail(input.email().trim());
        user.setPhone(input.phone());
        if (StringUtils.isNotBlank(input.password())) {
            user.setPassword(passwordService.hash(input.password()));
        }
        userRepository.persist(user);
        return UserDto.of(user);
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") long id) {
        userRepository.deleteById(id);
    }
}
