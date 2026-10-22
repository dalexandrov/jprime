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
import org.bgjug.jprime.admin.dto.Dtos.TagDto;
import org.bgjug.jprime.core.model.Tag;
import org.bgjug.jprime.core.repository.TagRepository;
import org.bgjug.jprime.core.service.ValidationFailedException;

@Path("/api/admin/tags")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class TagAdminResource {

    @Inject
    TagRepository tagRepository;

    @GET
    public List<TagDto> list() {
        return tagRepository.allTags().stream().map(TagDto::of).toList();
    }

    @POST
    @Transactional
    public TagDto create(TagDto input) {
        Tag tag = new Tag(validName(input));
        tagRepository.persist(tag);
        return TagDto.of(tag);
    }

    @PUT
    @Path("{id}")
    @Transactional
    public TagDto update(@PathParam("id") long id, TagDto input) {
        Tag tag = AdminSupport.found(tagRepository.findByIdOptional(id), "Tag");
        tag.setName(validName(input));
        return TagDto.of(tag);
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") long id) {
        Tag tag = AdminSupport.found(tagRepository.findByIdOptional(id), "Tag");
        tag.getArticles().forEach(a -> a.getTags().remove(tag));
        tagRepository.delete(tag);
    }

    private static String validName(TagDto input) {
        if (input == null || StringUtils.isBlank(input.name())) {
            throw ValidationFailedException.of("name", "The name is required");
        }
        return input.name().trim();
    }
}
