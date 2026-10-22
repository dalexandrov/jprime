package org.bgjug.jprime.core.repository;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.VisitorJPro;

@ApplicationScoped
public class VisitorJProRepository implements PanacheRepository<VisitorJPro> {

    public List<VisitorJPro> newestFirst() {
        return listAll(Sort.descending("createdDate"));
    }

    public List<VisitorJPro> findByName(String name) {
        return list("lower(name) = ?1", name.toLowerCase());
    }

    public List<VisitorJPro> findByNameAndEmail(String name, String email) {
        return list("lower(name) = ?1 and lower(email) = ?2", name.toLowerCase(), email.toLowerCase());
    }
}
