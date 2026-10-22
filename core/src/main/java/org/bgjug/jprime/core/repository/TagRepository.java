package org.bgjug.jprime.core.repository;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.Tag;

@ApplicationScoped
public class TagRepository implements PanacheRepository<Tag> {

    public List<Tag> allTags() {
        return listAll(Sort.ascending("id"));
    }
}
