package org.bgjug.jprime.core.repository;

import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.Article;

@ApplicationScoped
public class ArticleRepository implements PanacheRepository<Article> {

    public PanacheQuery<Article> publishedByTag(String tagName) {
        return find("select distinct a from Article a join a.tags t where t.name = ?1 and a.published = true "
            + "order by a.createdDate desc", tagName);
    }

    public PanacheQuery<Article> published() {
        return find("published", Sort.descending("createdDate"), true);
    }

    public List<Article> newestFirst() {
        return listAll(Sort.descending("createdDate"));
    }

    public Optional<Article> findByTitle(String title) {
        return find("title", title).firstResultOptional();
    }
}
