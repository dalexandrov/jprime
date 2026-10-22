package org.bgjug.jprime.site.web;

import java.util.Optional;

import io.quarkus.panache.common.Page;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.bgjug.jprime.core.model.Article;
import org.bgjug.jprime.core.repository.ArticleRepository;
import org.bgjug.jprime.core.service.PageResult;

/** Articles ("blog") and the pages behind the tags in the menu. */
@Path("/nav")
@Produces(MediaType.TEXT_HTML)
public class ArticleResource {

    private static final int PAGE_SIZE = 24;

    @Inject
    @Location("blog.html")
    Template blog;

    @Inject
    @Location("article.html")
    Template article;

    @Inject
    ArticleRepository articleRepository;

    @GET
    public String all(@QueryParam("page") @DefaultValue("0") int page) {
        PageResult<Article> articles = PageResult.of(articleRepository.published(), Page.of(page, PAGE_SIZE));
        return blog.data("articles", articles).data("title", "News").data("baseUrl", "/nav").render();
    }

    /** A tag with exactly one article (e.g. "Agenda") shows that article directly. */
    @GET
    @Path("{tag}")
    public String byTag(@PathParam("tag") String tag, @QueryParam("page") @DefaultValue("0") int page) {
        PageResult<Article> articles = PageResult.of(articleRepository.publishedByTag(tag), Page.of(page, PAGE_SIZE));
        if (articles.totalElements() == 0) {
            throw new NotFoundException("Invalid tag name " + tag);
        }
        if (articles.totalElements() == 1) {
            return article.data("article", articles.content().getFirst()).render();
        }
        return blog.data("articles", articles).data("title", tag).data("baseUrl", "/nav/" + tag).render();
    }

    @GET
    @Path("article/{id}")
    public String byId(@PathParam("id") long id) {
        return show(articleRepository.findByIdOptional(id));
    }

    @GET
    @Path("article")
    public String byTitle(@QueryParam("title") String title) {
        return show(title == null ? Optional.empty() : articleRepository.findByTitle(title));
    }

    private String show(Optional<Article> found) {
        Article a = found.filter(Article::isPublished).orElseThrow(() -> new NotFoundException("No such article"));
        return article.data("article", a).render();
    }
}
