package org.bgjug.jprime.core.repository;

import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.Branch;

@ApplicationScoped
public class BranchRepository implements PanacheRepositoryBase<Branch, String> {

    public Optional<Branch> findByYear(int year) {
        return find("from Branch b where b.year = ?1", year).firstResultOptional();
    }

    public Optional<Branch> findCurrent() {
        return find("currentBranch", true).firstResultOptional();
    }

    public List<Branch> allByYear() {
        return list("from Branch b order by b.year");
    }
}
