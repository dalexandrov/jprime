package org.bgjug.jprime.core.repository;

import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Registrant;

@ApplicationScoped
public class RegistrantRepository implements PanacheRepository<Registrant> {

    public Optional<Registrant> findByEpayInvoiceNumber(long number) {
        return find("epayInvoiceNumber", number).firstResultOptional();
    }

    public PanacheQuery<Registrant> byBranch(Branch branch) {
        return find("branch", Sort.descending("id"), branch);
    }

    public List<Registrant> listByBranch(Branch branch) {
        return list("branch", branch);
    }
}
