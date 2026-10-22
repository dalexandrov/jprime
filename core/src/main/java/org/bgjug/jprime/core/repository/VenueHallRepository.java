package org.bgjug.jprime.core.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.VenueHall;

@ApplicationScoped
public class VenueHallRepository implements PanacheRepository<VenueHall> {
}
