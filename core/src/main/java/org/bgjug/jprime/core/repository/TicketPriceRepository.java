package org.bgjug.jprime.core.repository;

import java.util.List;
import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.TicketPrice;
import org.bgjug.jprime.core.model.TicketType;

@ApplicationScoped
public class TicketPriceRepository implements PanacheRepository<TicketPrice> {

    public List<TicketPrice> findByBranch(Branch branch) {
        return list("branch", branch);
    }

    public Optional<TicketPrice> findByBranchAndType(Branch branch, TicketType type) {
        return find("branch = ?1 and ticketType = ?2", branch, type).firstResultOptional();
    }

    public long deleteByBranch(Branch branch) {
        return delete("branch", branch);
    }
}
