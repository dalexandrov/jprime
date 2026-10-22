package org.bgjug.jprime.core.repository;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.bgjug.jprime.core.model.Branch;
import org.bgjug.jprime.core.model.Session;

@ApplicationScoped
public class SessionRepository implements PanacheRepository<Session> {

    /** All talks of the branch and all breaks (sessions without submission), ordered by start time. */
    public List<Session> findForBranch(Branch branch) {
        return list("select s from Session s left join s.submission sub "
            + "where sub.branch = ?1 or s.submission is null order by s.startTime", branch);
    }

    /**
     * Sessions shown in a hall column of the agenda: the talks of the branch in that hall, titled sessions in that
     * hall (e.g. "TBA") and sessions without a hall (breaks, shown in every column). Same as the legacy native query.
     */
    public List<Session> findForBranchAndHall(Branch branch, String hallName) {
        return list("select s from Session s left join s.submission sub left join s.hall h "
            + "where (h.name = ?1 and s.title is not null) or (h.name = ?1 and sub.branch = ?2) or s.hall is null "
            + "order by s.startTime", hallName, branch);
    }
}
