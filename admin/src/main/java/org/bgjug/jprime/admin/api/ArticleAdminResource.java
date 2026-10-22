package org.bgjug.jprime.admin.api;

import java.util.HashSet;
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
import org.bgjug.jprime.admin.dto.Dtos.ArticleDto;
import org.bgjug.jprime.admin.dto.Dtos.ArticleInput;
import org.bgjug.jprime.core.model.Article;
import org.bgjug.jprime.core.model.User;
import org.bgjug.jprime.core.repository.ArticleRepository;
import org.bgjug.jprime.core.repository.TagRepository;
import org.bgjug.jprime.core.repository.UserRepository;
import org.bgjug.jprime.core.service.ValidationFailedException;

@Path("/api/admin/articles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed(AdminSupport.ADMIN)
public class ArticleAdminResource {

    /** Articles are authored by this technical user (as in the legacy app). */
    static final String AUTHOR_EMAIL = "admin@jsprime.io";

    @Inject
    ArticleRepository articleRepository;

    @Inject
    TagRepository tagRepository;

    @Inject
    UserRepository userRepository;

    @GET
    public List<ArticleDto> list() {
        return articleRepository.newestFirst().stream().map(ArticleDto::summary).toList();
    }

    @GET
    @Path("{id}")
    public ArticleDto get(@PathParam("id") long id) {
        return ArticleDto.of(AdminSupport.found(articleRepository.findByIdOptional(id), "Article"));
    }

    @POST
    @Transactional
    public ArticleDto create(ArticleInput input) {
        Article article = new Article();
        apply(article, input);
        article.setAuthor(author());
        articleRepository.persist(article);
        return ArticleDto.of(article);
    }

    @PUT
    @Path("{id}")
    @Transactional
    public ArticleDto update(@PathParam("id") long id, ArticleInput input) {
        Article article = AdminSupport.found(articleRepository.findByIdOptional(id), "Article");
        apply(article, input);
        return ArticleDto.of(article);
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public void delete(@PathParam("id") long id) {
        articleRepository.deleteById(id);
    }

    private void apply(Article article, ArticleInput input) {
        if (StringUtils.isBlank(input.title())) {
            throw ValidationFailedException.of("title", "The title is required");
        }
        article.setTitle(input.title().trim());
        article.setDescription(input.description());
        article.setText(input.text() != null ? input.text() : "");
        article.setPublished(input.published());
        article.setTags(new HashSet<>(input.tagIds() == null || input.tagIds().isEmpty() ? List.of() :
                                      tagRepository.list("id in ?1", input.tagIds())));
    }

    private User author() {
        return userRepository.findByEmail(AUTHOR_EMAIL).orElseGet(() -> {
            User admin = new User();
            admin.setEmail(AUTHOR_EMAIL);
            admin.setFirstName("Admin");
            admin.setLastName("");
            userRepository.persist(admin);
            return admin;
        });
    }
}
