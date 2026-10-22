package org.bgjug.jprime.core.repository;

import java.time.LocalDateTime;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.BackgroundJob;

@ApplicationScoped
public class BackgroundJobRepository implements PanacheRepositoryBase<BackgroundJob, String> {

    /** Pending jobs plus jobs completed after the given time stamp. */
    public List<BackgroundJob> findRecent(LocalDateTime completedAfter) {
        return list("completed is null or completed >= ?1", completedAfter);
    }
}
