package org.bgjug.jprime.core.repository;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.Sponsor;

@ApplicationScoped
public class SponsorRepository implements PanacheRepository<Sponsor> {

    public List<Sponsor> findActive() {
        return list("active", true);
    }
}
